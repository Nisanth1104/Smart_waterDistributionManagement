package com.smartwater;

import com.smartwater.network.WaterNetwork;
import com.smartwater.server.SimpleHttpServer;

import java.io.IOException;

/**
 * Main Class
 * Entry point for starting the Smart Water Distribution Management System server.
 */
public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static SimpleHttpServer server;
    private static WaterNetwork network;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            startServer(port);
        } catch (IOException e) {
            System.err.println("❌ Failed to start Smart Water Distribution server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void startServer(int port) throws IOException {
        network = new WaterNetwork();
        network.loadReservoirsFromDatabase();
        network.loadDemandAreasFromDatabase();
        network.loadPipelinesFromDatabase();
        network.loadWaterRequestsFromDatabase();
        network.loadComplaintsFromDatabase();
        network.loadMaintenanceRequestsFromDatabase();
        server = new SimpleHttpServer(port, network);

        server.start();

        System.out.println("===============================================================================");
        System.out.println("  💧 SMART WATER DISTRIBUTION MANAGEMENT SYSTEM (JAVA BACKEND) 💧             ");
        System.out.println("===============================================================================");
        System.out.println("  ✓ Java Server Running at:       http://localhost:" + port + "/");
        System.out.println("  ✓ REST API Endpoints active at: http://localhost:" + port + "/api/");
        System.out.println("  ✓ Frontend UI served at:        http://localhost:" + port + "/index.html");
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("  Data Structures Initialized in Java Memory:");
        System.out.println("    - Adjacency List Graph:       Reservoir-to-Area pipeline routing");
        System.out.println("    - Graph BFS Traversal:        Reachable & Disconnected area detection");
        System.out.println("    - PriorityQueue:              5-Tier priority water allocation");
        System.out.println("    - ArrayDeque:                 FIFO pipeline maintenance dispatch");
        System.out.println("    - HashMap & ArrayList:        O(1) lookups and historical audit logs");
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("  Press Ctrl+C to stop the server.");
        System.out.println("===============================================================================");
    }

    public static synchronized void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("Smart Water Distribution Server stopped.");
        }
    }

    public static WaterNetwork getNetwork() {
        return network;
    }
}
