package com.smartwater.model;

/**
 * DemandArea Abstract Base Class
 * Demonstrates:
 * - Abstraction: Abstract methods implemented by specific area types
 * - Encapsulation: Protected/private state with range validation (min <= max)
 * - Polymorphism: Subclasses override getAreaType() and getEffectivePriority()
 */
public abstract class DemandArea {
    protected String id;
    protected String name;
    protected int minRequirement;       // Minimum baseline Litres
    protected int maxRequirement;       // Maximum peak Litres
    protected PriorityLevel priority;   // Allocation Priority
    protected String connectedReservoir;// Primary connected reservoir ID
    protected String backupReservoir;   // Secondary backup reservoir ID (if applicable)
    protected String description;

    public DemandArea() {
    }

    public DemandArea(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description) {
        this.id = id;
        this.name = name;
        this.priority = (priority != null) ? priority : PriorityLevel.MEDIUM;
        this.connectedReservoir = connectedReservoir;
        this.description = description;
        setRequirements(minRequirement, maxRequirement);
    }

    /**
     * Polymorphic method to get the specific type label
     */
    public abstract String getAreaType();

    /**
     * Polymorphic priority evaluator: Critical institutions always enforce CRITICAL priority
     */
    public PriorityLevel getEffectivePriority() {
        return this.priority;
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

    public int getMinRequirement() {
        return minRequirement;
    }

    public int getMaxRequirement() {
        return maxRequirement;
    }

    public void setRequirements(int min, int max) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("Water requirements cannot be negative.");
        }
        if (min > max) {
            throw new IllegalArgumentException("Validation Error: Minimum requirement (" + min + " L) cannot exceed Maximum requirement (" + max + " L).");
        }
        this.minRequirement = min;
        this.maxRequirement = max;
    }

    public void setMinRequirement(int minRequirement) {
        setRequirements(minRequirement, this.maxRequirement);
    }

    public void setMaxRequirement(int maxRequirement) {
        setRequirements(this.minRequirement, maxRequirement);
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = (priority != null) ? priority : PriorityLevel.MEDIUM;
    }

    public String getConnectedReservoir() {
        return connectedReservoir;
    }

    public void setConnectedReservoir(String connectedReservoir) {
        this.connectedReservoir = connectedReservoir;
    }

    public String getBackupReservoir() {
        return backupReservoir;
    }

    public void setBackupReservoir(String backupReservoir) {
        this.backupReservoir = backupReservoir;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "DemandArea{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + getAreaType() + '\'' +
                ", priority=" + getEffectivePriority() +
                ", range=" + minRequirement + "-" + maxRequirement + " L" +
                '}';
    }
}
