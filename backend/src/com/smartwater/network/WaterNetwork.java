package com.smartwater.network;

import com.smartwater.model.*;
import com.smartwater.policy.PriorityAllocationPolicy;
import com.smartwater.policy.WaterAllocationPolicy;
import com.smartwater.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

/**
 * WaterNetwork Class
 * Core distribution grid coordinator demonstrating:
 * - Composition: Composes Reservoirs, Pipelines, DemandAreas, Queues, and Graph Adjacency List
 * - Adjacency List Graph: Map<String, List<Pipeline>> representing reservoir-to-area pipeline links
 * - BFS / DFS Graph Traversal: Identifies reachable vs disconnected demand areas
 * - ArrayDeque: Implements FIFO maintenance queue for pipeline repairs
 * - PriorityQueue: Priority-based sorting of water requests
 * - HashMap: O(1) indexed entity storage for reservoirs, areas, and pipelines
 * - ArrayList: Audit history of requests, complaints, and allocations
 */
public class WaterNetwork {
    // 1. HashMaps for O(1) entity lookups
    private final Map<String, Reservoir> reservoirs;
    private final Map<String, DemandArea> demandAreas;
    private final Map<String, Pipeline> pipelines;

    // 2. Adjacency List Graph Representation
    // Key: Node ID (Reservoir ID or Area ID), Value: List of outgoing/connecting pipelines
    private final Map<String, List<Pipeline>> adjacencyList;

    // 3. FIFO Queue for Pipeline Maintenance (ArrayDeque)
    private final ArrayDeque<MaintenanceRequest> maintenanceQueue;

    // 4. ArrayLists for History & Logs
    private final List<WaterRequest> requestHistory;
    private final List<Complaint> complaints;
    private AllocationSummary latestAllocationSummary;

    // Default Allocation Policy
    private WaterAllocationPolicy allocationPolicy;

    // Sequence counter for deterministic FIFO tie-breaking in PriorityQueue
    private long requestArrivalCounter = 0;

    public WaterNetwork() {
        this.reservoirs = new HashMap<>();
        this.demandAreas = new HashMap<>();
        this.pipelines = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.maintenanceQueue = new ArrayDeque<>();
        this.requestHistory = new ArrayList<>();
        this.complaints = new ArrayList<>();
        this.allocationPolicy = new PriorityAllocationPolicy();


        // Load standard default demonstration sample data
        loadInitialSampleData();
    }

    /**
     * Initializes default sample data:
     * - Reservoirs A (100k, 40k avail) & B (80k, 25k avail)
     * - Demand Areas 1 to 5 (Residential, Hospital, School, etc.)
     * - Pipelines PL-01 to PL-06 (PL-04 is intentionally Failed)
     * - Initial Complaints & FIFO Maintenance tasks
     */
    public synchronized void loadInitialSampleData() {
        reservoirs.clear();
        demandAreas.clear();
        pipelines.clear();
        adjacencyList.clear();
        maintenanceQueue.clear();
        requestHistory.clear();
        complaints.clear();
        requestArrivalCounter = 0;

        // 1. Reservoirs
        addReservoir(new Reservoir("RES-A", "Reservoir A (North Hill)", 100000, 40000, 50000, "Active", "North Hill Sector"));
        addReservoir(new Reservoir("RES-B", "Reservoir B (South Valley)", 80000, 25000, 40000, "Active", "South Valley Sector"));

        // 2. Demand Areas (Demonstrating Polymorphism with subclasses)
        addDemandArea(new ResidentialArea("AREA-01", "Greenfield Colony (Area 1)", 15000, 20000, PriorityLevel.HIGH, "RES-A", "Dense residential neighborhood with 800 households.", 800));
        
        // Critical Institution defaults to CRITICAL priority
        addDemandArea(new CriticalInstitution("AREA-02", "City Care Hospital (Area 2)", 12000, 15000, PriorityLevel.CRITICAL, "RES-A", "Multi-specialty 400-bed hospital with continuous ICU water requirement.", "911-HOSP-EMERGENCY", true));
        // Give Area 2 a backup reservoir link to Reservoir B!
        getDemandArea("AREA-02").setBackupReservoir("RES-B");

        addDemandArea(new ResidentialArea("AREA-03", "Sunrise Township (Area 3)", 14000, 18000, PriorityLevel.MEDIUM, "RES-B", "Suburban apartment complexes and community center.", 600));
        addDemandArea(new CriticalInstitution("AREA-04", "Apex Model School (Area 4)", 8000, 12000, PriorityLevel.HIGH, "RES-A", "Senior secondary campus with 2,500 students and staff.", "SCH-ADMIN-04", false));
        addDemandArea(new ResidentialArea("AREA-05", "Riverview Heights (Area 5)", 7000, 10000, PriorityLevel.LOW, "RES-B", "Low-density gated community on the city perimeter.", 350));

        // 3. Pipelines (PL-04 is initially FAILED to demonstrate disconnected areas!)
        addPipeline(new Pipeline("PL-01", "North Main Trunk 1", "RES-A", "AREA-01", 25000, PipelineStatus.WORKING, 4.2));
        addPipeline(new Pipeline("PL-02", "Hospital Express Line", "RES-A", "AREA-02", 20000, PipelineStatus.WORKING, 2.8));
        addPipeline(new Pipeline("PL-03", "South Valley Line 1", "RES-B", "AREA-03", 25000, PipelineStatus.WORKING, 3.5));
        addPipeline(new Pipeline("PL-04", "School Zone Feeder", "RES-A", "AREA-04", 15000, PipelineStatus.FAILED, 5.1)); // FAILED
        addPipeline(new Pipeline("PL-05", "Riverview Line", "RES-B", "AREA-05", 15000, PipelineStatus.WORKING, 6.0));
        addPipeline(new Pipeline("PL-06", "Hospital South Backup Link", "RES-B", "AREA-02", 15000, PipelineStatus.WORKING, 4.0)); // Backup for Area 2!

        // 4. Initial Water Demands
        addWaterRequest(new WaterRequest("REQ-101", "Hospital Superintendant", "AREA-02", "City Care Hospital (Area 2)", 15000, PriorityLevel.CRITICAL, "Critical surgical theatre and dialysis unit requirements", "2026-10-24", ++requestArrivalCounter));
        addWaterRequest(new WaterRequest("REQ-102", "RWA President - Greenfield", "AREA-01", "Greenfield Colony (Area 1)", 20000, PriorityLevel.HIGH, "Daily morning and evening domestic distribution", "2026-10-24", ++requestArrivalCounter));
        addWaterRequest(new WaterRequest("REQ-103", "School Principal", "AREA-04", "Apex Model School (Area 4)", 12000, PriorityLevel.HIGH, "Drinking water and cafeteria sanitation", "2026-10-25", ++requestArrivalCounter));
        addWaterRequest(new WaterRequest("REQ-104", "Sunrise Secretary", "AREA-03", "Sunrise Township (Area 3)", 18000, PriorityLevel.MEDIUM, "General household supply", "2026-10-25", ++requestArrivalCounter));
        addWaterRequest(new WaterRequest("REQ-105", "Riverview Resident", "AREA-05", "Riverview Heights (Area 5)", 10000, PriorityLevel.LOW, "Auxiliary garden and tank replenishment", "2026-10-25", ++requestArrivalCounter));

        // 5. Initial Complaints
        addComplaint(new Complaint("CMP-201", "Apex School Admin", "AREA-04", "Apex Model School (Area 4)", "Water Not Supplied", "Zero water flow in main inlet valve since morning due to suspected pipeline issue.", "2026-10-25", "Open"));
        addComplaint(new Complaint("CMP-202", "Greenfield Resident", "AREA-01", "Greenfield Colony (Area 1)", "Low Water Supply", "Water pressure in 3rd floor apartments is inadequate.", "2026-10-24", "In Progress"));
        addComplaint(new Complaint("CMP-203", "Sunrise Township Secretary", "AREA-03", "Sunrise Township (Area 3)", "Pipeline Leakage", "Minor water seepage observed near valve junction B-3.", "2026-10-23", "Resolved"));

        // 6. Initial FIFO Maintenance Queue
        enqueueMaintenance(new MaintenanceRequest("MNT-301", "PL-04", "Apex Model School (Area 4)", "Main line burst near Sector 4 underpass. Supply disconnected.", "Critical", "2026-10-25", "Queued"));
        enqueueMaintenance(new MaintenanceRequest("MNT-302", "PL-03", "Sunrise Township (Area 3)", "Periodic flange vibration and joint wear inspection.", "Medium", "2026-10-25", "Queued"));

        // Precompute initial allocation preview
        this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
    }

