package com.campushub.modules.locker.entity;

import com.campushub.common.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "compartments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_locker_compartment_code", columnNames = {"locker_id", "code"})
})
public class Compartment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "locker_id", nullable = false)
    private Locker locker;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompartmentStatus status = CompartmentStatus.AVAILABLE;

    public Locker getLocker() {
        return locker;
    }

    public void setLocker(Locker locker) {
        this.locker = locker;
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
}
