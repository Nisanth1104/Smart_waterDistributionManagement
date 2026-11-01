package com.smartwater.model;

/**
 * AllocationRecord Class
 * Stores the specific allocation outcome for an individual demand area.
 */
public class AllocationRecord {
    private String areaId;
    private String areaName;
    private String priority;
    private int requested;
    private int allocated;
    private int unmet;
    private String connectivity;    // "Connected" or "Disconnected"
    private String status;          // "Fully Allocated", "Partially Allocated", "Not Allocated", "Disconnected"
    private String suppliedBy;      // Reservoir(s) supplying water

    public AllocationRecord() {
    }

    public AllocationRecord(String areaId, String areaName, String priority, int requested, int allocated, int unmet, String connectivity, String status, String suppliedBy) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.priority = priority;
        this.requested = requested;
        this.allocated = allocated;
        this.unmet = unmet;
        this.connectivity = connectivity;
        this.status = status;
        this.suppliedBy = suppliedBy;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getRequested() {
        return requested;
    }

    public void setRequested(int requested) {
        this.requested = requested;
    }

    public int getAllocated() {
        return allocated;
    }

    public void setAllocated(int allocated) {
        this.allocated = allocated;
    }

    public int getUnmet() {
        return unmet;
    }

    public void setUnmet(int unmet) {
        this.unmet = unmet;
    }

    public String getConnectivity() {
        return connectivity;
    }

    public void setConnectivity(String connectivity) {
        this.connectivity = connectivity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSuppliedBy() {
        return suppliedBy;
    }

    public void setSuppliedBy(String suppliedBy) {
        this.suppliedBy = suppliedBy;
    }

    @Override
    public String toString() {
        return "AllocationRecord{" +
                "area='" + areaName + '\'' +
                ", req=" + requested +
                ", alloc=" + allocated +
                ", unmet=" + unmet +
                ", status='" + status + '\'' +
                '}';
    }
}
