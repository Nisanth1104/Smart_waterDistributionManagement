package com.smartwater.model;

/**
 * Complaint Class
 * Represents citizen or institutional grievances regarding water supply disruptions or pipe damage.
 */
public class Complaint {
    private String id;
    private String userName;
    private String areaId;
    private String areaName;
    private String type;         // "Water Not Supplied", "Low Water Supply", "Pipeline Leakage", "Pipeline Damage", "Other"
    private String description;
    private String date;
    private String status;       // "Open", "In Progress", "Resolved"

    public Complaint() {
        this.status = "Open";
    }

    public Complaint(String id, String userName, String areaId, String areaName, String type, String description, String date, String status) {
        this.id = id;
        this.userName = userName;
        this.areaId = areaId;
        this.areaName = areaName;
        this.type = type;
        this.description = description;
        this.date = date;
        this.status = (status != null) ? status : "Open";
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id='" + id + '\'' +
                ", user='" + userName + '\'' +
                ", area='" + areaName + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
