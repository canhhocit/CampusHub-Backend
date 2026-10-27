package com.campushub.modules.order.entity;

import com.campushub.common.model.BaseEntity;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_SELLER;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType = OrderType.PURCHASE;

    @Column(name = "rental_duration_days")
    private Integer rentalDurationDays;

    @Column(name = "due_return_at")
    private LocalDateTime dueReturnAt;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "seller_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellerPayout = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payos_order_code", unique = true)
    private Long payosOrderCode;

    @Column(name = "payos_payment_link_id", length = 100)
    private String payosPaymentLinkId;

    @Column(name = "payos_transaction_reference", length = 100)
    private String payosTransactionReference;

    @Column(name = "payos_checkout_url", columnDefinition = "TEXT")
    private String payosCheckoutUrl;

    @Column(name = "payos_qr_code", columnDefinition = "TEXT")
    private String payosQrCode;

    @Column(name = "payment_paid_at")
    private LocalDateTime paymentPaidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id")
    private Locker locker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compartment_id")
    private Compartment compartment;

    @Column(name = "pickup_code_hash")
    private String pickupCodeHash;

    @Column(name = "encrypted_pickup_code")
    private String encryptedPickupCode;

    @Column(name = "pickup_deadline")
    private LocalDateTime pickupDeadline;

    @Column(name = "pickup_attempts", nullable = false)
    private Integer pickupAttempts = 0;

    @Column(name = "deposit_at")
    private LocalDateTime depositAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_compartment_id")
    private Compartment returnCompartment;

    @Column(name = "return_pickup_code_hash")
    private String returnPickupCodeHash;

    @Column(name = "encrypted_return_pickup_code")
    private String encryptedReturnPickupCode;

    @Column(name = "return_deposit_at")
    private LocalDateTime returnDepositAt;

    @Column(name = "return_picked_up_at")
    private LocalDateTime returnPickedUpAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public User getBuyer() {
        return buyer;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
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

    public Locker getLocker() {
        return locker;
    }

    public void setLocker(Locker locker) {
        this.locker = locker;
    }

    public Compartment getCompartment() {
        return compartment;
    }

    public void setCompartment(Compartment compartment) {
        this.compartment = compartment;
    }

    public String getPickupCodeHash() {
        return pickupCodeHash;
    }

    public void setPickupCodeHash(String pickupCodeHash) {
        this.pickupCodeHash = pickupCodeHash;
    }

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public void setPickupDeadline(LocalDateTime pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
    }

    public Integer getPickupAttempts() {
        return pickupAttempts;
    }

    public void setPickupAttempts(Integer pickupAttempts) {
        this.pickupAttempts = pickupAttempts;
    }

    public LocalDateTime getDepositAt() {
        return depositAt;
    }

    public void setDepositAt(LocalDateTime depositAt) {
        this.depositAt = depositAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getEncryptedPickupCode() {
        return encryptedPickupCode;
    }

    public void setEncryptedPickupCode(String encryptedPickupCode) {
        this.encryptedPickupCode = encryptedPickupCode;
    }

    public LocalDateTime getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public Compartment getReturnCompartment() {
        return returnCompartment;
    }

    public void setReturnCompartment(Compartment returnCompartment) {
        this.returnCompartment = returnCompartment;
    }

    public String getReturnPickupCodeHash() {
        return returnPickupCodeHash;
    }

    public void setReturnPickupCodeHash(String returnPickupCodeHash) {
        this.returnPickupCodeHash = returnPickupCodeHash;
    }

    public String getEncryptedReturnPickupCode() {
        return encryptedReturnPickupCode;
    }

    public void setEncryptedReturnPickupCode(String encryptedReturnPickupCode) {
        this.encryptedReturnPickupCode = encryptedReturnPickupCode;
    }

    public LocalDateTime getReturnDepositAt() {
        return returnDepositAt;
    }

    public void setReturnDepositAt(LocalDateTime returnDepositAt) {
        this.returnDepositAt = returnDepositAt;
    }

    public LocalDateTime getReturnPickedUpAt() {
        return returnPickedUpAt;
    }

    public void setReturnPickedUpAt(LocalDateTime returnPickedUpAt) {
        this.returnPickedUpAt = returnPickedUpAt;
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

    public String getPayosPaymentLinkId() {
        return payosPaymentLinkId;
    }

    public void setPayosPaymentLinkId(String payosPaymentLinkId) {
        this.payosPaymentLinkId = payosPaymentLinkId;
    }

    public String getPayosTransactionReference() {
        return payosTransactionReference;
    }

    public void setPayosTransactionReference(String payosTransactionReference) {
        this.payosTransactionReference = payosTransactionReference;
    }

    public String getPayosCheckoutUrl() {
        return payosCheckoutUrl;
    }

    public void setPayosCheckoutUrl(String payosCheckoutUrl) {
        this.payosCheckoutUrl = payosCheckoutUrl;
    }

    public String getPayosQrCode() {
        return payosQrCode;
    }

    public void setPayosQrCode(String payosQrCode) {
        this.payosQrCode = payosQrCode;
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

    public BigDecimal getTotalAmount() {
        return (priceSnapshot != null ? priceSnapshot : BigDecimal.ZERO)
                .add(depositAmount != null ? depositAmount : BigDecimal.ZERO);
    }
}
