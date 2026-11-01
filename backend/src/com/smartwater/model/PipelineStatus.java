package com.smartwater.model;

/**
 * PipelineStatus Enum
 * Tracks the physical health and flow state of pipelines in the distribution network.
 */
public enum PipelineStatus {
    WORKING("Working"),
    FAILED("Failed"),
    UNDER_MAINTENANCE("Under Maintenance");

    private final String displayName;

    PipelineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PipelineStatus fromString(String text) {
        if (text == null) return WORKING;
        String clean = text.trim().replace(" ", "_").toUpperCase();
        for (PipelineStatus s : PipelineStatus.values()) {
            if (s.name().equalsIgnoreCase(clean) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return WORKING;
    }
}
