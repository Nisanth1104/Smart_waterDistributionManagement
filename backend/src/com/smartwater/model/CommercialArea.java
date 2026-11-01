package com.smartwater.model;

/**
 * CommercialArea Class
 * Subclass of DemandArea representing tech parks, markets, or commercial zones.
 */
public class CommercialArea extends DemandArea {
    private String businessCategory;

    public CommercialArea() {
        super();
        this.priority = PriorityLevel.MEDIUM;
    }

    public CommercialArea(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description) {
        super(id, name, minRequirement, maxRequirement, priority != null ? priority : PriorityLevel.MEDIUM, connectedReservoir, description);
        this.businessCategory = "General Commercial";
    }

    public CommercialArea(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description, String businessCategory) {
        super(id, name, minRequirement, maxRequirement, priority, connectedReservoir, description);
        this.businessCategory = businessCategory;
    }

    @Override
    public String getAreaType() {
        return "Commercial";
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }
}
