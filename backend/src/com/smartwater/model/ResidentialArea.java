package com.smartwater.model;

/**
 * ResidentialArea Class
 * Subclass of DemandArea representing residential colonies, societies, or apartments.
 */
public class ResidentialArea extends DemandArea {
    private int householdCount;

    public ResidentialArea() {
        super();
        this.priority = PriorityLevel.HIGH;
    }

    public ResidentialArea(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description) {
        super(id, name, minRequirement, maxRequirement, priority != null ? priority : PriorityLevel.HIGH, connectedReservoir, description);
        this.householdCount = 500;
    }

    public ResidentialArea(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description, int householdCount) {
        super(id, name, minRequirement, maxRequirement, priority, connectedReservoir, description);
        this.householdCount = householdCount;
    }

    @Override
    public String getAreaType() {
        return "Residential";
    }

    public int getHouseholdCount() {
        return householdCount;
    }

    public void setHouseholdCount(int householdCount) {
        this.householdCount = Math.max(0, householdCount);
    }
}
