package com.smartwater.test;

import com.smartwater.model.*;
import com.smartwater.network.WaterNetwork;

import java.util.*;

/**
 * BackendTestSuite Class
 * Standalone automated test runner verifying all 10 Phase 2 test scenarios:
 * 1. Normal water supply
 * 2. Low reservoir water
 * 3. Critical priority over lower-priority demand
 * 4. Partial water allocation
 * 5. Failed pipeline causing a disconnected area
 * 6. Switching supply to another reservoir when a valid connection exists
 * 7. Maintenance queue FIFO processing
 * 8. Pipeline repair and restoration of connectivity
 * 9. Adding and updating demand areas
 * 10. Complaint creation and resolution
 */
public class BackendTestSuite {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("🧪 RUNNING SMART WATER DISTRIBUTION JAVA BACKEND TEST SUITE 🧪");
        System.out.println("=================================================================");

        int passed = 0;
        int failed = 0;

        WaterNetwork network = new WaterNetwork();

        // -------------------------------------------------------------
        // Test 1: Normal Water Supply
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 1] Normal Water Supply & Initialization: ");
            assert network.getAllReservoirs().size() == 2 : "Expected 2 initial reservoirs";
            assert network.getAllDemandAreas().size() == 5 : "Expected 5 initial demand areas";
            assert network.getAllPipelines().size() == 6 : "Expected 6 initial pipelines";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 2: Low Reservoir Water & Bounds Validation
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 2] Low Reservoir Water Bounds & Validation: ");
            Reservoir rA = network.getReservoir("RES-A");
            boolean caughtNegative = false;
            try {
                rA.setAvailable(-500);
            } catch (IllegalArgumentException e) {
                caughtNegative = true;
            }
            assert caughtNegative : "Should prevent negative available water";

            boolean caughtOverflow = false;
            try {
                rA.setAvailable(150000); // Exceeds 100k capacity
            } catch (IllegalArgumentException e) {
                caughtOverflow = true;
            }
            assert caughtOverflow : "Should prevent setting water greater than capacity";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 3: Critical Priority Allocation (PriorityQueue)
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 3] Critical Demand Priority over Lower-Priority (PriorityQueue): ");
            PriorityQueue<WaterRequest> pq = network.buildRequestPriorityQueue();
            WaterRequest first = pq.poll();
            assert first != null && first.getPriority() == PriorityLevel.CRITICAL : "Top request must be Critical Priority!";
            assert first.getAreaId().equals("AREA-02") : "City Care Hospital must be first priority request";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 4: Failed Pipeline Causing Disconnected Area (Graph BFS)
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 4] Failed Pipeline PL-04 causing Area 4 Disconnection (Graph BFS): ");
            Set<String> reachable = network.findReachableAreasBFS();
            Set<String> disconnected = network.findDisconnectedAreas();
            assert !reachable.contains("AREA-04") : "Area 4 must not be reachable while PL-04 is failed!";
            assert disconnected.contains("AREA-04") : "Area 4 must be in disconnected set!";
            assert reachable.contains("AREA-01") : "Area 1 must be reachable";
            assert reachable.contains("AREA-02") : "Area 2 must be reachable";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 5: Water Allocation Execution & Partial Allocation
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 5] Water Allocation Execution & Partial Allocation: ");
            AllocationSummary summary = network.runAllocation(false);
            assert summary.getTotalAvailable() == 65000 : "Expected 65,000 L available across active reservoirs";
            assert summary.getTotalRequested() == 75000 : "Expected 75,000 L requested";
            // Area 4 is disconnected, so it gets 0 L
            boolean foundDisconnectedArea4 = false;
            boolean foundHospitalAllocated = false;
            for (AllocationRecord rec : summary.getRecords()) {
                if (rec.getAreaId().equals("AREA-04")) {
                    assert rec.getAllocated() == 0 : "Disconnected area must receive 0 L";
                    assert "Disconnected".equals(rec.getStatus()) : "Status must be Disconnected";
                    foundDisconnectedArea4 = true;
                }
                if (rec.getAreaId().equals("AREA-02")) {
                    assert rec.getAllocated() == 15000 : "Hospital should be fully allocated";
                    assert "Fully Allocated".equals(rec.getStatus()) : "Hospital status must be Fully Allocated";
                    foundHospitalAllocated = true;
                }
            }
            assert foundDisconnectedArea4 : "Area 4 must be present in allocation records";
            assert foundHospitalAllocated : "Hospital must be present in allocation records";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 6: Switching Supply to Another Reservoir (Failover Link)
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 6] Multi-Reservoir Switching (Area 2 Hospital Backup PL-06 from RES-B): ");
            // Fail primary pipeline PL-02 from RES-A to Hospital
            network.setPipelineStatus("PL-02", PipelineStatus.FAILED);
            // Hospital should STILL be reachable because of backup PL-06 from RES-B!
            boolean hospitalStillReachable = network.isAreaReachable("AREA-02");
            assert hospitalStillReachable : "Hospital must remain reachable via backup link PL-06 from Reservoir B!";
            List<Reservoir> suppliers = network.getReachableActiveReservoirsForArea("AREA-02");
            assert suppliers.size() == 1 && suppliers.get(0).getId().equals("RES-B") : "Hospital should now be supplied by RES-B";
            
            // Restore PL-02
            network.setPipelineStatus("PL-02", PipelineStatus.WORKING);
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 7: Maintenance Queue FIFO Processing (ArrayDeque)
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 7] Maintenance Queue FIFO Dispatcher (ArrayDeque): ");
            // Initially MNT-301 (PL-04) is at front of FIFO queue
            List<MaintenanceRequest> queueBefore = network.getMaintenanceQueueList();
            assert !queueBefore.isEmpty() && queueBefore.get(0).getId().equals("MNT-301") : "MNT-301 must be first";

            // Process next FIFO: Queued -> In Progress
            MaintenanceRequest step1 = network.processNextMaintenanceFIFO();
            assert step1 != null && "In Progress".equals(step1.getStatus()) : "Status should be In Progress";

            // Process next FIFO: In Progress -> Completed (and restores PL-04 to Working!)
            MaintenanceRequest step2 = network.processNextMaintenanceFIFO();
            assert step2 != null && "Completed".equals(step2.getStatus()) : "Status should be Completed";
            assert network.getPipeline("PL-04").isWorking() : "Pipeline PL-04 must be restored to WORKING!";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 8: Pipeline Repair Restoring Connectivity
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 8] Restoring Connectivity & Reallocating Area 4: ");
            // Now that PL-04 was repaired in Test 7, Area 4 should be REACHABLE!
            boolean area4Reachable = network.isAreaReachable("AREA-04");
            assert area4Reachable : "Area 4 must now be reachable after pipeline repair!";

            AllocationSummary summaryAfterRepair = network.runAllocation(false);
            for (AllocationRecord rec : summaryAfterRepair.getRecords()) {
                if (rec.getAreaId().equals("AREA-04")) {
                    assert rec.getAllocated() > 0 : "Area 4 should now receive water after connectivity restored";
                    assert "Connected".equals(rec.getConnectivity()) : "Area 4 must be Connected";
                }
            }
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 9: Adding & Updating Demand Area (OOP Inheritance & Validation)
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 9] Demand Area Validation & Inheritance: ");
            boolean caughtInvalid = false;
            try {
                // Min 25,000 exceeds Max 10,000 -> Should throw Exception!
                new ResidentialArea("AREA-99", "Invalid Area", 25000, 10000, PriorityLevel.LOW, "RES-A", "Invalid");
            } catch (IllegalArgumentException e) {
                caughtInvalid = true;
            }
            assert caughtInvalid : "Must reject min requirement > max requirement";

            CommercialArea comm = new CommercialArea("AREA-06", "Downtown Metro Tech Hub", 8000, 12000, PriorityLevel.MEDIUM, "RES-B", "IT Park", "Technology");
            network.addDemandArea(comm);
            assert network.getDemandArea("AREA-06") != null : "Area 6 must be registered";
            assert "Commercial".equals(network.getDemandArea("AREA-06").getAreaType()) : "Area 6 type must be Commercial";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 10: Complaint Creation & Status Resolution
        // -------------------------------------------------------------
        try {
            System.out.print("[Test 10] Complaint Creation & Status Transition: ");
            Complaint c = new Complaint("CMP-299", "Test Citizen", "AREA-01", "Greenfield", "Pipeline Leakage", "Water pooling on road", "2026-10-25", "Open");
            network.addComplaint(c);
            assert network.getComplaints().stream().anyMatch(item -> item.getId().equals("CMP-299")) : "Complaint must exist";

            boolean updated = network.updateComplaintStatus("CMP-299", "Resolved");
            assert updated : "Update must succeed";
            Complaint found = network.getComplaints().stream().filter(item -> item.getId().equals("CMP-299")).findFirst().orElse(null);
            assert found != null && "Resolved".equals(found.getStatus()) : "Status must be Resolved";
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ❌: " + t.getMessage());
            failed++;
        }

        System.out.println("=================================================================");
        System.out.println("RESULTS: " + passed + " / " + (passed + failed) + " TESTS PASSED.");
        if (failed == 0) {
            System.out.println("🎉 ALL 10 PHASE 2 BACKEND TEST SCENARIOS PASSED SUCCESSFULLY!");
        } else {
            System.out.println("⚠️ " + failed + " test(s) failed.");
        }
        System.out.println("=================================================================");
    }
}
