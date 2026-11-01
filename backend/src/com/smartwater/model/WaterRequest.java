package com.smartwater.model;

/**
 * WaterRequest Class
 * Implements Comparable for PriorityQueue insertion.
 * Priority Rule:
 * 1. Critical > High > Medium > Low > Very Low (descending priority weight)
 * 2. Deterministic arrival order (FIFO) for equal priorities
 */
public class WaterRequest implements Comparable<WaterRequest> {
    private String id;
    private String userName;
    private String areaId;
    private String areaName;
    private int quantity;               // Requested Litres
    private PriorityLevel priority;     // CRITICAL, HIGH, MEDIUM, LOW, VERY_LOW
    private String notes;
    private String status;              // "Pending", "Partially Allocated", "Allocated"
    private String date;
    private long arrivalOrder;          // Sequence counter for deterministic FIFO tie-breaking

    public WaterRequest() {
        this.status = "Pending";
    }

    public WaterRequest(String id, String userName, String areaId, String areaName, int quantity, PriorityLevel priority, String notes, String date, long arrivalOrder) {
        this.id = id;
        this.userName = userName;
        this.areaId = areaId;
        this.areaName = areaName;
        this.quantity = Math.max(0, quantity);
        this.priority = (priority != null) ? priority : PriorityLevel.MEDIUM;
        this.notes = notes;
        this.date = date;
        this.arrivalOrder = arrivalOrder;
        this.status = "Pending";
    }

    /**
     * PriorityQueue comparator logic:
     * - Primary: Higher PriorityLevel weight wins
     * - Secondary: Smaller arrivalOrder (earlier arrival) wins
     */
    @Override
    public int compareTo(WaterRequest other) {
        if (other == null) return -1;
        int priorityDiff = other.priority.getWeight() - this.priority.getWeight();
        if (priorityDiff != 0) {
            return priorityDiff;
        }
        return Long.compare(this.arrivalOrder, other.arrivalOrder);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = (priority != null) ? priority : PriorityLevel.MEDIUM;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getArrivalOrder() {
        return arrivalOrder;
    }

    public void setArrivalOrder(long arrivalOrder) {
        this.arrivalOrder = arrivalOrder;
    }

    @Override
    public String toString() {
        return "WaterRequest{" +
                "id='" + id + '\'' +
                ", area='" + areaName + '\'' +
                ", quantity=" + quantity +
                ", priority=" + priority +
                ", arrival=" + arrivalOrder +
                ", status='" + status + '\'' +
                '}';
    }
}
