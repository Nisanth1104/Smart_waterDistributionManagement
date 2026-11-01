package com.smartwater.policy;

import com.smartwater.model.AllocationSummary;
import com.smartwater.network.WaterNetwork;

/**
 * WaterAllocationPolicy Interface
 * Demonstrates Abstraction:
 * Decouples the allocation strategy from the underlying network data structure.
 * Different policies (Priority, Fair-Share, Emergency-Only) can be plugged in polymorphically.
 */
public interface WaterAllocationPolicy {
    /**
     * Compute and execute water allocation across the network.
     * @param network The WaterNetwork containing reservoirs, pipelines, and demand areas
     * @param commitDeductions If true, deducts allocated volume from reservoir available supplies
     * @return AllocationSummary detailing allocations per area and overall grid statistics
     */
    AllocationSummary allocateWater(WaterNetwork network, boolean commitDeductions);
}
