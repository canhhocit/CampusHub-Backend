package com.campushub.modules.order.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAttemptService {

    private final OrderRepository orderRepository;

    public OrderAttemptService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Called only within pickup's locked transaction. Never suspend the transaction
     * holding this row's lock to update the same row on another connection.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int recordFailedPickupAttempt(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        int currentAttempts = order.getPickupAttempts() != null ? order.getPickupAttempts() : 0;
        int newAttempts = currentAttempts + 1;
        order.setPickupAttempts(newAttempts);
        orderRepository.saveAndFlush(order);
        return newAttempts;
    }
}
