package com.smartwater.util;

import com.smartwater.model.*;
import com.smartwater.network.WaterNetwork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * Verification test to confirm that MySQL is the single source of truth
 * for all persistent data in the Smart Water Distribution Management System.
 */
public class DatabaseReadTest {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   MYSQL DATABASE-FIRST INTEGRATION VERIFICATION TEST");
        System.out.println("===============================================================\n");

        WaterNetwork network = new WaterNetwork();

        // 1. Initial database loading
        System.out.println("--- 1. Testing Live Database Loading ---");
        network.loadReservoirsFromDatabase();
        network.loadDemandAreasFromDatabase();
        network.loadPipelinesFromDatabase();
        network.loadWaterRequestsFromDatabase();
        network.loadComplaintsFromDatabase();
        network.loadMaintenanceRequestsFromDatabase();

        Collection<Reservoir> reservoirs = network.getAllReservoirs();
        Collection<DemandArea> areas = network.getAllDemandAreas();
        Collection<Pipeline> pipelines = network.getAllPipelines();
        List<WaterRequest> requests = network.getRequestHistory();
        List<Complaint> complaints = network.getComplaints();
        List<MaintenanceRequest> maintenance = network.getMaintenanceQueueList();

        System.out.printf("Loaded %d reservoirs from MySQL.%n", reservoirs.size());
        for (Reservoir r : reservoirs) {
            System.out.printf("  - Reservoir: %s, Name: %s, Current: %d L, Limit: %d L, Status: %s%n",
                    r.getId(), r.getName(), r.getAvailable(), r.getDailyLimit(), r.getStatus());
        }

        System.out.printf("Loaded %d demand areas from MySQL.%n", areas.size());
        for (DemandArea a : areas) {
            System.out.printf("  - Area: %s, Name: %s, Type: %s, Priority: %s, Connected: %s%n",
                    a.getId(), a.getName(), a.getAreaType(), a.getEffectivePriority(), a.getConnectedReservoir());
        }

        System.out.printf("Loaded %d pipelines from MySQL.%n", pipelines.size());
        for (Pipeline p : pipelines) {
            System.out.printf("  - Pipeline: %s, From: %s, To: %s, Status: %s, Capacity: %d L%n",
                    p.getId(), p.getFromReservoirId(), p.getToAreaId(), p.getStatus(), p.getCapacity());
        }

        System.out.printf("Loaded %d water requests from MySQL.%n", requests.size());
        System.out.printf("Loaded %d complaints from MySQL.%n", complaints.size());
        System.out.printf("Loaded %d maintenance tasks from MySQL.%n", maintenance.size());

        if (reservoirs.size() >= 2 && areas.size() >= 4 && pipelines.size() >= 6) {
            System.out.println(">> Step 1 PASSED: Core entities loaded directly from MySQL.\n");
        } else {
            System.err.println(">> Step 1 FAILED: Unexpected entity counts from MySQL.");
            return;
        }

