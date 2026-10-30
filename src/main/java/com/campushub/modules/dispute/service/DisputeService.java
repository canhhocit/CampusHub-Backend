package com.campushub.modules.dispute.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.dispute.dto.DisputeCreateRequest;
import com.campushub.modules.dispute.dto.DisputeResolveRequest;
import com.campushub.modules.dispute.dto.DisputeResponse;
import com.campushub.modules.dispute.entity.OrderDispute;
import com.campushub.modules.dispute.enums.DisputeStatus;
import com.campushub.modules.dispute.repository.OrderDisputeRepository;
import com.campushub.modules.notification.service.NotificationService;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.modules.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    private final OrderDisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public DisputeService(OrderDisputeRepository disputeRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          WalletService walletService,
                          NotificationService notificationService) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    @Transactional
    public DisputeResponse createDispute(Long orderId, DisputeCreateRequest request, String buyerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.DISPUTE_NOT_ALLOWED, "Không thể khiếu nại đơn hàng đã bị hủy hoặc hết hạn");
        }

        if (disputeRepository.existsByOrderIdAndStatus(orderId, DisputeStatus.OPEN)) {
            throw new AppException(ErrorCode.DISPUTE_ALREADY_EXISTS);
        }

        OrderDispute dispute = new OrderDispute();
        dispute.setOrder(order);
        dispute.setCreator(order.getBuyer());
        dispute.setReason(request.getReason());
        dispute.setDescription(request.getDescription().trim());
        if (request.getProofImageUrl() != null && !request.getProofImageUrl().isBlank()) {
            dispute.setProofImageUrl(request.getProofImageUrl().trim());
        }
        dispute.setStatus(DisputeStatus.OPEN);

        dispute = disputeRepository.save(dispute);
        log.info("User {} created dispute #{} for order #{}", buyerUsername, dispute.getId(), order.getId());

        // Notify seller about the dispute
        notificationService.createNotification(
                order.getSeller(),
                "Đơn hàng bị khiếu nại",
                String.format("Người mua đã gửi khiếu nại cho đơn hàng %s. Lý do: %s. Đội ngũ quản trị viên sẽ xem xét bằng chứng.",
                        order.getOrderNumber(), request.getReason().getDescription()),
                "DISPUTE_OPENED",
                "ORDER",
                order.getId()
        );

        return mapToResponse(dispute);
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> getDisputesByOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getUsername().equals(username);
        boolean isSeller = order.getSeller().getUsername().equals(username);
        User currentUser = userRepository.findByUsername(username).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == com.campushub.common.enums.Role.ADMIN;

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        return disputeRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> getAdminDisputes(DisputeStatus status, Pageable pageable) {
        if (status != null) {
            return disputeRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::mapToResponse);
        }
        return disputeRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OrderDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new AppException(ErrorCode.DISPUTE_NOT_FOUND));

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new AppException(ErrorCode.DISPUTE_ALREADY_RESOLVED);
        }

        Order order = dispute.getOrder();
        DisputeStatus resolution = request.getResolution();
        dispute.setStatus(resolution);
        dispute.setAdminNote(request.getAdminNote() != null ? request.getAdminNote().trim() : null);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(LocalDateTime.now());

        if (resolution == DisputeStatus.RESOLVED_REFUND_BUYER) {
            // Refund buyer
            walletService.refundOrderPayment(order, "Hoàn tiền do khiếu nại được chấp thuận: " + dispute.getReason().getDescription());

            notificationService.createNotification(
                    order.getBuyer(),
                    "Khiếu nại được chấp thuận",
                    String.format("Khiếu nại cho đơn hàng %s đã được duyệt. Tiền đã được hoàn về ví của bạn.", order.getOrderNumber()),
                    "DISPUTE_RESOLVED",
                    "ORDER",
                    order.getId()
            );

            notificationService.createNotification(
                    order.getSeller(),
                    "Kết quả giải quyết khiếu nại",
                    String.format("Khiếu nại cho đơn hàng %s đã được giải quyết: Hoàn tiền cho người mua.", order.getOrderNumber()),
                    "DISPUTE_RESOLVED",
                    "ORDER",
                    order.getId()
            );
        } else if (resolution == DisputeStatus.RESOLVED_PAYOUT_SELLER) {
            notificationService.createNotification(
                    order.getBuyer(),
                    "Kết quả giải quyết khiếu nại",
                    String.format("Khiếu nại cho đơn hàng %s đã được từ chối sau khi xem xét bằng chứng.", order.getOrderNumber()),
                    "DISPUTE_REJECTED",
                    "ORDER",
                    order.getId()
            );

            notificationService.createNotification(
                    order.getSeller(),
                    "Khiếu nại đã được giải quyết",
                    String.format("Khiếu nại cho đơn hàng %s của bạn đã được bác bỏ. Giao dịch vẫn an toàn.", order.getOrderNumber()),
                    "DISPUTE_RESOLVED",
                    "ORDER",
                    order.getId()
            );
        }

        dispute = disputeRepository.save(dispute);
        log.info("Admin {} resolved dispute #{} with status {}", adminUsername, dispute.getId(), resolution);

        return mapToResponse(dispute);
    }

    private DisputeResponse mapToResponse(OrderDispute d) {
        DisputeResponse res = new DisputeResponse();
        res.setId(d.getId());
        res.setOrderId(d.getOrder().getId());
        res.setOrderNumber(d.getOrder().getOrderNumber());
        if (d.getOrder().getProduct() != null) {
            res.setProductId(d.getOrder().getProduct().getId());
            res.setProductTitle(d.getOrder().getProduct().getTitle());
        }
        res.setTotalAmount(d.getOrder().getTotalAmount());
        res.setBuyerId(d.getCreator().getId());
        res.setBuyerUsername(d.getCreator().getUsername());
        res.setBuyerFullName(d.getCreator().getFullName());
        res.setSellerId(d.getOrder().getSeller().getId());
        res.setSellerUsername(d.getOrder().getSeller().getUsername());
        res.setSellerFullName(d.getOrder().getSeller().getFullName());
        res.setReason(d.getReason());
        res.setReasonDescription(d.getReason().getDescription());
        res.setDescription(d.getDescription());
        res.setProofImageUrl(d.getProofImageUrl());
        res.setStatus(d.getStatus());
        res.setStatusDescription(d.getStatus().getDescription());
        res.setAdminNote(d.getAdminNote());
        if (d.getResolvedBy() != null) {
            res.setResolvedByUsername(d.getResolvedBy().getUsername());
        }
        res.setResolvedAt(d.getResolvedAt());
        res.setCreatedAt(d.getCreatedAt());
        return res;
    }
}

