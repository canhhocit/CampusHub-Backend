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
    INVALID_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "Invalid product status transition");

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
