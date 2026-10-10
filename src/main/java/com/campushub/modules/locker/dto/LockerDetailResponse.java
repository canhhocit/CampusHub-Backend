package com.campushub.modules.locker.dto;

import com.campushub.modules.locker.entity.LockerStatus;
import java.time.LocalDateTime;
import java.util.List;

public class LockerDetailResponse {
    private Long id;
    private String code;
    private String name;
    private String location;
    private LockerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CompartmentSummaryResponse compartmentSummary;
    private List<CompartmentResponse> compartments;

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

    public CompartmentSummaryResponse getCompartmentSummary() {
        return compartmentSummary;
    }

    public void setCompartmentSummary(CompartmentSummaryResponse compartmentSummary) {
        this.compartmentSummary = compartmentSummary;
    }

    public List<CompartmentResponse> getCompartments() {
        return compartments;
    }

    public void setCompartments(List<CompartmentResponse> compartments) {
        this.compartments = compartments;
    }
}
