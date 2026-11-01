package com.smartwater.model;

/**
 * Reservoir Class
 * Encapsulates reservoir storage parameters and enforces volume bounds:
 * 0 <= available <= capacity.
 */
public class Reservoir {
    private String id;
    private String name;
    private int capacity;       // Total Capacity in Litres
    private int available;      // Currently Available Water in Litres
    private int dailyLimit;     // Daily Supply Limit in Litres
    private String status;      // "Active" or "Inactive"
    private String location;

    public Reservoir() {
    }

    public Reservoir(String id, String name, int capacity, int available, int dailyLimit, String status, String location) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.dailyLimit = dailyLimit;
        this.status = (status != null && status.equalsIgnoreCase("Inactive")) ? "Inactive" : "Active";
        this.location = location;
        setAvailable(available); // Enforces 0 <= available <= capacity
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Reservoir capacity cannot be negative.");
        }
        this.capacity = capacity;
        if (this.available > capacity) {
            this.available = capacity;
        }
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        if (available < 0) {
            throw new IllegalArgumentException("Available water cannot be negative: " + available);
        }
        if (this.capacity > 0 && available > this.capacity) {
            throw new IllegalArgumentException("Available water cannot exceed reservoir capacity (" + this.capacity + " L).");
        }
        this.available = available;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        if (dailyLimit < 0) {
            throw new IllegalArgumentException("Daily supply limit cannot be negative.");
        }
        this.dailyLimit = dailyLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status != null && (status.equalsIgnoreCase("Inactive") || status.equalsIgnoreCase("Active"))) {
            this.status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        } else {
            this.status = "Active";
        }
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(this.status);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Deduct allocated water from reservoir with strict availability check
     * @param amount Litres to deduct
     * @return true if successfully deducted, false if insufficient water
     */
    public synchronized boolean deductWater(int amount) {
        if (amount < 0 || !isActive()) {
            return false;
        }
        if (this.available >= amount) {
            this.available -= amount;
            return true;
        }
        return false;
    }

    /**
     * Replenish reservoir with fresh water up to maximum capacity
     */
    public synchronized void replenishWater(int amount) {
        if (amount <= 0) return;
        this.available = Math.min(this.capacity, this.available + amount);
    }

    @Override
    public String toString() {
        return "Reservoir{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", capacity=" + capacity +
                ", available=" + available +
                ", status='" + status + '\'' +
                '}';
    }
}
