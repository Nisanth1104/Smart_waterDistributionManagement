package com.smartwater.model;

/**
 * CriticalInstitution Class
 * Subclass of DemandArea representing emergency healthcare facilities, hospitals, or emergency centers.
 * Demonstrates:
 * - Inheritance: Extends DemandArea
 * - Polymorphism: Enforces CRITICAL priority by default
 */
public class CriticalInstitution extends DemandArea {
    private String emergencyContact;
    private boolean hasIcuFacility;

    public CriticalInstitution() {
        super();
        this.priority = PriorityLevel.CRITICAL;
    }

    public CriticalInstitution(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description) {
        super(id, name, minRequirement, maxRequirement, PriorityLevel.CRITICAL, connectedReservoir, description);
        this.hasIcuFacility = true;
    }

    public CriticalInstitution(String id, String name, int minRequirement, int maxRequirement, PriorityLevel priority, String connectedReservoir, String description, String emergencyContact, boolean hasIcuFacility) {
        super(id, name, minRequirement, maxRequirement, PriorityLevel.CRITICAL, connectedReservoir, description);
        this.emergencyContact = emergencyContact;
        this.hasIcuFacility = hasIcuFacility;
    }

    @Override
    public String getAreaType() {
        return "Critical Institution";
    }

    @Override
    public PriorityLevel getEffectivePriority() {
        // Critical institutions are always evaluated at highest priority
        return PriorityLevel.CRITICAL;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public boolean isHasIcuFacility() {
        return hasIcuFacility;
    }

    public void setHasIcuFacility(boolean hasIcuFacility) {
        this.hasIcuFacility = hasIcuFacility;
    }
}
