package com.smartwater.policy;

import com.smartwater.model.*;
import com.smartwater.network.WaterNetwork;

import java.util.*;

/**
 * PriorityAllocationPolicy Class
 * Implements WaterAllocationPolicy using Java's PriorityQueue.
 * Enforces the 9 allocation requirements:
 * 1. Critical areas receive highest priority (5-tier: Critical > High > Medium > Low > Very Low)
 * 2. Deterministic request arrival order (FIFO) for equal priorities
 * 3. Network reachability via active reservoirs and working pipelines (BFS graph traversal)
 * 4. Multi-reservoir failover / backup switching when secondary pipeline exists
 * 5. Partial allocation support when water is scarce
 * 6. Disconnected area detection when feeder pipelines fail
 */
public class PriorityAllocationPolicy implements WaterAllocationPolicy {

    @Override
    public AllocationSummary allocateWater(WaterNetwork network, boolean commitDeductions) {
        // 1. Gather all active reservoirs and track their available working volume
        Map<String, Integer> workingReservoirBalance = new HashMap<>();
        int totalSystemAvailable = 0;

        for (Reservoir r : network.getAllReservoirs()) {
            if (r.isActive()) {
                workingReservoirBalance.put(r.getId(), r.getAvailable());
                totalSystemAvailable += r.getAvailable();
            }
        }

        // 2. Build PriorityQueue of WaterRequests
        // WaterRequest.compareTo enforces: PriorityLevel weight DESC, then arrivalOrder ASC
        PriorityQueue<WaterRequest> pq = network.buildRequestPriorityQueue();

        // Map to aggregate requests by demand area (so each area has one consolidated allocation outcome)
        Map<String, AggregatedAreaDemand> aggregatedDemands = new LinkedHashMap<>();

        // Also ensure all configured demand areas are included even if they didn't submit a manual request today
        for (DemandArea area : network.getAllDemandAreas()) {
            aggregatedDemands.put(area.getId(), new AggregatedAreaDemand(area.getId(), area.getName(), area.getEffectivePriority(), area.getMaxRequirement(), 999999));
        }

        // Pull from PriorityQueue to update demands and priorities
        while (!pq.isEmpty()) {
            WaterRequest req = pq.poll();
            AggregatedAreaDemand entry = aggregatedDemands.get(req.getAreaId());
            if (entry != null) {
                // If demand request is present, use requested quantity
                entry.totalRequested = req.getQuantity();
                // Retain the highest priority if multiple requests exist
                if (req.getPriority().getWeight() > entry.priority.getWeight()) {
                    entry.priority = req.getPriority();
                }
                entry.arrivalOrder = Math.min(entry.arrivalOrder, req.getArrivalOrder());
            }
        }

        // 3. Sort demand entries strictly by PriorityQueue Comparator logic
        List<AggregatedAreaDemand> sortedDemands = new ArrayList<>(aggregatedDemands.values());
        sortedDemands.sort((a, b) -> {
            int pDiff = b.priority.getWeight() - a.priority.getWeight();
            if (pDiff != 0) return pDiff;
            return Long.compare(a.arrivalOrder, b.arrivalOrder);
        });

        int totalRequested = 0;
        int totalAllocated = 0;
        int disconnectedCount = 0;
        int unmetDemandTotal = 0;
        List<AllocationRecord> records = new ArrayList<>();

        // Map to track how much water each reservoir supplied during this allocation run
        Map<String, Integer> reservoirDeductionMap = new HashMap<>();

        // 4. Run Allocation across sorted demand list
        for (AggregatedAreaDemand demand : sortedDemands) {
            totalRequested += demand.totalRequested;

            // Check graph connectivity: which active reservoirs have a working pipeline to this area?
            List<Reservoir> supplyingReservoirs = network.getReachableActiveReservoirsForArea(demand.areaId);

            boolean isConnected = !supplyingReservoirs.isEmpty();
            int allocated = 0;
            String status = "Not Allocated";
            List<String> supplyingResNames = new ArrayList<>();

            if (!isConnected) {
                // Disconnected due to pipeline failure or inactive reservoirs
                allocated = 0;
                status = "Disconnected";
                disconnectedCount++;
            } else {
                int needed = demand.totalRequested;

                // Allocate water from connected active reservoirs (with multi-reservoir failover support!)
                for (Reservoir res : supplyingReservoirs) {
                    int availableInRes = workingReservoirBalance.getOrDefault(res.getId(), 0);
                    if (availableInRes > 0 && needed > 0) {
                        int canGive = Math.min(needed, availableInRes);
                        allocated += canGive;
                        workingReservoirBalance.put(res.getId(), availableInRes - canGive);
                        reservoirDeductionMap.put(res.getId(), reservoirDeductionMap.getOrDefault(res.getId(), 0) + canGive);
                        needed -= canGive;
                        supplyingResNames.add(res.getId() + " (" + canGive + " L)");
                    }
                    if (needed == 0) break;
                }

                // Determine allocation state
                if (allocated == demand.totalRequested) {
                    status = "Fully Allocated";
                } else if (allocated > 0) {
                    status = "Partially Allocated";
                } else {
                    status = "Not Allocated";
                }
            }

            int unmet = Math.max(0, demand.totalRequested - allocated);
            totalAllocated += allocated;
            unmetDemandTotal += unmet;

            String suppliedByStr = supplyingResNames.isEmpty() ? "None" : String.join(", ", supplyingResNames);

            records.add(new AllocationRecord(
                    demand.areaId,
                    demand.areaName,
                    demand.priority.getDisplayName(),
                    demand.totalRequested,
                    allocated,
                    unmet,
                    isConnected ? "Connected" : "Disconnected",
                    status,
                    suppliedByStr
            ));
        }

        int remainingWater = Math.max(0, totalSystemAvailable - totalAllocated);

        // 5. Build AllocationSummary
        AllocationSummary summary = new AllocationSummary(totalSystemAvailable, totalRequested, totalAllocated, remainingWater, unmetDemandTotal);
        summary.setRecords(records);

        // Alert banners
        if (totalRequested > totalSystemAvailable) {
            summary.setAlertMessage("⚠️ Water Deficit Notice: Requested demand (" + totalRequested + " L) exceeds available active storage (" + totalSystemAvailable + " L). Critical & High priority institutions served first. Total unmet shortfall: " + unmetDemandTotal + " L.");
            summary.setAlertType("warning");
        } else if (disconnectedCount > 0) {
            summary.setAlertMessage("🚫 Network Failure Alert: " + disconnectedCount + " demand area(s) are DISCONNECTED due to pipeline faults. Repaired in the FIFO Maintenance Queue to restore supply.");
            summary.setAlertType("danger");
        } else {
            summary.setAlertMessage("✅ Optimal Grid Supply: Grid demand balanced across active reservoirs according to priority hierarchy.");
            summary.setAlertType("success");
        }

        // 6. Commit deductions to live Reservoir instances if requested
        if (commitDeductions) {
            for (Map.Entry<String, Integer> entry : reservoirDeductionMap.entrySet()) {
                Reservoir r = network.getReservoir(entry.getKey());
                if (r != null) {
                    r.deductWater(entry.getValue());
                }
            }

            // Sync request statuses in history
            for (WaterRequest req : network.getRequestHistory()) {
                for (AllocationRecord rec : records) {
                    if (rec.getAreaId().equals(req.getAreaId())) {
                        if ("Fully Allocated".equals(rec.getStatus())) {
                            req.setStatus("Allocated");
                        } else if ("Partially Allocated".equals(rec.getStatus())) {
                            req.setStatus("Partially Allocated");
                        } else {
                            req.setStatus("Pending");
                        }
                    }
                }
            }
        }

        return summary;
    }

    /**
     * Internal helper class to aggregate multiple demands for an area
     */
    private static class AggregatedAreaDemand {
        String areaId;
        String areaName;
        PriorityLevel priority;
        int totalRequested;
        long arrivalOrder;

        AggregatedAreaDemand(String areaId, String areaName, PriorityLevel priority, int totalRequested, long arrivalOrder) {
            this.areaId = areaId;
            this.areaName = areaName;
            this.priority = priority;
            this.totalRequested = totalRequested;
            this.arrivalOrder = arrivalOrder;
        }
    }
}
