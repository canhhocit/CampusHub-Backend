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
    CANNOT_CLEAR_NON_EXPIRED_ORDER(HttpStatus.BAD_REQUEST, "Cannot clear an order that is not expired"),
    AGE_RESTRICTION(HttpStatus.BAD_REQUEST, "Bạn phải từ đủ 13 tuổi trở lên để đăng ký tài khoản"),
    IDENTITY_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Bạn cần xác minh danh tính (CCCD) để thực hiện giao dịch này"),
    CITIZEN_ID_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Số CCCD này đã được sử dụng để xác minh một tài khoản khác"),
    INVALID_CITIZEN_ID(HttpStatus.BAD_REQUEST, "Số CCCD không hợp lệ (phải gồm đúng 12 chữ số)"),
    ALREADY_IDENTITY_VERIFIED(HttpStatus.BAD_REQUEST, "Tài khoản của bạn đã được xác minh danh tính"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "Vui lòng chọn file ảnh để tải lên"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận file định dạng hình ảnh (JPG, PNG, WEBP, GIF)"),
    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải file lên hệ thống lưu trữ. Vui lòng thử lại sau."),
    MAX_IMAGES_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng ảnh mô tả vượt quá giới hạn tối đa cho phép của hệ thống"),
    VIDEO_UPLOAD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Tính năng đăng video sản phẩm hiện đang tạm khóa bởi quản trị viên"),
    MAX_VIDEOS_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng video vượt quá giới hạn cho phép của hệ thống"),
    INVALID_VIDEO_TYPE(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận file video định dạng MP4, WEBM hoặc MOV"),
    VIDEO_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "Dung lượng video vượt quá giới hạn tối đa cho phép"),
    ORDER_ALREADY_PAID(HttpStatus.BAD_REQUEST, "Đơn hàng này đã được thanh toán"),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái thanh toán không hợp lệ"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "Giao dịch thanh toán không thành công"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "Số tiền thanh toán không hợp lệ (tối thiểu 1,000 VNĐ)"),
    PAYMENT_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "Chữ ký webhook PayOS không hợp lệ"),
    SIMULATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Thao tác mô phỏng thanh toán bị vô hiệu hóa trong môi trường này"),
    IDENTITY_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phiên xác minh danh tính"),
    IDENTITY_ATTEMPT_EXPIRED(HttpStatus.BAD_REQUEST, "Phiên xác minh danh tính đã hết hạn"),
    IDENTITY_ATTEMPT_INVALID_STATE(HttpStatus.BAD_REQUEST, "Trạng thái phiên xác minh không hợp lệ cho thao tác này"),
    IDENTITY_OCR_FAILED(HttpStatus.BAD_REQUEST, "Không thể trích xuất thông tin hợp lệ từ ảnh thẻ CCCD"),
    IDENTITY_LIVENESS_FAILED(HttpStatus.BAD_REQUEST, "Xác minh chuyển động khuôn mặt hoặc chống giả mạo (PAD) không đạt"),
    IDENTITY_FACE_MISMATCH(HttpStatus.BAD_REQUEST, "Khuôn mặt selfie không trùng khớp với ảnh trên thẻ CCCD"),
    IDENTITY_CARD_MISMATCH(HttpStatus.BAD_REQUEST, "Ảnh thẻ CCCD cung cấp không trùng khớp với ảnh đã trích xuất ban đầu"),
    IDENTITY_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã vượt quá số lần thử xác minh trong ngày. Vui lòng thử lại sau."),
    IDENTITY_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng hoàn thành luồng xác minh eKYC trực tuyến (OCR, Liveness, Face Match)."),
    IDENTITY_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ nhận diện sinh trắc học hiện không khả dụng. Vui lòng thử lại sau."),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ví của người dùng"),
    INSUFFICIENT_WALLET_BALANCE(HttpStatus.BAD_REQUEST, "Số dư ví không đủ để thực hiện giao dịch"),
    INVALID_WITHDRAWAL_AMOUNT(HttpStatus.BAD_REQUEST, "Số tiền rút không hợp lệ (tối thiểu 10,000 VNĐ)"),
    BANK_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tài khoản ngân hàng"),
    PAYOUT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"),
    PAYOUT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "Yêu cầu rút tiền này đã được xử lý trước đó");

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
