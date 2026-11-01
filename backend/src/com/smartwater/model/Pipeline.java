package com.smartwater.model;

/**
 * Pipeline Class
 * Represents a physical water transmission conduit between a Reservoir and a Demand Area.
 */
public class Pipeline {
    private String id;
    private String name;
    private String fromReservoirId;
    private String toAreaId;
    private int capacity;               // Maximum flow rate in Litres
    private PipelineStatus status;      // WORKING, FAILED, UNDER_MAINTENANCE
    private double lengthKm;

    public Pipeline() {
        this.status = PipelineStatus.WORKING;
    }

    public Pipeline(String id, String name, String fromReservoirId, String toAreaId, int capacity, PipelineStatus status, double lengthKm) {
        this.id = id;
        this.name = name;
        this.fromReservoirId = fromReservoirId;
        this.toAreaId = toAreaId;
        this.capacity = capacity;
        this.status = (status != null) ? status : PipelineStatus.WORKING;
        this.lengthKm = lengthKm;
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

    public String getFromReservoirId() {
        return fromReservoirId;
    }

    public void setFromReservoirId(String fromReservoirId) {
        this.fromReservoirId = fromReservoirId;
    }

    public String getToAreaId() {
        return toAreaId;
    }

    public void setToAreaId(String toAreaId) {
        this.toAreaId = toAreaId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = (status != null) ? status : PipelineStatus.WORKING;
    }

    public double getLengthKm() {
        return lengthKm;
    }

    public void setLengthKm(double lengthKm) {
        this.lengthKm = Math.max(0.0, lengthKm);
    }

    public boolean isWorking() {
        return this.status == PipelineStatus.WORKING;
    }

    public boolean isFailed() {
        return this.status == PipelineStatus.FAILED;
    }

    public boolean isUnderMaintenance() {
        return this.status == PipelineStatus.UNDER_MAINTENANCE;
    }

    @Override
    public String toString() {
        return "Pipeline{" +
                "id='" + id + '\'' +
                ", from='" + fromReservoirId + '\'' +
                ", to='" + toAreaId + '\'' +
                ", status=" + status +
                '}';
    }
}
