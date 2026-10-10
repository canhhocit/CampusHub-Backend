package com.campushub.modules.locker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LockerUpdateRequest {

    @NotBlank(message = "Locker name is required")
    @Size(max = 100, message = "Locker name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Locker location is required")
    @Size(max = 255, message = "Locker location cannot exceed 255 characters")
    private String location;

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
}
