package com.campushub.modules.order.dto;

import com.campushub.modules.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long productId;
    private String productTitle;
    private String productImageUrl;
    private BigDecimal priceSnapshot;
    private OrderStatus status;

    private Long buyerId;
    private String buyerUsername;
    private String buyerFullName;

    private Long sellerId;
    private String sellerUsername;
    private String sellerFullName;

    private Long lockerId;
    private String lockerCode;
    private String lockerName;
    private String lockerLocation;

    private Long compartmentId;
    private String compartmentCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public void setBuyerUsername(String buyerUsername) {
        this.buyerUsername = buyerUsername;
    }

    public String getBuyerFullName() {
        return buyerFullName;
    }

    public void setBuyerFullName(String buyerFullName) {
        this.buyerFullName = buyerFullName;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public String getSellerFullName() {
        return sellerFullName;
    }

    public void setSellerFullName(String sellerFullName) {
        this.sellerFullName = sellerFullName;
    }

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }

    public String getLockerCode() {
        return lockerCode;
    }

    public void setLockerCode(String lockerCode) {
        this.lockerCode = lockerCode;
    }

    public String getLockerName() {
        return lockerName;
    }

    public void setLockerName(String lockerName) {
        this.lockerName = lockerName;
    }

    public String getLockerLocation() {
        return lockerLocation;
    }

    public void setLockerLocation(String lockerLocation) {
        this.lockerLocation = lockerLocation;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }

    public void setCompartmentId(Long compartmentId) {
        this.compartmentId = compartmentId;
    }

    public String getCompartmentCode() {
        return compartmentCode;
    }

    public void setCompartmentCode(String compartmentCode) {
        this.compartmentCode = compartmentCode;
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
