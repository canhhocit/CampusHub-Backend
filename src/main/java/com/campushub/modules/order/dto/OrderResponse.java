package com.campushub.modules.order.dto;

import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.OrderType;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.entity.PaymentMethod;

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
    private OrderType orderType = OrderType.PURCHASE;
    private Integer rentalDurationDays;
    private LocalDateTime dueReturnAt;
    private BigDecimal depositAmount;
    private BigDecimal platformFee;
    private BigDecimal sellerPayout;
    private BigDecimal totalAmount;

    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Long payosOrderCode;
    private LocalDateTime paymentPaidAt;
    private LocalDateTime refundedAt;
    private String checkoutUrl;
    private String qrCode;

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

    private Long returnLockerId;
    private String returnLockerName;
    private Long returnCompartmentId;
    private String returnCompartmentCode;

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

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public Integer getRentalDurationDays() {
        return rentalDurationDays;
    }

    public void setRentalDurationDays(Integer rentalDurationDays) {
        this.rentalDurationDays = rentalDurationDays;
    }

    public LocalDateTime getDueReturnAt() {
        return dueReturnAt;
    }

    public void setDueReturnAt(LocalDateTime dueReturnAt) {
        this.dueReturnAt = dueReturnAt;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getSellerPayout() {
        return sellerPayout;
    }

    public void setSellerPayout(BigDecimal sellerPayout) {
        this.sellerPayout = sellerPayout;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getReturnLockerId() {
        return returnLockerId;
    }

    public void setReturnLockerId(Long returnLockerId) {
        this.returnLockerId = returnLockerId;
    }

    public String getReturnLockerName() {
        return returnLockerName;
    }

    public void setReturnLockerName(String returnLockerName) {
        this.returnLockerName = returnLockerName;
    }

    public Long getReturnCompartmentId() {
        return returnCompartmentId;
    }

    public void setReturnCompartmentId(Long returnCompartmentId) {
        this.returnCompartmentId = returnCompartmentId;
    }

    public String getReturnCompartmentCode() {
        return returnCompartmentCode;
    }

    public void setReturnCompartmentCode(String returnCompartmentCode) {
        this.returnCompartmentCode = returnCompartmentCode;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getPayosOrderCode() {
        return payosOrderCode;
    }

    public void setPayosOrderCode(Long payosOrderCode) {
        this.payosOrderCode = payosOrderCode;
    }

    public LocalDateTime getPaymentPaidAt() {
        return paymentPaidAt;
    }

    public void setPaymentPaidAt(LocalDateTime paymentPaidAt) {
        this.paymentPaidAt = paymentPaidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
