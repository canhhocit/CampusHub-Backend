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
     * Record a failed pickup attempt in an independent, committed transaction.
     * This ensures that when an invalid PIN exception rolls back the caller transaction,
     * the incremented attempt count is retained and committed in the database immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
