package com.campushub.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Username or email already exists"),
    USER_PENDING(HttpStatus.FORBIDDEN, "Account is pending verification"),
    USER_LOCKED(HttpStatus.FORBIDDEN, "Account is locked"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Token reuse detected. All sessions revoked."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    UNAUTHORIZED_ACTION(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "The resource was updated by another transaction. Please refresh and try again."),
    INVALID_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "Invalid product status transition"),
    LOCKER_NOT_FOUND(HttpStatus.NOT_FOUND, "Locker not found"),
    LOCKER_CODE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Locker code already exists"),
    COMPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Compartment not found"),
    COMPARTMENT_CODE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Compartment code already exists in this locker"),
    INVALID_LOCKER_STATUS(HttpStatus.BAD_REQUEST, "Invalid locker status transition"),
    INVALID_COMPARTMENT_STATUS(HttpStatus.BAD_REQUEST, "Invalid compartment status transition"),
    UNSAFE_COMPARTMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Unsafe compartment status transition: cannot manually free occupied or reserved compartments"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    CANNOT_ORDER_OWN_PRODUCT(HttpStatus.BAD_REQUEST, "Cannot order your own product"),
    PRODUCT_NOT_AVAILABLE_FOR_ORDER(HttpStatus.BAD_REQUEST, "Product is not available for order"),
    ACTIVE_ORDER_EXISTS(HttpStatus.CONFLICT, "An active order already exists for this product"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Invalid order status transition"),
    NO_AVAILABLE_COMPARTMENT(HttpStatus.BAD_REQUEST, "No available compartment in the selected locker"),
    INVALID_PICKUP_CODE(HttpStatus.BAD_REQUEST, "Invalid pickup code"),
    PICKUP_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "Pickup code has expired"),
    PICKUP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Exceeded maximum pickup attempts. Please contact support."),
    LOCKER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "Locker is currently not active"),
    MOCK_IOT_DEVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Locker hardware error occurred"),
    ORDER_EXPIRED(HttpStatus.BAD_REQUEST, "Order has expired and can no longer be picked up"),
    CANNOT_CLEAR_NON_EXPIRED_ORDER(HttpStatus.BAD_REQUEST, "Cannot clear an order that is not expired");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
