package com.campushub.modules.locker.dto;

import com.campushub.modules.locker.entity.LockerStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class LockerResponse implements Serializable {
    private Long id;
    private String code;
    private String name;
    private String location;
    private LockerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long totalCompartments;
    private long availableCompartments;
    private long occupiedCompartments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LockerStatus getStatus() {
        return status;
    }

    public void setStatus(LockerStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getTotalCompartments() {
        return totalCompartments;
    }

    public void setTotalCompartments(long totalCompartments) {
        this.totalCompartments = totalCompartments;
    }

    public long getAvailableCompartments() {
        return availableCompartments;
    }

    public void setAvailableCompartments(long availableCompartments) {
        this.availableCompartments = availableCompartments;
    }

    public long getOccupiedCompartments() {
        return occupiedCompartments;
    }

    public void setOccupiedCompartments(long occupiedCompartments) {
        this.occupiedCompartments = occupiedCompartments;
    }
}
