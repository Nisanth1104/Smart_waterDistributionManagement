package com.smartwater.model;

/**
 * PriorityLevel Enum
 * Represents the 5-tier water allocation hierarchy.
 * Higher weight = higher priority in allocation.
 */
public enum PriorityLevel {
    CRITICAL(5, "Critical"),
    HIGH(4, "High"),
    MEDIUM(3, "Medium"),
    LOW(2, "Low"),
    VERY_LOW(1, "Very Low");

    private final int weight;
    private final String displayName;

    PriorityLevel(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PriorityLevel fromString(String text) {
        if (text == null) return MEDIUM;
        String clean = text.trim().replace(" ", "_").toUpperCase();
        for (PriorityLevel p : PriorityLevel.values()) {
            if (p.name().equalsIgnoreCase(clean) || p.displayName.equalsIgnoreCase(text.trim())) {
                return p;
            }
        }
        return MEDIUM;
    }
}