    // =========================================================================
    // GRAPH ADJACENCY LIST & BFS / DFS REACHABILITY TRAVERSAL
    // =========================================================================

    /**
     * Rebuilds the internal Adjacency List graph representation from all registered pipelines
     */
    public synchronized void rebuildAdjacencyList() {
        adjacencyList.clear();
        for (Pipeline p : pipelines.values()) {
            adjacencyList.computeIfAbsent(p.getFromReservoirId(), k -> new ArrayList<>()).add(p);
            // Also store under target for bidirectional lookups
            adjacencyList.computeIfAbsent(p.getToAreaId(), k -> new ArrayList<>()).add(p);
        }
    }

    /**
     * Breadth-First Search (BFS) to identify all demand areas reachable from active reservoirs.
     * An area is reachable IF AND ONLY IF:
     * 1. There is a path from at least one ACTIVE reservoir.
     * 2. All pipelines in the path have status WORKING.
     * 
     * @return Set of area IDs that are reachable
     */
    public synchronized Set<String> findReachableAreasBFS() {
        Set<String> reachableAreas = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visitedNodes = new HashSet<>();

        // Enqueue all active reservoirs as starting sources
        for (Reservoir res : reservoirs.values()) {
            if (res.isActive()) {
                queue.offer(res.getId());
                visitedNodes.add(res.getId());
            }
        }

        // BFS Traversal
        while (!queue.isEmpty()) {
            String currentNode = queue.poll();

            List<Pipeline> outgoing = adjacencyList.get(currentNode);
            if (outgoing == null) continue;

            for (Pipeline edge : outgoing) {
                // Must be a WORKING pipeline
                if (!edge.isWorking()) {
                    continue;
                }

                // If traversing from reservoir to area
                if (edge.getFromReservoirId().equals(currentNode)) {
                    String targetAreaId = edge.getToAreaId();
                    reachableAreas.add(targetAreaId);

                    if (!visitedNodes.contains(targetAreaId)) {
                        visitedNodes.add(targetAreaId);
                        queue.offer(targetAreaId);
                    }
                }
            }
        }

        return reachableAreas;
    }

    /**
     * Identifies disconnected areas (areas configured in network that cannot be reached via working pipelines)
     */
    public synchronized Set<String> findDisconnectedAreas() {
        Set<String> reachable = findReachableAreasBFS();
        Set<String> disconnected = new HashSet<>();
        for (String areaId : demandAreas.keySet()) {
            if (!reachable.contains(areaId)) {
                disconnected.add(areaId);
            }
        }
        return disconnected;
    }

    /**
     * Check if a single area is reachable from any active reservoir via working pipelines
     */
    public synchronized boolean isAreaReachable(String areaId) {
        return findReachableAreasBFS().contains(areaId);
    }

