package com.campushub.modules.locker.entity;

import com.campushub.common.model.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lockers")
public class Locker extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LockerStatus status = LockerStatus.ACTIVE;

    @OneToMany(mappedBy = "locker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Compartment> compartments = new ArrayList<>();

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

    public List<Compartment> getCompartments() {
        return compartments;
    }

    public void setCompartments(List<Compartment> compartments) {
        this.compartments = compartments;
    }
}
