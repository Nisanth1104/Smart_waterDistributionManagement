package com.smartwater.model;

/**
 * MaintenanceRequest Class
 * Managed in an ArrayDeque to demonstrate a strict FIFO (First-In, First-Out) repair dispatch queue.
 */
public class MaintenanceRequest {
    private String id;
    private String pipelineId;
    private String affectedArea;
    private String problemDescription;
    private String priority;        // "Critical", "High", "Medium", "Low"
    private String dateAdded;
    private String status;          // "Queued", "In Progress", "Completed"

    public MaintenanceRequest() {
        this.status = "Queued";
    }

    public MaintenanceRequest(String id, String pipelineId, String affectedArea, String problemDescription, String priority, String dateAdded, String status) {
        this.id = id;
        this.pipelineId = pipelineId;
        this.affectedArea = affectedArea;
        this.problemDescription = problemDescription;
        this.priority = priority;
        this.dateAdded = dateAdded;
        this.status = (status != null) ? status : "Queued";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }

    public String getAffectedArea() {
        return affectedArea;
    }

    public void setAffectedArea(String affectedArea) {
        this.affectedArea = affectedArea;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MaintenanceRequest{" +
                "id='" + id + '\'' +
                ", pipelineId='" + pipelineId + '\'' +
                ", affectedArea='" + affectedArea + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