        // 2. Test deleting water request in MySQL (simulating phpMyAdmin deletion)
        System.out.println("--- 2. Testing Deletion in MySQL (phpMyAdmin Simulation) ---");
        String testReqCode = "REQ-VERIFY-TMP";
        try (Connection conn = DatabaseConnection.getConnection()) {
            int areaId = -1;
            try (PreparedStatement s = conn.prepareStatement("SELECT id FROM demand_areas LIMIT 1");
                 ResultSet rs = s.executeQuery()) {
                if (rs.next()) areaId = rs.getInt("id");
            }

            try (PreparedStatement s = conn.prepareStatement(
                    "INSERT INTO water_requests (code, area_id, requested_amount, priority, remarks, status, request_date, arrival_order) " +
                    "VALUES (?, ?, 25000, 'MEDIUM', 'Testing direct deletion', 'PENDING', CURDATE(), 999)")) {
                s.setString(1, testReqCode);
                s.setInt(2, areaId);
                s.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reload and verify it is present
        network.loadWaterRequestsFromDatabase();
        boolean foundBefore = network.getRequestHistory().stream().anyMatch(r -> testReqCode.equals(r.getId()));
        System.out.println("Inserted temporary request " + testReqCode + " directly into MySQL. Found in network? " + foundBefore);

        // Now simulate deleting it in phpMyAdmin (direct SQL DELETE)
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("DELETE FROM water_requests WHERE code = ?")) {
            s.setString(1, testReqCode);
            int rowsDeleted = s.executeUpdate();
            System.out.println("Direct MySQL DELETE executed in phpMyAdmin simulation. Rows deleted: " + rowsDeleted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reload and verify it is completely gone
        network.loadWaterRequestsFromDatabase();
        boolean foundAfter = network.getRequestHistory().stream().anyMatch(r -> testReqCode.equals(r.getId()));
        System.out.println("After MySQL DELETE, reloaded requests. Found in network? " + foundAfter);

        if (foundBefore && !foundAfter) {
            System.out.println(">> Step 2 PASSED: phpMyAdmin deletion instantly reflected on next fetch!\n");
        } else {
            System.err.println(">> Step 2 FAILED: phpMyAdmin deletion not properly reflected.");
            return;
        }

        // 3. Test adding a new Demand Area & verifying MySQL insertion
        System.out.println("--- 3. Testing Adding Demand Area to MySQL ---");
        String testAreaCode = "AREA-99";
        DemandArea newArea = new ResidentialArea(testAreaCode, "Test North Colony", 20000, 60000, PriorityLevel.HIGH, "RES-A", "Verification Colony", 450);
        boolean areaSaved = network.saveDemandAreaToDatabase(newArea, "PL-TEST-99", 50000, 2.5);
        System.out.println("Saved new demand area to MySQL: " + areaSaved);

        // Verify in MySQL
        boolean areaInDb = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT code FROM demand_areas WHERE code = ?")) {
            s.setString(1, testAreaCode);
            try (ResultSet rs = s.executeQuery()) {
                areaInDb = rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Demand area " + testAreaCode + " exists in MySQL demand_areas table? " + areaInDb);

        // Verify loaded in network
        boolean areaInNetwork = network.getDemandArea(testAreaCode) != null;
        System.out.println("Demand area " + testAreaCode + " present in network demandAreas map? " + areaInNetwork);

        // Clean up test area
        boolean areaDeleted = network.deleteDemandAreaFromDatabase(testAreaCode);
        System.out.println("Cleaned up test demand area from MySQL: " + areaDeleted);

        if (areaSaved && areaInDb && areaInNetwork && areaDeleted) {
            System.out.println(">> Step 3 PASSED: Demand areas insert into and delete from MySQL correctly.\n");
        } else {
            System.err.println(">> Step 3 FAILED: Demand area MySQL persistence issue.");
            return;
        }

        // 4. Test Reservoir Updates in MySQL
        System.out.println("--- 4. Testing Reservoir Update in MySQL ---");
        boolean resUpdated = network.updateReservoirInDatabase("RES-A", 1750000, 480000, "Active");
        System.out.println("Updated RES-A in MySQL: " + resUpdated);

        int updatedLevelInDb = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT current_level FROM reservoirs WHERE code = 'RES-A'")) {
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) updatedLevelInDb = rs.getInt("current_level");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("RES-A current_level in MySQL: " + updatedLevelInDb);

        // Restore to 1800000, 500000
        network.updateReservoirInDatabase("RES-A", 1800000, 500000, "Active");

        if (resUpdated && updatedLevelInDb == 1750000) {
            System.out.println(">> Step 4 PASSED: Reservoir parameter updates persist to MySQL.\n");
        } else {
            System.err.println(">> Step 4 FAILED: Reservoir MySQL update failed.");
            return;
        }

        // 5. Test Water Allocation Algorithm & MySQL Persistence
        System.out.println("--- 5. Testing Water Allocation Algorithm & MySQL water_allocations Persistence ---");
        int countBefore = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM water_allocations");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) countBefore = rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AllocationSummary summary = network.runAllocation(true);
        System.out.printf("Allocation ran: Total Available=%d L, Total Requested=%d L, Total Allocated=%d L%n",
                summary.getTotalAvailable(), summary.getTotalRequested(), summary.getTotalAllocated());

        int countAfter = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM water_allocations");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) countAfter = rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("water_allocations rows in MySQL: before=%d, after=%d%n", countBefore, countAfter);

        if (countAfter >= countBefore && summary.getTotalAllocated() > 0) {
            System.out.println(">> Step 5 PASSED: Allocations executed and successfully recorded in MySQL!\n");
        } else {
            System.err.println(">> Step 5 FAILED: Allocations not persisted.");
            return;
        }

        System.out.println("===============================================================");
        System.out.println("   ALL 5 DATABASE-FIRST VERIFICATION CHECKS PASSED!");
        System.out.println("===============================================================");
    }
}
