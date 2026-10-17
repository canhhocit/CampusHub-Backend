package com.campushub.modules.order.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;

/** Expected rejection whose attempt/expiry changes must commit under the order lock. */
public class PickupValidationException extends AppException {
    public PickupValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
