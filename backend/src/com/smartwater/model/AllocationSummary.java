package com.smartwater.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AllocationSummary Class
 * Aggregates high-level metrics across all demand areas after running an allocation cycle.
 */
public class AllocationSummary {
    private int totalAvailable;
    private int totalRequested;
    private int totalAllocated;
    private int remainingWater;
    private int unmetDemand;
    private List<AllocationRecord> records;
    private String alertMessage;
    private String alertType; // "success", "warning", "danger"

    public AllocationSummary() {
        this.records = new ArrayList<>();
    }

    public AllocationSummary(int totalAvailable, int totalRequested, int totalAllocated, int remainingWater, int unmetDemand) {
        this.totalAvailable = totalAvailable;
        this.totalRequested = totalRequested;
        this.totalAllocated = totalAllocated;
        this.remainingWater = remainingWater;
        this.unmetDemand = unmetDemand;
        this.records = new ArrayList<>();
    }

    public int getTotalAvailable() {
        return totalAvailable;
    }

    public void setTotalAvailable(int totalAvailable) {
        this.totalAvailable = totalAvailable;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(int totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public int getRemainingWater() {
        return remainingWater;
    }

    public void setRemainingWater(int remainingWater) {
        this.remainingWater = remainingWater;
    }

    public int getUnmetDemand() {
        return unmetDemand;
    }

    public void setUnmetDemand(int unmetDemand) {
        this.unmetDemand = unmetDemand;
    }

    public List<AllocationRecord> getRecords() {
        return records;
    }

    public void setRecords(List<AllocationRecord> records) {
        this.records = records;
    }

    public void addRecord(AllocationRecord record) {
        this.records.add(record);
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
}
