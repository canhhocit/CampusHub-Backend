package com.campushub.modules.locker.dto;

import com.campushub.modules.locker.entity.CompartmentStatus;
import java.time.LocalDateTime;

public class CompartmentResponse {
    private Long id;
    private Long lockerId;
    private String code;
    private CompartmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CompartmentStatus getStatus() {
        return status;
    }

    public void setStatus(CompartmentStatus status) {
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
}