    /**
     * Find which active reservoirs can reach a given demand area via working pipelines.
     * Enables multi-reservoir switching (e.g. Area 2 drawing from Reservoir B when A is depleted or PL-02 fails).
     */
    public synchronized List<Reservoir> getReachableActiveReservoirsForArea(String areaId) {
        List<Reservoir> result = new ArrayList<>();
        for (Pipeline p : pipelines.values()) {
            if (p.getToAreaId().equals(areaId) && p.isWorking()) {
                Reservoir r = reservoirs.get(p.getFromReservoirId());
                if (r != null && r.isActive()) {
                    if (!result.contains(r)) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    // =========================================================================
    // RESERVOIR MANAGEMENT
    // =========================================================================

    public synchronized void addReservoir(Reservoir reservoir) {
        if (reservoir != null) {
            reservoirs.put(reservoir.getId(), reservoir);
            rebuildAdjacencyList();
        }
    }

    public synchronized Reservoir getReservoir(String id) {
        return reservoirs.get(id);
    }

    public synchronized Collection<Reservoir> getAllReservoirs() {
        return new ArrayList<>(reservoirs.values());
    }

    public synchronized int getTotalAvailableActiveWater() {
        int total = 0;
        for (Reservoir r : reservoirs.values()) {
            if (r.isActive()) {
                total += r.getAvailable();
            }
        }
        return total;
    }

    public synchronized boolean updateReservoir(String id, int available, int dailyLimit, String status) {
        Reservoir r = reservoirs.get(id);
        if (r == null) return false;

        r.setAvailable(available);
        r.setDailyLimit(dailyLimit);
        r.setStatus(status);
        return true;
    }

    /**
     * Loads reservoir records from MySQL database using DatabaseConnection.
     * Transactional/staged loading:
     * - Parses records into a temporary map first.
     * - Preserves stable identifiers (RES-A, RES-B) using the 'code' column.
     * - Existing collections are only updated after all records are loaded successfully.
     * - If database connection or query fails, active in-memory state is preserved safely.
     *
     * @return true if database records were loaded successfully, false otherwise.
     */
    public synchronized boolean loadReservoirsFromDatabase() {
        String sql = "SELECT id, code, name, location, capacity, current_level, daily_limit, minimum_level, status FROM reservoirs";
        Map<String, Reservoir> loadedReservoirs = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String name = rs.getString("name");
                String location = rs.getString("location");
                int capacity = rs.getInt("capacity");
                int currentLevel = rs.getInt("current_level");
                int dailyLimit = rs.getInt("daily_limit");
                String status = rs.getString("status");

                // Preserve stable identifiers (e.g. RES-A, RES-B) from 'code' column
                String resId = (code != null && !code.trim().isEmpty()) ? code.trim() : "RES-" + dbId;
                if (name == null || name.trim().isEmpty()) {
                    name = "Reservoir " + resId;
                }
                if (status == null || status.trim().isEmpty()) {
                    status = "Active";
                }

                // Volume bounds safety to prevent IllegalArgumentException
                if (capacity < 0) capacity = 0;
                if (currentLevel < 0) currentLevel = 0;
                if (capacity > 0 && currentLevel > capacity) currentLevel = capacity;
                if (dailyLimit < 0) dailyLimit = capacity;

                Reservoir reservoir = new Reservoir(resId, name, capacity, currentLevel, dailyLimit, status, location);
                loadedReservoirs.put(resId, reservoir);
            }

            if (!loadedReservoirs.isEmpty()) {
                this.reservoirs.clear();
                for (Map.Entry<String, Reservoir> entry : loadedReservoirs.entrySet()) {
                    this.reservoirs.put(entry.getKey(), entry.getValue());
                }
                rebuildAdjacencyList();
                if (this.allocationPolicy != null) {
                    this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
                }
                System.out.println("✓ Successfully loaded " + loadedReservoirs.size() + " reservoir(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 reservoir records; retaining existing in-memory reservoirs.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("⚠ Database connection/query failed when loading reservoirs: " + e.getMessage());
            System.err.println("ℹ Preserving active in-memory reservoir data.");
            return false;
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error loading reservoirs from database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates reservoir current_level, daily_limit, and status in MySQL database.
     */
    public synchronized boolean updateReservoirInDatabase(String id, int available, int dailyLimit, String status) {
        if (id == null || id.trim().isEmpty()) return false;
        String sql = "UPDATE reservoirs SET current_level = ?, daily_limit = ?, status = ? WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, Math.max(0, available));
            stmt.setDouble(2, Math.max(0, dailyLimit));
            stmt.setString(3, (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE");
            stmt.setString(4, id.trim());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadReservoirsFromDatabase();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to update reservoir in database: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // DEMAND AREA MANAGEMENT
    // =========================================================================

    public synchronized void addDemandArea(DemandArea area) {
        if (area != null) {
            demandAreas.put(area.getId(), area);
            rebuildAdjacencyList();
        }
    }

    public synchronized DemandArea getDemandArea(String id) {
        return demandAreas.get(id);
    }

    public synchronized Collection<DemandArea> getAllDemandAreas() {
        return new ArrayList<>(demandAreas.values());
    }

    public synchronized boolean removeDemandArea(String areaId) {
        DemandArea removed = demandAreas.remove(areaId);
        if (removed != null) {
            // Remove associated pipelines
            pipelines.entrySet().removeIf(entry -> entry.getValue().getToAreaId().equals(areaId));
            rebuildAdjacencyList();
            return true;
        }
        return false;
    }

    /**
     * Loads demand-area records from MySQL database using DatabaseConnection.
     * Transactional/staged loading:
     * - Parses records into a temporary map first.
     * - Maps area types to polymorphic subclasses (CriticalInstitution, ResidentialArea, CommercialArea).
     * - Preserves stable identifiers (AREA-01..AREA-05) from 'code' column.
     * - Links connected and backup reservoirs via foreign keys joined from reservoirs table.
     * - Existing collections are only updated after all records are loaded successfully.
     * - If database connection or query fails, active in-memory state is preserved safely.
     *
     * @return true if database records were loaded successfully, false otherwise.
     */
    public synchronized boolean loadDemandAreasFromDatabase() {
        String sql = "SELECT d.id, d.code, d.name, d.area_type, d.priority, d.population, " +
                     "d.minimum_demand, d.maximum_demand, d.is_critical, d.description, " +
                     "r.code AS connected_reservoir_code, " +
                     "b.code AS backup_reservoir_code " +
                     "FROM demand_areas d " +
                     "LEFT JOIN reservoirs r ON d.connected_reservoir_id = r.id " +
                     "LEFT JOIN reservoirs b ON d.backup_reservoir_id = b.id";
        Map<String, DemandArea> loadedAreas = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String name = rs.getString("name");
                String areaType = rs.getString("area_type");
                String priorityStr = rs.getString("priority");
                int population = rs.getInt("population");
                double minDemand = rs.getDouble("minimum_demand");
                double maxDemand = rs.getDouble("maximum_demand");
                boolean isCritical = rs.getBoolean("is_critical");
                String description = rs.getString("description");
                String connectedRes = rs.getString("connected_reservoir_code");
                String backupRes = rs.getString("backup_reservoir_code");

                String areaId = (code != null && !code.trim().isEmpty()) ? code.trim() : "AREA-" + dbId;
                if (name == null || name.trim().isEmpty()) {
                    name = "Demand Area " + areaId;
                }
                if (connectedRes == null || connectedRes.trim().isEmpty()) {
                    connectedRes = "RES-A";
                }

                PriorityLevel priority = PriorityLevel.fromString(priorityStr);
                int minReq = (int) Math.max(0, minDemand);
                int maxReq = (int) Math.max(minReq, maxDemand);

                DemandArea area;
                String typeUpper = (areaType != null) ? areaType.trim().toUpperCase() : "";

                if (typeUpper.contains("HOSPITAL") || isCritical) {
                    area = new CriticalInstitution(areaId, name, minReq, maxReq, PriorityLevel.CRITICAL, connectedRes, description, "911-HOSP-EMERGENCY", true);
                } else if (typeUpper.contains("SCHOOL") || typeUpper.contains("CRITICAL")) {
                    area = new CriticalInstitution(areaId, name, minReq, maxReq, priority, connectedRes, description, "SCH-ADMIN-" + dbId, false);
                } else if (typeUpper.contains("COMMERCIAL")) {
                    area = new CommercialArea(areaId, name, minReq, maxReq, priority, connectedRes, description, "General Commercial");
                } else {
                    area = new ResidentialArea(areaId, name, minReq, maxReq, priority, connectedRes, description, population > 0 ? population : 500);
                }

                if (backupRes != null && !backupRes.trim().isEmpty()) {
                    area.setBackupReservoir(backupRes.trim());
                }

                loadedAreas.put(areaId, area);
            }

            if (!loadedAreas.isEmpty()) {
                this.demandAreas.clear();
                for (Map.Entry<String, DemandArea> entry : loadedAreas.entrySet()) {
                    this.demandAreas.put(entry.getKey(), entry.getValue());
                }
                rebuildAdjacencyList();
                if (this.allocationPolicy != null) {
                    this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
                }
                System.out.println("✓ Successfully loaded " + loadedAreas.size() + " demand area(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 demand area records; retaining existing in-memory demand areas.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("⚠ Database connection/query failed when loading demand areas: " + e.getMessage());
            System.err.println("ℹ Preserving active in-memory demand area data.");
            return false;
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error loading demand areas from database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts a new Demand Area and its dedicated Feeder Pipeline directly into MySQL.
     */
    public synchronized boolean saveDemandAreaToDatabase(DemandArea area, String pipelineName, int pipeCapacity, double pipeLength) {
        if (area == null || area.getName() == null || area.getName().trim().isEmpty()) return false;
        if (area.getMinRequirement() > area.getMaxRequirement()) return false;

        String checkCodeSql = "SELECT COUNT(*) FROM demand_areas WHERE code = ?";
        String maxAreaCodeSql = "SELECT code FROM demand_areas WHERE code LIKE 'AREA-%'";
        String maxPipeCodeSql = "SELECT code FROM pipelines WHERE code LIKE 'PL-%'";
        String getResIdSql = "SELECT id FROM reservoirs WHERE code = ?";
        String insertAreaSql = "INSERT INTO demand_areas (code, name, area_type, priority, population, demand_amount, minimum_demand, maximum_demand, allocated_amount, is_critical, connected_reservoir_id, description) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.0, ?, ?, ?)";
        String insertPipeSql = "INSERT INTO pipelines (code, name, source_reservoir_id, destination_area_id, capacity, length, is_backup, status) " +
                               "VALUES (?, ?, ?, ?, ?, ?, 0, 'WORKING')";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Resolve unique Area code (e.g. AREA-06)
                String areaCode = (area.getId() != null && !area.getId().trim().isEmpty()) ? area.getId().trim() : "";
                boolean codeExists = false;
                if (!areaCode.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement(checkCodeSql)) {
                        stmt.setString(1, areaCode);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) codeExists = true;
                        }
                    }
                }
                if (areaCode.isEmpty() || codeExists) {
                    int maxNum = 5;
                    try (PreparedStatement stmt = conn.prepareStatement(maxAreaCodeSql);
                         ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String c = rs.getString("code");
                            if (c != null && c.startsWith("AREA-")) {
                                try {
                                    int n = Integer.parseInt(c.substring(5).trim());
                                    if (n > maxNum) maxNum = n;
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    areaCode = String.format("AREA-%02d", maxNum + 1);
                }
                area.setId(areaCode);

                // 2. Resolve connected_reservoir_id
                int resDbId = 1;
                String resCode = area.getConnectedReservoir();
                if (resCode == null || resCode.trim().isEmpty()) resCode = "RES-A";
                try (PreparedStatement stmt = conn.prepareStatement(getResIdSql)) {
                    stmt.setString(1, resCode.trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) resDbId = rs.getInt("id");
                    }
                }

                // 3. Determine area type, priority, is_critical, population
                String typeStr = (area.getAreaType() != null) ? area.getAreaType().toUpperCase() : "RESIDENTIAL";
                boolean isCritical = (area instanceof CriticalInstitution) || typeStr.contains("CRITICAL") || typeStr.contains("HOSPITAL");
                int population = (area instanceof ResidentialArea) ? ((ResidentialArea) area).getHouseholdCount() : 500;
                String priorityStr = (area.getEffectivePriority() != null) ? area.getEffectivePriority().name() : "MEDIUM";

                int areaDbId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(insertAreaSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, areaCode);
                    stmt.setString(2, area.getName().trim());
                    stmt.setString(3, typeStr);
                    stmt.setString(4, priorityStr);
                    stmt.setInt(5, population);
                    stmt.setDouble(6, (double) area.getMaxRequirement());
                    stmt.setDouble(7, (double) area.getMinRequirement());
                    stmt.setDouble(8, (double) area.getMaxRequirement());
                    stmt.setBoolean(9, isCritical);
                    stmt.setInt(10, resDbId);
                    stmt.setString(11, area.getDescription() != null ? area.getDescription() : "");
                    stmt.executeUpdate();
                    try (ResultSet gk = stmt.getGeneratedKeys()) {
                        if (gk.next()) areaDbId = gk.getInt(1);
                    }
                }

                if (areaDbId == -1) {
                    conn.rollback();
                    return false;
                }

                // 4. Create and insert Feeder Pipeline into pipelines table
                int maxPipeNum = 6;
                try (PreparedStatement stmt = conn.prepareStatement(maxPipeCodeSql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String c = rs.getString("code");
                        if (c != null && c.startsWith("PL-")) {
                            try {
                                int n = Integer.parseInt(c.substring(3).trim());
                                if (n > maxPipeNum) maxPipeNum = n;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                String pipeCode = String.format("PL-%02d", maxPipeNum + 1);
                String pName = (pipelineName != null && !pipelineName.trim().isEmpty()) ? pipelineName.trim() : (area.getName() + " Feeder Trunk");
                int capacity = pipeCapacity > 0 ? pipeCapacity : (area.getMaxRequirement() + 5000);
                double length = pipeLength > 0 ? pipeLength : 4.5;

                try (PreparedStatement stmt = conn.prepareStatement(insertPipeSql)) {
                    stmt.setString(1, pipeCode);
                    stmt.setString(2, pName);
                    stmt.setInt(3, resDbId);
                    stmt.setInt(4, areaDbId);
                    stmt.setDouble(5, capacity);
                    stmt.setDouble(6, length);
                    stmt.executeUpdate();
                }

                conn.commit();

                // Reload state from database
                loadDemandAreasFromDatabase();
                loadPipelinesFromDatabase();
                System.out.println("✓ Successfully created demand area " + areaCode + " and pipeline " + pipeCode + " in MySQL.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("⚠ Failed to insert demand area in database: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Database connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing Demand Area in MySQL.
     */
    public synchronized boolean updateDemandAreaInDatabase(String areaId, String name, int minReq, int maxReq, PriorityLevel priority, String resCode, String desc) {
        if (areaId == null || areaId.trim().isEmpty() || minReq > maxReq) return false;
        String getResIdSql = "SELECT id FROM reservoirs WHERE code = ?";
        String updateSql = "UPDATE demand_areas SET name = ?, minimum_demand = ?, maximum_demand = ?, demand_amount = ?, priority = ?, connected_reservoir_id = ?, description = ? WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            int resDbId = 1;
            if (resCode != null && !resCode.trim().isEmpty()) {
                try (PreparedStatement stmt = conn.prepareStatement(getResIdSql)) {
                    stmt.setString(1, resCode.trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) resDbId = rs.getInt("id");
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, name);
                stmt.setDouble(2, Math.max(0, minReq));
                stmt.setDouble(3, Math.max(minReq, maxReq));
                stmt.setDouble(4, Math.max(minReq, maxReq));
                stmt.setString(5, (priority != null) ? priority.name() : "MEDIUM");
                stmt.setInt(6, resDbId);
                stmt.setString(7, desc != null ? desc : "");
                stmt.setString(8, areaId.trim());

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    loadDemandAreasFromDatabase();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to update demand area in database: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a Demand Area and its associated pipelines from MySQL.
     */
    public synchronized boolean deleteDemandAreaFromDatabase(String areaId) {
        if (areaId == null || areaId.trim().isEmpty()) return false;
        String getAreaIdSql = "SELECT id FROM demand_areas WHERE code = ?";
        String deletePipesSql = "DELETE FROM pipelines WHERE destination_area_id = ?";
        String deleteAreaSql = "DELETE FROM demand_areas WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int areaDbId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(getAreaIdSql)) {
                    stmt.setString(1, areaId.trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) areaDbId = rs.getInt("id");
                    }
                }
                if (areaDbId == -1) {
                    conn.rollback();
                    return false;
                }

                // Delete associated pipelines
                try (PreparedStatement stmt = conn.prepareStatement(deletePipesSql)) {
                    stmt.setInt(1, areaDbId);
                    stmt.executeUpdate();
                }

                // Delete area
                try (PreparedStatement stmt = conn.prepareStatement(deleteAreaSql)) {
                    stmt.setInt(1, areaDbId);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        conn.commit();
                        loadDemandAreasFromDatabase();
                        loadPipelinesFromDatabase();
                        loadWaterRequestsFromDatabase();
                        return true;
                    }
                }
                conn.rollback();
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("⚠ Failed to delete demand area from database: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("⚠ Database connection failed: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // PIPELINE MANAGEMENT
    // =========================================================================

    public synchronized void addPipeline(Pipeline pipeline) {
        if (pipeline != null) {
            pipelines.put(pipeline.getId(), pipeline);
            rebuildAdjacencyList();
        }
    }

    public synchronized Pipeline getPipeline(String id) {
        return pipelines.get(id);
    }

    public synchronized Collection<Pipeline> getAllPipelines() {
        return new ArrayList<>(pipelines.values());
    }

    /**
     * Update pipeline health status and automatically enqueue maintenance if failed
     */
    public synchronized boolean setPipelineStatus(String pipelineId, PipelineStatus status) {
        Pipeline p = pipelines.get(pipelineId);
        if (p == null) return false;

        p.setStatus(status);

        // If pipeline is set to FAILED or UNDER_MAINTENANCE, ensure a maintenance ticket exists in FIFO queue
        if (status == PipelineStatus.FAILED || status == PipelineStatus.UNDER_MAINTENANCE) {
            boolean alreadyInQueue = false;
            for (MaintenanceRequest req : maintenanceQueue) {
                if (req.getPipelineId().equals(pipelineId) && !"Completed".equalsIgnoreCase(req.getStatus())) {
                    alreadyInQueue = true;
                    break;
                }
            }
            if (!alreadyInQueue) {
                String nextMntId = "MNT-" + (301 + maintenanceQueue.size());
                DemandArea area = demandAreas.get(p.getToAreaId());
                String areaName = area != null ? area.getName() : p.getToAreaId();
                enqueueMaintenance(new MaintenanceRequest(nextMntId, pipelineId, areaName, "Pipeline status changed to " + status.getDisplayName() + ". Dispatched for repair.", "Critical", "2026-10-25", "Queued"));
            }
        }

        rebuildAdjacencyList();
        return true;
    }

    /**
     * Loads pipeline transmission conduits from MySQL database using DatabaseConnection.
     * Transactional/staged loading:
     * - Parses records into a temporary map first.
     * - Resolves source reservoir and destination area codes via foreign key joins.
     * - Preserves stable IDs (PL-01..PL-06) and health status (WORKING, FAILED, UNDER_MAINTENANCE).
     * - Rebuilds adjacency list and updates reachability graph.
     * - If database connection or query fails, active in-memory state is preserved safely.
     *
     * @return true if database records were loaded successfully, false otherwise.
     */
    public synchronized boolean loadPipelinesFromDatabase() {
        String sql = "SELECT p.id, p.code, p.name, p.capacity, p.length, p.is_backup, p.status, " +
                     "p.source_reservoir_id, p.destination_area_id, " +
                     "r.code AS source_reservoir_code, " +
                     "d.code AS destination_area_code " +
                     "FROM pipelines p " +
                     "LEFT JOIN reservoirs r ON p.source_reservoir_id = r.id " +
                     "LEFT JOIN demand_areas d ON p.destination_area_id = d.id";
        Map<String, Pipeline> loadedPipelines = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String name = rs.getString("name");
                double capacity = rs.getDouble("capacity");
                double length = rs.getDouble("length");
                String statusStr = rs.getString("status");
                String sourceRes = rs.getString("source_reservoir_code");
                String destArea = rs.getString("destination_area_code");

                String pipeId = (code != null && !code.trim().isEmpty()) ? code.trim() : "PL-" + dbId;
                if (name == null || name.trim().isEmpty()) {
                    name = "Pipeline " + pipeId;
                }
                if (sourceRes == null || sourceRes.trim().isEmpty()) {
                    sourceRes = "RES-" + rs.getInt("source_reservoir_id");
                }
                if (destArea == null || destArea.trim().isEmpty()) {
                    destArea = "AREA-" + rs.getInt("destination_area_id");
                }

                PipelineStatus status = PipelineStatus.fromString(statusStr);
                Pipeline pipeline = new Pipeline(pipeId, name, sourceRes.trim(), destArea.trim(), (int) Math.max(0, capacity), status, Math.max(0.0, length));
                loadedPipelines.put(pipeId, pipeline);
            }

            if (!loadedPipelines.isEmpty()) {
                this.pipelines.clear();
                for (Map.Entry<String, Pipeline> entry : loadedPipelines.entrySet()) {
                    this.pipelines.put(entry.getKey(), entry.getValue());
                }
                rebuildAdjacencyList();
                if (this.allocationPolicy != null) {
                    this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
                }
                System.out.println("✓ Successfully loaded " + loadedPipelines.size() + " pipeline(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 pipeline records; retaining existing in-memory pipelines.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("⚠ Database connection/query failed when loading pipelines: " + e.getMessage());
            System.err.println("ℹ Preserving active in-memory pipeline data.");
            return false;
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error loading pipelines from database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates pipeline health status in MySQL and enqueues a maintenance request in MySQL if failed.
     */
    public synchronized boolean updatePipelineStatusInDatabase(String pipelineId, PipelineStatus status) {
        if (pipelineId == null || pipelineId.trim().isEmpty() || status == null) return false;
        String updateSql = "UPDATE pipelines SET status = ? WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, pipelineId.trim());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                if (status == PipelineStatus.FAILED || status == PipelineStatus.UNDER_MAINTENANCE) {
                    ensureMaintenanceJobInDatabase(conn, pipelineId.trim(), status);
                }
                loadPipelinesFromDatabase();
                loadMaintenanceRequestsFromDatabase();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to update pipeline status in database: " + e.getMessage());
        }
        return false;
    }

    private void ensureMaintenanceJobInDatabase(Connection conn, String pipelineCode, PipelineStatus status) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM maintenance_requests m JOIN pipelines p ON m.pipeline_id = p.id WHERE p.code = ? AND m.status != 'COMPLETED'";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, pipelineCode);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }

        int pipeDbId = -1;
        int areaDbId = 1;
        String areaName = "";
        String pipeDetailsSql = "SELECT p.id, p.destination_area_id, d.name AS area_name FROM pipelines p LEFT JOIN demand_areas d ON p.destination_area_id = d.id WHERE p.code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(pipeDetailsSql)) {
            stmt.setString(1, pipelineCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    pipeDbId = rs.getInt("id");
                    areaDbId = rs.getInt("destination_area_id");
                    areaName = rs.getString("area_name");
                }
            }
        }

        if (pipeDbId != -1) {
            String maxMntCodeSql = "SELECT code FROM maintenance_requests WHERE code LIKE 'MNT-%'";
            int maxNum = 300;
            try (PreparedStatement stmt = conn.prepareStatement(maxMntCodeSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String c = rs.getString("code");
                    if (c != null && c.startsWith("MNT-")) {
                        try {
                            int n = Integer.parseInt(c.substring(4).trim());
                            if (n > maxNum) maxNum = n;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            String nextCode = "MNT-" + (maxNum + 1);
            String desc = "Pipeline status changed to " + status.getDisplayName() + ". Dispatched for repair.";

            String insertMntSql = "INSERT INTO maintenance_requests (code, pipeline_id, affected_area_id, affected_area, description, priority, status, created_at) " +
                                  "VALUES (?, ?, ?, ?, ?, 'CRITICAL', 'QUEUED', CURRENT_TIMESTAMP)";
            try (PreparedStatement stmt = conn.prepareStatement(insertMntSql)) {
                stmt.setString(1, nextCode);
                stmt.setInt(2, pipeDbId);
                stmt.setInt(3, areaDbId);
                stmt.setString(4, (areaName != null && !areaName.isEmpty()) ? areaName : pipelineCode);
                stmt.setString(5, desc);
                stmt.executeUpdate();
            }
        }
    }

    // =========================================================================
    // FIFO MAINTENANCE QUEUE (ArrayDeque Implementation)
    // =========================================================================

    /**
     * Enqueue a new maintenance request at the TAIL of the FIFO queue (offerLast)
     */
    public synchronized void enqueueMaintenance(MaintenanceRequest request) {
        if (request != null) {
            maintenanceQueue.offerLast(request);
        }
    }

    public synchronized List<MaintenanceRequest> getMaintenanceQueueList() {
        return new ArrayList<>(maintenanceQueue);
    }

    /**
     * Process the next task at the FRONT of the FIFO queue (peekFirst / pollFirst)
     * Demonstrates FIFO repair sequence:
     * - If Queued -> moves to In Progress
     * - If In Progress -> Completed, and RESTORES pipeline status to WORKING!
     */
    public synchronized MaintenanceRequest processNextMaintenanceFIFO() {
        // Find the first task in queue that is not completed
        for (MaintenanceRequest job : maintenanceQueue) {
            if (!"Completed".equalsIgnoreCase(job.getStatus())) {
                if ("Queued".equalsIgnoreCase(job.getStatus())) {
                    job.setStatus("In Progress");
                    return job;
                } else if ("In Progress".equalsIgnoreCase(job.getStatus())) {
                    job.setStatus("Completed");

                    // Restore the pipeline in the network to WORKING!
                    Pipeline p = pipelines.get(job.getPipelineId());
                    if (p != null) {
                        p.setStatus(PipelineStatus.WORKING);
                        rebuildAdjacencyList();
                    }
                    return job;
                }
            }
        }
        return null; // Queue has no pending tasks
    }

    /**
     * Mark a specific maintenance job as repaired and restore pipeline
     */
    public synchronized boolean markMaintenanceRepaired(String jobId) {
        for (MaintenanceRequest job : maintenanceQueue) {
            if (job.getId().equals(jobId)) {
                job.setStatus("Completed");
                Pipeline p = pipelines.get(job.getPipelineId());
                if (p != null) {
                    p.setStatus(PipelineStatus.WORKING);
                    rebuildAdjacencyList();
                }
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // WATER REQUESTS & PRIORITY QUEUE
    // =========================================================================

    public synchronized void addWaterRequest(WaterRequest request) {
        if (request != null) {
            if (request.getArrivalOrder() <= 0) {
                request.setArrivalOrder(++requestArrivalCounter);
            }
            requestHistory.add(request);
        }
    }

    public synchronized List<WaterRequest> getRequestHistory() {
        return new ArrayList<>(requestHistory);
    }

    /**
     * Builds and returns a Java PriorityQueue ordered by 5 priority tiers + FIFO tie breaker
     */
    public synchronized PriorityQueue<WaterRequest> buildRequestPriorityQueue() {
        PriorityQueue<WaterRequest> pq = new PriorityQueue<>();
        pq.addAll(requestHistory);
        return pq;
    }

    /**
     * Loads water demand requests from MySQL database using DatabaseConnection.
     * Transactional/staged loading:
     * - Parses records into a temporary list first.
     * - Joins demand_areas to resolve area code and name.
     * - Preserves stable request codes (e.g. REQ-101) and arrival order for PriorityQueue.
     * - Existing collections are only updated after all records are loaded successfully.
     * - If database connection or query fails, active in-memory state is preserved safely.
     *
     * @return true if database records were loaded successfully, false otherwise.
     */
    public synchronized boolean loadWaterRequestsFromDatabase() {
        String sql = "SELECT r.id, r.code, r.user_id, r.requester_name, r.area_id, " +
                     "r.requested_amount, r.priority, r.status, r.request_date, r.remarks, r.arrival_order, " +
                     "d.code AS area_code, d.name AS area_name " +
                     "FROM water_requests r " +
                     "LEFT JOIN demand_areas d ON r.area_id = d.id " +
                     "ORDER BY r.arrival_order ASC, r.id ASC";

        List<WaterRequest> loadedRequests = new ArrayList<>();
        long maxOrder = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String requesterName = rs.getString("requester_name");
                int areaDbId = rs.getInt("area_id");
                String areaCode = rs.getString("area_code");
                String areaName = rs.getString("area_name");
                double requestedAmount = rs.getDouble("requested_amount");
                String priorityStr = rs.getString("priority");
                String statusStr = rs.getString("status");
                Timestamp reqTimestamp = rs.getTimestamp("request_date");
                String remarks = rs.getString("remarks");
                long arrivalOrder = rs.getLong("arrival_order");

                String reqId = (code != null && !code.trim().isEmpty()) ? code.trim() : "REQ-" + dbId;
                if (requesterName == null || requesterName.trim().isEmpty()) {
                    requesterName = "Resident";
                }
                String finalAreaId = (areaCode != null && !areaCode.trim().isEmpty()) ? areaCode.trim() : "AREA-" + areaDbId;
                if (areaName == null || areaName.trim().isEmpty()) {
                    DemandArea da = demandAreas.get(finalAreaId);
                    areaName = (da != null) ? da.getName() : finalAreaId;
                }
                int quantity = (int) Math.max(0, requestedAmount);
                PriorityLevel priority = PriorityLevel.fromString(priorityStr);
                String dateStr = "2026-10-25";
                if (reqTimestamp != null) {
                    dateStr = reqTimestamp.toString();
                    if (dateStr.length() >= 10) {
                        dateStr = dateStr.substring(0, 10);
                    }
                }
                if (arrivalOrder <= 0) {
                    arrivalOrder = loadedRequests.size() + 1;
                }
                if (arrivalOrder > maxOrder) {
                    maxOrder = arrivalOrder;
                }

                String finalStatus = "Pending";
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    String s = statusStr.trim();
                    if ("PENDING".equalsIgnoreCase(s)) finalStatus = "Pending";
                    else if ("ALLOCATED".equalsIgnoreCase(s)) finalStatus = "Allocated";
                    else if ("PARTIALLY_ALLOCATED".equalsIgnoreCase(s) || "PARTIALLY ALLOCATED".equalsIgnoreCase(s)) finalStatus = "Partially Allocated";
                    else finalStatus = s;
                }

                WaterRequest request = new WaterRequest(reqId, requesterName, finalAreaId, areaName, quantity, priority, remarks != null ? remarks : "", dateStr, arrivalOrder);
                request.setStatus(finalStatus);
                loadedRequests.add(request);
            }

            if (!loadedRequests.isEmpty()) {
                this.requestHistory.clear();
                this.requestHistory.addAll(loadedRequests);
                this.requestArrivalCounter = Math.max(this.requestArrivalCounter, maxOrder);
                if (this.allocationPolicy != null) {
                    this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
                }
                System.out.println("✓ Successfully loaded " + loadedRequests.size() + " water request(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 water request records; retaining existing in-memory requests.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("⚠ Database connection/query failed when loading water requests: " + e.getMessage());
            System.err.println("ℹ Preserving active in-memory water request data.");
            return false;
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error loading water requests from database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts a newly submitted water request into the MySQL database using DatabaseConnection.
     * - Validates area existence and resolves area foreign key ID from demand_areas table.
     * - Generates unique request code (REQ-xxx) if needed and prevents duplicate codes.
     * - Computes arrival order sequence.
     * - Appends to in-memory requestHistory and updates allocation summary upon successful insert.
     * - If database connection or query fails, leaves in-memory state unchanged and returns false.
     *
     * @param req The water request to persist
     * @return true if row was successfully inserted into database, false otherwise.
     */
    public synchronized boolean saveWaterRequestToDatabase(WaterRequest req) {
        if (req == null) return false;
        if (req.getAreaId() == null || req.getAreaId().trim().isEmpty() || req.getQuantity() <= 0) {
            System.err.println("⚠ Invalid water request data: Area and positive quantity required.");
            return false;
        }

        String checkAreaSql = "SELECT id, code, name FROM demand_areas WHERE code = ?";
        String checkCodeSql = "SELECT COUNT(*) FROM water_requests WHERE code = ?";
        String maxCodeSql = "SELECT code FROM water_requests WHERE code LIKE 'REQ-%'";
        String maxOrderSql = "SELECT COALESCE(MAX(arrival_order), 0) + 1 FROM water_requests";
        String insertSql = "INSERT INTO water_requests (code, user_id, requester_name, area_id, requested_amount, priority, status, request_date, remarks, arrival_order) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Resolve area foreign key integer ID
            int areaDbId = -1;
            String resolvedAreaCode = req.getAreaId().trim();
            String resolvedAreaName = req.getAreaName();

            try (PreparedStatement areaStmt = conn.prepareStatement(checkAreaSql)) {
                areaStmt.setString(1, resolvedAreaCode);
                try (ResultSet rs = areaStmt.executeQuery()) {
                    if (rs.next()) {
                        areaDbId = rs.getInt("id");
                        resolvedAreaCode = rs.getString("code");
                        resolvedAreaName = rs.getString("name");
                    }
                }
            }

            // Fallback: check if areaId was passed as an integer ID
            if (areaDbId == -1) {
                try {
                    int parsedId = Integer.parseInt(resolvedAreaCode);
                    String checkByIdSql = "SELECT id, code, name FROM demand_areas WHERE id = ?";
                    try (PreparedStatement areaIdStmt = conn.prepareStatement(checkByIdSql)) {
                        areaIdStmt.setInt(1, parsedId);
                        try (ResultSet rs = areaIdStmt.executeQuery()) {
                            if (rs.next()) {
                                areaDbId = rs.getInt("id");
                                resolvedAreaCode = rs.getString("code");
                                resolvedAreaName = rs.getString("name");
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (areaDbId == -1) {
                System.err.println("⚠ Cannot save request: Demand area '" + req.getAreaId() + "' not found in database.");
                return false;
            }

            // 2. Ensure unique request code (REQ-xxx)
            String requestCode = (req.getId() != null && !req.getId().trim().isEmpty()) ? req.getId().trim() : "";
            boolean codeExists = false;
            if (!requestCode.isEmpty()) {
                try (PreparedStatement checkStmt = conn.prepareStatement(checkCodeSql)) {
                    checkStmt.setString(1, requestCode);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            codeExists = true;
                        }
                    }
                }
            }

            if (requestCode.isEmpty() || codeExists) {
                int maxNum = 100;
                try (PreparedStatement maxStmt = conn.prepareStatement(maxCodeSql);
                     ResultSet rs = maxStmt.executeQuery()) {
                    while (rs.next()) {
                        String c = rs.getString("code");
                        if (c != null && c.startsWith("REQ-")) {
                            try {
                                int num = Integer.parseInt(c.substring(4).trim());
                                if (num > maxNum) maxNum = num;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                requestCode = "REQ-" + (maxNum + 1);
            }
            req.setId(requestCode);

            // 3. Compute arrival order
            long nextArrivalOrder = 1;
            try (PreparedStatement orderStmt = conn.prepareStatement(maxOrderSql);
                 ResultSet rs = orderStmt.executeQuery()) {
                if (rs.next()) {
                    nextArrivalOrder = rs.getLong(1);
                }
            }
            if (nextArrivalOrder <= this.requestArrivalCounter) {
                nextArrivalOrder = this.requestArrivalCounter + 1;
            }
            req.setArrivalOrder(nextArrivalOrder);

            // 4. Parse request date
            Timestamp requestTimestamp;
            try {
                String d = req.getDate();
                if (d != null && d.trim().length() == 10) {
                    requestTimestamp = Timestamp.valueOf(d.trim() + " 00:00:00");
                } else if (d != null && d.trim().length() > 10) {
                    requestTimestamp = Timestamp.valueOf(d.trim());
                } else {
                    requestTimestamp = new Timestamp(System.currentTimeMillis());
                }
            } catch (Exception ignored) {
                requestTimestamp = new Timestamp(System.currentTimeMillis());
            }

            // 5. Insert into water_requests table
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, requestCode);
                insertStmt.setNull(2, Types.INTEGER); // user_id is nullable
                insertStmt.setString(3, req.getUserName() != null ? req.getUserName() : "Resident");
                insertStmt.setInt(4, areaDbId);
                insertStmt.setDouble(5, req.getQuantity());
                insertStmt.setString(6, req.getPriority() != null ? req.getPriority().name() : "MEDIUM");
                insertStmt.setString(7, (req.getStatus() != null && !req.getStatus().trim().isEmpty()) ? req.getStatus().toUpperCase() : "PENDING");
                insertStmt.setTimestamp(8, requestTimestamp);
                insertStmt.setString(9, req.getNotes() != null ? req.getNotes() : "");
                insertStmt.setLong(10, nextArrivalOrder);

                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Update in-memory state
                    req.setAreaId(resolvedAreaCode);
                    if (resolvedAreaName != null && !resolvedAreaName.isEmpty()) {
                        req.setAreaName(resolvedAreaName);
                    }
                    this.requestArrivalCounter = Math.max(this.requestArrivalCounter, nextArrivalOrder);
                    this.requestHistory.add(req);
                    if (this.allocationPolicy != null) {
                        this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
                    }
                    System.out.println("✓ Successfully inserted water request " + requestCode + " into MySQL database.");
                    return true;
                } else {
                    System.err.println("⚠ Failed to insert water request " + requestCode + " (0 rows affected).");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("⚠ Database connection/insert failed for water request: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error saving water request to database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a water demand request from MySQL by code.
     */
    public synchronized boolean deleteWaterRequestFromDatabase(String reqId) {
        if (reqId == null || reqId.trim().isEmpty()) return false;
        String deleteSql = "DELETE FROM water_requests WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, reqId.trim());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadWaterRequestsFromDatabase();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to delete water request from database: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // COMPLAINTS
    // =========================================================================

    public synchronized void addComplaint(Complaint complaint) {
        if (complaint != null) {
            complaints.add(complaint);
        }
    }

    public synchronized List<Complaint> getComplaints() {
        return new ArrayList<>(complaints);
    }

    public synchronized boolean updateComplaintStatus(String complaintId, String newStatus) {
        for (Complaint c : complaints) {
            if (c.getId().equals(complaintId)) {
                c.setStatus(newStatus);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean loadComplaintsFromDatabase() {
        String sql = "SELECT c.id, c.code, c.reporter_name, c.area_id, c.subject, c.complaint_type, c.description, c.status, c.created_at, " +
                     "d.code AS area_code, d.name AS area_name " +
                     "FROM complaints c " +
                     "LEFT JOIN demand_areas d ON c.area_id = d.id " +
                     "ORDER BY c.id ASC";
        List<Complaint> loaded = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String user = rs.getString("reporter_name");
                String areaCode = rs.getString("area_code");
                String areaName = rs.getString("area_name");
                String type = rs.getString("complaint_type");
                String desc = rs.getString("description");
                String status = rs.getString("status");
                Timestamp createdAt = rs.getTimestamp("created_at");

                String cmpId = (code != null && !code.trim().isEmpty()) ? code.trim() : "CMP-" + dbId;
                if (user == null || user.trim().isEmpty()) user = "Resident";
                String finalAreaId = (areaCode != null && !areaCode.trim().isEmpty()) ? areaCode.trim() : "AREA-" + rs.getInt("area_id");
                if (areaName == null || areaName.trim().isEmpty()) {
                    DemandArea da = demandAreas.get(finalAreaId);
                    areaName = (da != null) ? da.getName() : finalAreaId;
                }
                String dateStr = "2026-10-25";
                if (createdAt != null) {
                    dateStr = createdAt.toString();
                    if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
                }
                String finalStatus = "Open";
                if (status != null && !status.trim().isEmpty()) {
                    String s = status.trim();
                    if ("OPEN".equalsIgnoreCase(s)) finalStatus = "Open";
                    else if ("IN_PROGRESS".equalsIgnoreCase(s) || "IN PROGRESS".equalsIgnoreCase(s)) finalStatus = "In Progress";
                    else if ("RESOLVED".equalsIgnoreCase(s)) finalStatus = "Resolved";
                    else finalStatus = s;
                }

                loaded.add(new Complaint(cmpId, user, finalAreaId, areaName, type != null ? type : "Water Not Supplied", desc != null ? desc : "", dateStr, finalStatus));
            }

            if (!loaded.isEmpty()) {
                this.complaints.clear();
                this.complaints.addAll(loaded);
                System.out.println("✓ Successfully loaded " + loaded.size() + " complaint(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 complaints; retaining existing in-memory complaints.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to load complaints from database: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean saveComplaintToDatabase(Complaint c) {
        if (c == null || c.getAreaId() == null || c.getDescription() == null || c.getDescription().trim().isEmpty()) return false;
        String getAreaSql = "SELECT id, name FROM demand_areas WHERE code = ?";
        String maxCodeSql = "SELECT code FROM complaints WHERE code LIKE 'CMP-%'";
        String insertSql = "INSERT INTO complaints (code, user_id, reporter_name, area_id, subject, complaint_type, description, status, created_at) " +
                           "VALUES (?, NULL, ?, ?, ?, ?, ?, 'OPEN', CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            int areaDbId = 1;
            String areaName = c.getAreaName();
            try (PreparedStatement stmt = conn.prepareStatement(getAreaSql)) {
                stmt.setString(1, c.getAreaId().trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        areaDbId = rs.getInt("id");
                        areaName = rs.getString("name");
                    }
                }
            }

            int maxNum = 200;
            try (PreparedStatement stmt = conn.prepareStatement(maxCodeSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    if (code != null && code.startsWith("CMP-")) {
                        try {
                            int n = Integer.parseInt(code.substring(4).trim());
                            if (n > maxNum) maxNum = n;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            String code = "CMP-" + (maxNum + 1);
            c.setId(code);

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, code);
                stmt.setString(2, c.getUserName() != null ? c.getUserName() : "Resident");
                stmt.setInt(3, areaDbId);
                stmt.setString(4, c.getType() != null ? c.getType() : "Water Supply Issue");
                stmt.setString(5, c.getType() != null ? c.getType() : "Water Not Supplied");
                stmt.setString(6, c.getDescription().trim());
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    loadComplaintsFromDatabase();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to save complaint to database: " + e.getMessage());
        }
        return false;
    }

    public synchronized boolean updateComplaintStatusInDatabase(String complaintId, String newStatus) {
        if (complaintId == null || complaintId.trim().isEmpty() || newStatus == null) return false;
        String statusUpper = newStatus.trim().toUpperCase().replace(" ", "_");
        boolean isResolved = statusUpper.contains("RESOLVED");
        String sql = "UPDATE complaints SET status = ?, resolved_at = " + (isResolved ? "CURRENT_TIMESTAMP" : "NULL") + " WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statusUpper);
            stmt.setString(2, complaintId.trim());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                loadComplaintsFromDatabase();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to update complaint status in database: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // MAINTENANCE REQUESTS & FIFO QUEUE DATABASE OPERATIONS
    // =========================================================================

    public synchronized boolean loadMaintenanceRequestsFromDatabase() {
        String sql = "SELECT m.id, m.code, m.pipeline_id, m.affected_area_id, m.affected_area, m.description, m.priority, m.status, m.created_at, m.completed_at, " +
                     "p.code AS pipeline_code, d.name AS area_name " +
                     "FROM maintenance_requests m " +
                     "LEFT JOIN pipelines p ON m.pipeline_id = p.id " +
                     "LEFT JOIN demand_areas d ON m.affected_area_id = d.id " +
                     "ORDER BY m.id ASC";
        ArrayDeque<MaintenanceRequest> loaded = new ArrayDeque<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dbId = rs.getInt("id");
                String code = rs.getString("code");
                String pipeCode = rs.getString("pipeline_code");
                String affectedArea = rs.getString("affected_area");
                String desc = rs.getString("description");
                String priority = rs.getString("priority");
                String status = rs.getString("status");
                Timestamp createdAt = rs.getTimestamp("created_at");

                String mntId = (code != null && !code.trim().isEmpty()) ? code.trim() : "MNT-" + dbId;
                String pCode = (pipeCode != null && !pipeCode.trim().isEmpty()) ? pipeCode.trim() : "PL-" + rs.getInt("pipeline_id");
                String dateStr = "2026-10-25";
                if (createdAt != null) {
                    dateStr = createdAt.toString();
                    if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
                }
                String finalStatus = "Queued";
                if (status != null && !status.trim().isEmpty()) {
                    String s = status.trim();
                    if ("QUEUED".equalsIgnoreCase(s)) finalStatus = "Queued";
                    else if ("IN_PROGRESS".equalsIgnoreCase(s) || "IN PROGRESS".equalsIgnoreCase(s)) finalStatus = "In Progress";
                    else if ("COMPLETED".equalsIgnoreCase(s)) finalStatus = "Completed";
                    else finalStatus = s;
                }

                loaded.offerLast(new MaintenanceRequest(mntId, pCode, affectedArea != null ? affectedArea : "Main Grid", desc != null ? desc : "", priority != null ? priority : "Medium", dateStr, finalStatus));
            }

            if (!loaded.isEmpty()) {
                this.maintenanceQueue.clear();
                this.maintenanceQueue.addAll(loaded);
                System.out.println("✓ Successfully loaded " + loaded.size() + " maintenance request(s) from database.");
                return true;
            } else {
                System.out.println("ℹ Database returned 0 maintenance requests; retaining existing in-memory queue.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Failed to load maintenance requests from database: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean saveMaintenanceRequestToDatabase(MaintenanceRequest req) {
        if (req == null || req.getPipelineId() == null || req.getProblemDescription() == null) return false;
        String getPipeSql = "SELECT p.id, p.destination_area_id, d.name AS area_name FROM pipelines p LEFT JOIN demand_areas d ON p.destination_area_id = d.id WHERE p.code = ?";
        String maxCodeSql = "SELECT code FROM maintenance_requests WHERE code LIKE 'MNT-%'";
        String insertSql = "INSERT INTO maintenance_requests (code, pipeline_id, affected_area_id, affected_area, description, priority, status, created_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, 'QUEUED', CURRENT_TIMESTAMP)";
        String updatePipeSql = "UPDATE pipelines SET status = 'UNDER_MAINTENANCE' WHERE id = ? AND status = 'WORKING'";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int pipeDbId = -1;
                int areaDbId = 1;
                String areaName = req.getAffectedArea();
                try (PreparedStatement stmt = conn.prepareStatement(getPipeSql)) {
                    stmt.setString(1, req.getPipelineId().trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            pipeDbId = rs.getInt("id");
                            areaDbId = rs.getInt("destination_area_id");
                            if (areaName == null || areaName.isEmpty()) areaName = rs.getString("area_name");
                        }
                    }
                }
                if (pipeDbId == -1) {
                    conn.rollback();
                    return false;
                }

                int maxNum = 300;
                try (PreparedStatement stmt = conn.prepareStatement(maxCodeSql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString("code");
                        if (code != null && code.startsWith("MNT-")) {
                            try {
                                int n = Integer.parseInt(code.substring(4).trim());
                                if (n > maxNum) maxNum = n;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                String code = "MNT-" + (maxNum + 1);
                req.setId(code);

                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, code);
                    stmt.setInt(2, pipeDbId);
                    stmt.setInt(3, areaDbId);
                    stmt.setString(4, areaName != null ? areaName : req.getPipelineId());
                    stmt.setString(5, req.getProblemDescription().trim());
                    stmt.setString(6, req.getPriority() != null ? req.getPriority().toUpperCase() : "MEDIUM");
                    stmt.executeUpdate();
                }

                // Mark pipeline as UNDER_MAINTENANCE if currently working
                try (PreparedStatement stmt = conn.prepareStatement(updatePipeSql)) {
                    stmt.setInt(1, pipeDbId);
                    stmt.executeUpdate();
                }

                conn.commit();
                loadMaintenanceRequestsFromDatabase();
                loadPipelinesFromDatabase();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("⚠ Failed to save maintenance request in database: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("⚠ Database connection failed: " + e.getMessage());
        }
        return false;
    }

    public synchronized MaintenanceRequest processNextMaintenanceFIFOInDatabase() {
        MaintenanceRequest target = null;
        for (MaintenanceRequest job : maintenanceQueue) {
            if (!"Completed".equalsIgnoreCase(job.getStatus())) {
                target = job;
                break;
            }
        }
        if (target == null) return null;

        String code = target.getId();
        if ("Queued".equalsIgnoreCase(target.getStatus())) {
            String sql = "UPDATE maintenance_requests SET status = 'IN_PROGRESS' WHERE code = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code);
                stmt.executeUpdate();
                loadMaintenanceRequestsFromDatabase();
                target.setStatus("In Progress");
                return target;
            } catch (SQLException e) {
                System.err.println("⚠ Failed to update maintenance job in database: " + e.getMessage());
            }
        } else if ("In Progress".equalsIgnoreCase(target.getStatus())) {
            String sqlMnt = "UPDATE maintenance_requests SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE code = ?";
            String sqlPipe = "UPDATE pipelines SET status = 'WORKING' WHERE code = ?";
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement stmt = conn.prepareStatement(sqlMnt)) {
                        stmt.setString(1, code);
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(sqlPipe)) {
                        stmt.setString(1, target.getPipelineId());
                        stmt.executeUpdate();
                    }
                    conn.commit();
                    loadMaintenanceRequestsFromDatabase();
                    loadPipelinesFromDatabase();
                    target.setStatus("Completed");
                    return target;
                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("⚠ Failed to complete maintenance job in database: " + e.getMessage());
                }
            } catch (SQLException e) {
                System.err.println("⚠ Database connection failed: " + e.getMessage());
            }
        }
        return null;
    }

    public synchronized boolean markMaintenanceRepairedInDatabase(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) return false;
        String findPipeSql = "SELECT p.code AS pipe_code FROM maintenance_requests m JOIN pipelines p ON m.pipeline_id = p.id WHERE m.code = ?";
        String updateMntSql = "UPDATE maintenance_requests SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE code = ?";
        String updatePipeSql = "UPDATE pipelines SET status = 'WORKING' WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String pipeCode = "";
                try (PreparedStatement stmt = conn.prepareStatement(findPipeSql)) {
                    stmt.setString(1, jobId.trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) pipeCode = rs.getString("pipe_code");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateMntSql)) {
                    stmt.setString(1, jobId.trim());
                    stmt.executeUpdate();
                }

                if (!pipeCode.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement(updatePipeSql)) {
                        stmt.setString(1, pipeCode);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();
                loadMaintenanceRequestsFromDatabase();
                loadPipelinesFromDatabase();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("⚠ Failed to repair maintenance job in database: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("⚠ Database connection failed: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // ALLOCATION ENGINE (PriorityQueue-driven)
    // =========================================================================

    public synchronized AllocationSummary runAllocation(boolean commitDeductions) {
        this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, commitDeductions);
        if (commitDeductions && this.latestAllocationSummary != null) {
            persistAllocationsToDatabase(this.latestAllocationSummary);
        }
        return this.latestAllocationSummary;
    }

    private void persistAllocationsToDatabase(AllocationSummary summary) {
        if (summary == null || summary.getRecords() == null) return;
        String batchId = "BATCH-" + System.currentTimeMillis();
        String getResIdSql = "SELECT id FROM reservoirs WHERE code = ?";
        String getAreaIdSql = "SELECT id FROM demand_areas WHERE code = ?";
        String getReqIdSql = "SELECT id FROM water_requests WHERE area_id = ? AND status = 'PENDING' ORDER BY id ASC LIMIT 1";
        String insertAllocSql = "INSERT INTO water_allocations (reservoir_id, area_id, request_id, allocation_batch_id, requested_amount, allocated_amount, unmet_amount, priority, connectivity_status, supplied_by, status, allocation_date) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        String updateAreaSql = "UPDATE demand_areas SET allocated_amount = ? WHERE code = ?";
        String updateResSql = "UPDATE reservoirs SET current_level = ? WHERE code = ?";
        String updateReqSql = "UPDATE water_requests SET status = ? WHERE area_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update live reservoirs current_level in MySQL
                for (Reservoir r : reservoirs.values()) {
                    try (PreparedStatement stmt = conn.prepareStatement(updateResSql)) {
                        stmt.setDouble(1, (double) r.getAvailable());
                        stmt.setString(2, r.getId());
                        stmt.executeUpdate();
                    }
                }

                for (AllocationRecord rec : summary.getRecords()) {
                    int areaDbId = -1;
                    try (PreparedStatement stmt = conn.prepareStatement(getAreaIdSql)) {
                        stmt.setString(1, rec.getAreaId());
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) areaDbId = rs.getInt("id");
                        }
                    }

                    int resDbId = 1;
                    if (rec.getSuppliedBy() != null && rec.getSuppliedBy().startsWith("RES-")) {
                        String rCode = rec.getSuppliedBy().substring(0, 5);
                        try (PreparedStatement stmt = conn.prepareStatement(getResIdSql)) {
                            stmt.setString(1, rCode);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) resDbId = rs.getInt("id");
                            }
                        }
                    }

                    Integer reqDbId = null;
                    if (areaDbId != -1) {
                        try (PreparedStatement stmt = conn.prepareStatement(getReqIdSql)) {
                            stmt.setInt(1, areaDbId);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) reqDbId = rs.getInt("id");
                            }
                        }
                    }

                    // Update area allocated amount in MySQL
                    try (PreparedStatement stmt = conn.prepareStatement(updateAreaSql)) {
                        stmt.setDouble(1, (double) rec.getAllocated());
                        stmt.setString(2, rec.getAreaId());
                        stmt.executeUpdate();
                    }

                    // Update request status in MySQL
                    if (areaDbId != -1) {
                        String reqStatus = "PENDING";
                        if ("Fully Allocated".equals(rec.getStatus())) reqStatus = "ALLOCATED";
                        else if ("Partially Allocated".equals(rec.getStatus())) reqStatus = "PARTIALLY_ALLOCATED";
                        try (PreparedStatement stmt = conn.prepareStatement(updateReqSql)) {
                            stmt.setString(1, reqStatus);
                            stmt.setInt(2, areaDbId);
                            stmt.executeUpdate();
                        }
                    }

                    // Insert allocation record
                    try (PreparedStatement stmt = conn.prepareStatement(insertAllocSql)) {
                        stmt.setInt(1, resDbId);
                        if (areaDbId != -1) stmt.setInt(2, areaDbId);
                        else stmt.setNull(2, Types.INTEGER);
                        if (reqDbId != null) stmt.setInt(3, reqDbId);
                        else stmt.setNull(3, Types.INTEGER);
                        stmt.setString(4, batchId);
                        stmt.setDouble(5, (double) rec.getRequested());
                        stmt.setDouble(6, (double) rec.getAllocated());
                        stmt.setDouble(7, (double) rec.getUnmet());
                        stmt.setString(8, rec.getPriority());
                        stmt.setString(9, rec.getConnectivity());
                        stmt.setString(10, rec.getSuppliedBy());
                        stmt.setString(11, rec.getStatus());
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                System.out.println("✓ Successfully persisted allocation batch " + batchId + " into MySQL water_allocations.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("⚠ Failed to persist allocation records into MySQL: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("⚠ Database connection failed for allocation persist: " + e.getMessage());
        }
    }

    public synchronized AllocationSummary getLatestAllocationSummary() {
        if (this.latestAllocationSummary == null) {
            this.latestAllocationSummary = this.allocationPolicy.allocateWater(this, false);
        }
        return this.latestAllocationSummary;
    }

    public synchronized void setAllocationPolicy(WaterAllocationPolicy policy) {
        if (policy != null) {
            this.allocationPolicy = policy;
        }
    }
}
