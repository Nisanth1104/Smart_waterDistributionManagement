package com.smartwater.server;

import com.smartwater.model.*;
import com.smartwater.network.WaterNetwork;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SimpleHttpServer Class
 * Pure standard Java HTTP Server built on standard java.net.ServerSocket.
 * Operates reliably on all platforms without NIO Selector loopback permission restrictions.
 * Serves both REST API endpoints (/api/...) and static frontend files (HTML/CSS/JS).
 */
public class SimpleHttpServer {
    private final int port;
    private final WaterNetwork network;
    private final Path staticBaseDir;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public SimpleHttpServer(int port, WaterNetwork network) {
        this.port = port;
        this.network = network;

        // Locate static files folder
        Path base = Paths.get("SmartWaterDistribution").toAbsolutePath().normalize();
        if (!Files.exists(base)) {
            base = Paths.get(".").toAbsolutePath().normalize();
        }
        this.staticBaseDir = base;
    }

    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(12);
        this.running = true;

        Thread listenerThread = new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    threadPool.submit(() -> handleClientSocket(socket));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        }, "WaterServer-Listener");
        listenerThread.setDaemon(false); // Non-daemon thread keeps JVM alive
        listenerThread.start();
    }

    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }

    private void handleClientSocket(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            // 1. Read Request Line: "METHOD /path HTTP/1.1"
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0].toUpperCase();
            String fullPath = parts[1];

            // Separate path and query string
            String path = fullPath;
            String query = "";
            int qIdx = fullPath.indexOf('?');
            if (qIdx != -1) {
                path = fullPath.substring(0, qIdx);
                query = fullPath.substring(qIdx + 1);
            }

            // 2. Read Request Headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            int contentLength = 0;

            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                int colon = headerLine.indexOf(':');
                if (colon != -1) {
                    String hName = headerLine.substring(0, colon).trim().toLowerCase();
                    String hVal = headerLine.substring(colon + 1).trim();
                    headers.put(hName, hVal);
                    if ("content-length".equals(hName)) {
                        try {
                            contentLength = Integer.parseInt(hVal);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            // 3. Read Body (if Content-Length specified)
            StringBuilder bodyBuilder = new StringBuilder();
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int r = reader.read(buffer, totalRead, contentLength - totalRead);
                    if (r == -1) break;
                    totalRead += r;
                }
                bodyBuilder.append(buffer, 0, totalRead);
            }
            String body = bodyBuilder.toString();

            // 4. Handle CORS Preflight OPTIONS
            if ("OPTIONS".equals(method)) {
                sendResponse(out, 204, "No Content", "text/plain", new byte[0]);
                return;
            }

            // 5. Route to API or Static Files
            if (path.startsWith("/api")) {
                handleApi(method, path, query, body, out);
            } else {
                handleStaticFile(path, out);
            }

        } catch (Exception ex) {
            // Client disconnect or socket error
        }
    }

    // =========================================================================
    // REST API ROUTING
    // =========================================================================

    private void handleApi(String method, String path, String query, String body, OutputStream out) throws IOException {
        try {
            if (path.equals("/api/login") && "POST".equals(method)) {
                handleLogin(body, out);
            } else if (path.equals("/api/overview") && "GET".equals(method)) {
                network.loadReservoirsFromDatabase();
                network.loadDemandAreasFromDatabase();
                network.loadPipelinesFromDatabase();
                network.loadWaterRequestsFromDatabase();
                network.loadComplaintsFromDatabase();
                network.loadMaintenanceRequestsFromDatabase();
                handleOverview(out);
            } else if (path.equals("/api/reservoirs")) {
                if ("GET".equals(method)) {
                    network.loadReservoirsFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.reservoirsToJson(network.getAllReservoirs()));
                } else if ("PUT".equals(method)) {
                    handleUpdateReservoir(body, out);
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/areas")) {
                if ("GET".equals(method)) {
                    network.loadDemandAreasFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.demandAreasToJson(network.getAllDemandAreas()));
                } else if ("POST".equals(method)) {
                    handleAddDemandArea(body, out);
                } else if ("PUT".equals(method)) {
                    handleUpdateDemandArea(body, out);
                } else if ("DELETE".equals(method)) {
                    handleDeleteDemandArea(query, body, out);
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/pipelines")) {
                if ("GET".equals(method)) {
                    network.loadPipelinesFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.pipelinesToJson(network.getAllPipelines()));
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/pipelines/status") && "PUT".equals(method)) {
                handleUpdatePipelineStatus(body, out);
            } else if (path.equals("/api/requests")) {
                if ("GET".equals(method)) {
                    network.loadWaterRequestsFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.waterRequestsToJson(network.getRequestHistory()));
                } else if ("POST".equals(method)) {
                    handleAddRequest(body, out);
                } else if ("DELETE".equals(method)) {
                    handleDeleteRequest(query, body, out);
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/complaints")) {
                if ("GET".equals(method)) {
                    network.loadComplaintsFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.complaintsToJson(network.getComplaints()));
                } else if ("POST".equals(method)) {
                    handleAddComplaint(body, out);
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/complaints/status") && "PUT".equals(method)) {
                handleUpdateComplaintStatus(body, out);
            } else if (path.equals("/api/maintenance")) {
                if ("GET".equals(method)) {
                    network.loadMaintenanceRequestsFromDatabase();
                    sendJsonResponse(out, 200, SimpleJson.maintenanceQueueToJson(network.getMaintenanceQueueList()));
                } else if ("POST".equals(method)) {
                    handleAddMaintenanceJob(body, out);
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/maintenance/process") && "POST".equals(method)) {
                handleProcessMaintenanceFIFO(out);
            } else if (path.equals("/api/maintenance/repair") && "PUT".equals(method)) {
                handleRepairMaintenanceJob(body, out);
            } else if (path.equals("/api/network/connectivity") && "GET".equals(method)) {
                network.loadPipelinesFromDatabase();
                network.loadReservoirsFromDatabase();
                handleNetworkConnectivity(out);
            } else if (path.equals("/api/allocation")) {
                if ("GET".equals(method)) {
                    network.loadReservoirsFromDatabase();
                    network.loadDemandAreasFromDatabase();
                    network.loadPipelinesFromDatabase();
                    network.loadWaterRequestsFromDatabase();
                    AllocationSummary summary = network.getLatestAllocationSummary();
                    sendJsonResponse(out, 200, SimpleJson.allocationSummaryToJson(summary));
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }
            } else if (path.equals("/api/allocation/run") && "POST".equals(method)) {
                network.loadReservoirsFromDatabase();
                network.loadDemandAreasFromDatabase();
                network.loadPipelinesFromDatabase();
                network.loadWaterRequestsFromDatabase();
                AllocationSummary summary = network.runAllocation(true);
                sendJsonResponse(out, 200, SimpleJson.allocationSummaryToJson(summary));
            } else if (path.equals("/api/reset") && "POST".equals(method)) {
                network.loadInitialSampleData();
                sendJsonResponse(out, 200, SimpleJson.successResponse("Default sample data restored successfully."));
            } else {
                sendError(out, 404, "API Endpoint Not Found: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(out, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleLogin(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String username = SimpleJson.getString(data, "username", "");
        String password = SimpleJson.getString(data, "password", "");
        String role = SimpleJson.getString(data, "role", "user");

        boolean isValid = false;
        String userDisplayName = "";

        if ("admin".equalsIgnoreCase(role)) {
            if ("admin".equals(username) && "admin123".equals(password)) {
                isValid = true;
                userDisplayName = "Chief Water Engineer (Admin)";
            }
        } else {
            if ("user".equals(username) && "user123".equals(password)) {
                isValid = true;
                userDisplayName = "Resident User (Greenfield)";
            }
        }

        if (isValid) {
            String json = String.format("{\"success\":true,\"username\":\"%s\",\"displayName\":\"%s\",\"role\":\"%s\"}",
                    username, userDisplayName, role);
            sendJsonResponse(out, 200, json);
        } else {
            sendJsonResponse(out, 401, "{\"success\":false,\"error\":\"Invalid credentials!\"}");
        }
    }

    private void handleOverview(OutputStream out) throws IOException {
        int totalAreas = network.getAllDemandAreas().size();
        int totalDemand = 0;
        for (WaterRequest r : network.getRequestHistory()) {
            totalDemand += r.getQuantity();
        }
        int availableWater = network.getTotalAvailableActiveWater();
        int pendingComplaints = 0;
        for (Complaint c : network.getComplaints()) {
            if ("Open".equalsIgnoreCase(c.getStatus()) || "In Progress".equalsIgnoreCase(c.getStatus())) {
                pendingComplaints++;
            }
        }
        int failedPipelines = 0;
        for (Pipeline p : network.getAllPipelines()) {
            if (p.isFailed()) failedPipelines++;
        }
        Set<String> reachable = network.findReachableAreasBFS();
        int areasReceivingWater = reachable.size();

        String json = String.format(Locale.US,
                "{\"totalAreas\":%d,\"totalDemand\":%d,\"availableWater\":%d,\"pendingComplaints\":%d,\"failedPipelines\":%d,\"areasReceivingWater\":%d}",
                totalAreas, totalDemand, availableWater, pendingComplaints, failedPipelines, areasReceivingWater);
        sendJsonResponse(out, 200, json);
    }

    private void handleUpdateReservoir(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        int available = SimpleJson.getInt(data, "available", -1);
        int dailyLimit = SimpleJson.getInt(data, "dailyLimit", -1);
        String status = SimpleJson.getString(data, "status", "Active");

        if (id.isEmpty() || available < 0 || dailyLimit < 0) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Invalid reservoir parameters."));
            return;
        }

        boolean updated = network.updateReservoirInDatabase(id, available, dailyLimit, status);
        if (updated) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Reservoir " + id + " updated successfully in database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to update reservoir in database: " + id));
        }
    }

    private void handleAddDemandArea(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String name = SimpleJson.getString(data, "name", "");
        String type = SimpleJson.getString(data, "type", "Residential");
        int minReq = SimpleJson.getInt(data, "minRequirement", 0);
        int maxReq = SimpleJson.getInt(data, "maxRequirement", 0);
        String priorityStr = SimpleJson.getString(data, "priority", "Medium");
        String res = SimpleJson.getString(data, "connectedReservoir", "RES-A");
        String desc = SimpleJson.getString(data, "description", "");

        if (name.isEmpty() || minReq > maxReq || minReq < 0) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Validation error: Min demand must not exceed Max demand."));
            return;
        }

        PriorityLevel priority = PriorityLevel.fromString(priorityStr);
        DemandArea area;
        if ("Critical Institution".equalsIgnoreCase(type)) {
            area = new CriticalInstitution(id, name, minReq, maxReq, priority, res, desc);
        } else if ("Commercial".equalsIgnoreCase(type)) {
            area = new CommercialArea(id, name, minReq, maxReq, priority, res, desc);
        } else {
            area = new ResidentialArea(id, name, minReq, maxReq, priority, res, desc);
        }

        boolean saved = network.saveDemandAreaToDatabase(area, name + " Feeder Trunk", maxReq + 5000, 4.5);
        if (saved) {
            sendJsonResponse(out, 201, SimpleJson.successResponse("Demand Area " + area.getId() + " and Feeder Pipeline saved to database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to save demand area to database."));
        }
    }

    private void handleUpdateDemandArea(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        DemandArea area = network.getDemandArea(id);
        if (area == null) {
            sendJsonResponse(out, 404, SimpleJson.errorResponse("Area not found: " + id));
            return;
        }

        String name = SimpleJson.getString(data, "name", area.getName());
        int minReq = SimpleJson.getInt(data, "minRequirement", area.getMinRequirement());
        int maxReq = SimpleJson.getInt(data, "maxRequirement", area.getMaxRequirement());
        String priorityStr = SimpleJson.getString(data, "priority", area.getPriority().getDisplayName());
        String res = SimpleJson.getString(data, "connectedReservoir", area.getConnectedReservoir());
        String desc = SimpleJson.getString(data, "description", area.getDescription());

        if (minReq > maxReq) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Min requirement cannot exceed Max requirement."));
            return;
        }

        boolean updated = network.updateDemandAreaInDatabase(id, name, minReq, maxReq, PriorityLevel.fromString(priorityStr), res, desc);
        if (updated) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Demand Area " + id + " updated successfully in database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to update demand area in database: " + id));
        }
    }

    private void handleDeleteDemandArea(String query, String body, OutputStream out) throws IOException {
        String areaId = "";
        if (query != null && query.startsWith("id=")) {
            areaId = query.substring(3);
        } else {
            Map<String, String> data = SimpleJson.parseJsonObject(body);
            areaId = SimpleJson.getString(data, "id", "");
        }

        if (areaId.isEmpty()) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Area ID is required."));
            return;
        }

        boolean removed = network.deleteDemandAreaFromDatabase(areaId);
        if (removed) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Area " + areaId + " deleted successfully from database."));
        } else {
            sendJsonResponse(out, 404, SimpleJson.errorResponse("Area not found or failed to delete from database: " + areaId));
        }
    }

    private void handleUpdatePipelineStatus(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String statusStr = SimpleJson.getString(data, "status", "Working");

        PipelineStatus status = PipelineStatus.fromString(statusStr);
        boolean updated = network.updatePipelineStatusInDatabase(id, status);

        if (updated) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Pipeline " + id + " status set to " + status.getDisplayName() + " in database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to update pipeline status in database: " + id));
        }
    }

    private void handleAddRequest(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String user = SimpleJson.getString(data, "userName", "Resident");
        String areaId = SimpleJson.getString(data, "areaId", "");
        int quantity = SimpleJson.getInt(data, "quantity", 0);
        String priorityStr = SimpleJson.getString(data, "priority", "High");
        String notes = SimpleJson.getString(data, "notes", "");
        String date = SimpleJson.getString(data, "date", "2026-10-25");

        if (areaId.isEmpty() || quantity <= 0) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Area and positive quantity are required."));
            return;
        }

        DemandArea area = network.getDemandArea(areaId);
        String areaName = area != null ? area.getName() : areaId;
        PriorityLevel priority = PriorityLevel.fromString(priorityStr);

        WaterRequest req = new WaterRequest(id, user, areaId, areaName, quantity, priority, notes, date, 0);
        boolean saved = network.saveWaterRequestToDatabase(req);

        if (saved) {
            String json = String.format("{\"success\":true,\"id\":\"%s\",\"message\":\"Water demand %s registered and saved successfully.\"}",
                    SimpleJson.escape(req.getId()), SimpleJson.escape(req.getId()));
            sendJsonResponse(out, 201, json);
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to save water demand to database."));
        }
    }

    private void handleDeleteRequest(String query, String body, OutputStream out) throws IOException {
        String reqId = "";
        if (query != null && query.startsWith("id=")) {
            reqId = query.substring(3);
        } else {
            Map<String, String> data = SimpleJson.parseJsonObject(body);
            reqId = SimpleJson.getString(data, "id", "");
        }

        if (reqId.isEmpty()) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Request ID is required."));
            return;
        }

        boolean removed = network.deleteWaterRequestFromDatabase(reqId);
        if (removed) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Water request " + reqId + " deleted successfully from database."));
        } else {
            sendJsonResponse(out, 404, SimpleJson.errorResponse("Water request not found or could not be deleted: " + reqId));
        }
    }

    private void handleAddComplaint(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String user = SimpleJson.getString(data, "userName", "Resident");
        String areaId = SimpleJson.getString(data, "areaId", "");
        String type = SimpleJson.getString(data, "type", "Water Not Supplied");
        String desc = SimpleJson.getString(data, "description", "");
        String date = SimpleJson.getString(data, "date", "2026-10-25");

        if (areaId.isEmpty() || desc.isEmpty()) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Area and description are mandatory."));
            return;
        }

        DemandArea area = network.getDemandArea(areaId);
        String areaName = area != null ? area.getName() : areaId;

        Complaint c = new Complaint(id, user, areaId, areaName, type, desc, date, "Open");
        boolean saved = network.saveComplaintToDatabase(c);

        if (saved) {
            sendJsonResponse(out, 201, SimpleJson.successResponse("Complaint " + c.getId() + " submitted and saved to database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to save complaint to database."));
        }
    }

    private void handleUpdateComplaintStatus(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String status = SimpleJson.getString(data, "status", "Open");

        boolean ok = network.updateComplaintStatusInDatabase(id, status);
        if (ok) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Complaint " + id + " status set to " + status + " in database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to update complaint in database: " + id));
        }
    }

    private void handleAddMaintenanceJob(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");
        String pipeId = SimpleJson.getString(data, "pipelineId", "");
        String area = SimpleJson.getString(data, "affectedArea", "");
        String problem = SimpleJson.getString(data, "problemDescription", "");
        String priority = SimpleJson.getString(data, "priority", "Medium");
        String date = SimpleJson.getString(data, "dateAdded", "2026-10-25");

        if (pipeId.isEmpty() || problem.isEmpty()) {
            sendJsonResponse(out, 400, SimpleJson.errorResponse("Pipeline ID and problem description are required."));
            return;
        }

        MaintenanceRequest job = new MaintenanceRequest(id, pipeId, area, problem, priority, date, "Queued");
        boolean saved = network.saveMaintenanceRequestToDatabase(job);

        if (saved) {
            sendJsonResponse(out, 201, SimpleJson.successResponse("Task " + job.getId() + " enqueued and saved to database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to save maintenance task to database."));
        }
    }

    private void handleProcessMaintenanceFIFO(OutputStream out) throws IOException {
        MaintenanceRequest job = network.processNextMaintenanceFIFOInDatabase();
        if (job == null) {
            sendJsonResponse(out, 200, "{\"success\":true,\"message\":\"The maintenance queue is currently empty. All pipelines are operational.\"}");
        } else {
            String json = String.format("{\"success\":true,\"message\":\"FIFO Dequeue: Job %s is now %s in database. Pipeline %s state updated.\",\"job\":%s}",
                    job.getId(), job.getStatus(), job.getPipelineId(), SimpleJson.maintenanceRequestToJson(job));
            sendJsonResponse(out, 200, json);
        }
    }

    private void handleRepairMaintenanceJob(String body, OutputStream out) throws IOException {
        Map<String, String> data = SimpleJson.parseJsonObject(body);
        String id = SimpleJson.getString(data, "id", "");

        boolean ok = network.markMaintenanceRepairedInDatabase(id);
        if (ok) {
            sendJsonResponse(out, 200, SimpleJson.successResponse("Maintenance task " + id + " completed and pipeline restored to Working in database."));
        } else {
            sendJsonResponse(out, 500, SimpleJson.errorResponse("Failed to repair maintenance task in database: " + id));
        }
    }

    private void handleNetworkConnectivity(OutputStream out) throws IOException {
        Set<String> reachable = network.findReachableAreasBFS();
        Set<String> disconnected = network.findDisconnectedAreas();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"reachable\":[");
        int i = 0;
        for (String a : reachable) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(a).append("\"");
        }
        sb.append("],\"disconnected\":[");
        i = 0;
        for (String a : disconnected) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(a).append("\"");
        }
        sb.append("]}");

        sendJsonResponse(out, 200, sb.toString());
    }

    // =========================================================================
    // STATIC FILE SERVING
    // =========================================================================

    private void handleStaticFile(String reqPath, OutputStream out) throws IOException {
        if (reqPath.equals("/") || reqPath.isEmpty()) {
            reqPath = "/index.html";
        }
        if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        Path target = staticBaseDir.resolve(reqPath).normalize();

        if (!target.startsWith(staticBaseDir) || !Files.exists(target) || Files.isDirectory(target)) {
            String notFound = "<h1>404 Not Found</h1><p>Static file not found: " + reqPath + "</p>";
            sendResponse(out, 404, "Not Found", "text/html; charset=utf-8", notFound.getBytes(StandardCharsets.UTF_8));
            return;
        }

        String mime = getContentType(target.toString());
        byte[] bytes = Files.readAllBytes(target);
        sendResponse(out, 200, "OK", mime, bytes);
    }

    private String getContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=utf-8";
    }

    private void sendJsonResponse(OutputStream out, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        sendResponse(out, status, status == 200 || status == 201 ? "OK" : "Error", "application/json; charset=utf-8", bytes);
    }

    private void sendError(OutputStream out, int status, String msg) throws IOException {
        sendJsonResponse(out, status, SimpleJson.errorResponse(msg));
    }

    private void sendResponse(OutputStream out, int statusCode, String statusText, String contentType, byte[] bodyBytes) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), false);
        pw.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        pw.print("Content-Type: " + contentType + "\r\n");
        pw.print("Content-Length: " + bodyBytes.length + "\r\n");
        pw.print("Access-Control-Allow-Origin: *\r\n");
        pw.print("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        pw.print("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        pw.print("Connection: close\r\n");
        pw.print("\r\n");
        pw.flush();

        out.write(bodyBytes);
        out.flush();
    }
}
