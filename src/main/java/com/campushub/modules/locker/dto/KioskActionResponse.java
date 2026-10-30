package com.campushub.modules.locker.dto;

public class KioskActionResponse {
    private boolean success;
    private String actionType; // "BUYER_PICKUP", "RETURN_PICKUP", "SELLER_DEPOSIT", "RETURN_DEPOSIT"
    private Long lockerId;
    private String lockerCode;
    private Long compartmentId;
    private String compartmentCode;
    private String orderNumber;
    private String productTitle;
    private String message;

    public KioskActionResponse() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

