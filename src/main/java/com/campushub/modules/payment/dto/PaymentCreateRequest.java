package com.campushub.modules.payment.dto;

import com.campushub.modules.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PaymentCreateRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
