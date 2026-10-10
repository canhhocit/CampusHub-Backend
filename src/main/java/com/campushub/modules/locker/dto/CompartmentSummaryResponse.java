package com.campushub.modules.locker.dto;

public class CompartmentSummaryResponse {
    private long total;
    private long available;
    private long reserved;
    private long occupied;
    private long error;
    private long outOfService;

    public CompartmentSummaryResponse() {}

    public CompartmentSummaryResponse(long total, long available, long reserved, long occupied, long error, long outOfService) {
        this.total = total;
        this.available = available;
        this.reserved = reserved;
        this.occupied = occupied;
        this.error = error;
        this.outOfService = outOfService;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getAvailable() {
        return available;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    public long getReserved() {
        return reserved;
    }

    public void setReserved(long reserved) {
        this.reserved = reserved;
    }

    public long getOccupied() {
        return occupied;
    }

    public void setOccupied(long occupied) {
        this.occupied = occupied;
    }

    public long getError() {
        return error;
    }

    public void setError(long error) {
        this.error = error;
    }

    public long getOutOfService() {
        return outOfService;
    }

    public void setOutOfService(long outOfService) {
        this.outOfService = outOfService;
    }
}
