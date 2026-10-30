package com.campushub.modules.dispute.entity;

import com.campushub.common.model.BaseEntity;
import com.campushub.modules.dispute.enums.DisputeReason;
import com.campushub.modules.dispute.enums.DisputeStatus;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_disputes")
public class OrderDispute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DisputeReason reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "proof_image_url", length = 500)
    private String proofImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public OrderDispute() {}

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public DisputeReason getReason() {
        return reason;
    }

    public void setReason(DisputeReason reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

