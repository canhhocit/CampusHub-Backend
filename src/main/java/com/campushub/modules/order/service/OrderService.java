package com.campushub.modules.order.service;

import com.campushub.common.email.EmailService;
import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.dto.*;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;
    private final MockLockerService mockLockerService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Transient in-memory store for active pickup codes (cleared on pickup/cancel)
    // NEVER stored long-term in DB (only BCrypt hash is stored in DB)
    private final Map<Long, String> activePickupCodes = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        LockerRepository lockerRepository,
                        CompartmentRepository compartmentRepository,
                        MockLockerService mockLockerService,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
        this.mockLockerService = mockLockerService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setProductId(order.getProduct().getId());
        res.setProductTitle(order.getProduct().getTitle());
        res.setProductImageUrl(order.getProduct().getImageUrl());
        res.setPriceSnapshot(order.getPriceSnapshot());
        res.setStatus(order.getStatus());

        res.setBuyerId(order.getBuyer().getId());
        res.setBuyerUsername(order.getBuyer().getUsername());
        res.setBuyerFullName(order.getBuyer().getFullName());

        res.setSellerId(order.getSeller().getId());
        res.setSellerUsername(order.getSeller().getUsername());
        res.setSellerFullName(order.getSeller().getFullName());

        if (order.getLocker() != null) {
            res.setLockerId(order.getLocker().getId());
            res.setLockerCode(order.getLocker().getCode());
            res.setLockerName(order.getLocker().getName());
            res.setLockerLocation(order.getLocker().getLocation());
        }

        if (order.getCompartment() != null) {
            res.setCompartmentId(order.getCompartment().getId());
            res.setCompartmentCode(order.getCompartment().getCode());
        }

        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());
        return res;
    }

    private OrderDetailResponse mapToOrderDetailResponse(Order order, String username) {
        OrderDetailResponse res = new OrderDetailResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setProductId(order.getProduct().getId());
        res.setProductTitle(order.getProduct().getTitle());
        res.setProductImageUrl(order.getProduct().getImageUrl());
        res.setPriceSnapshot(order.getPriceSnapshot());
        res.setStatus(order.getStatus());

        res.setBuyerId(order.getBuyer().getId());
        res.setBuyerUsername(order.getBuyer().getUsername());
        res.setBuyerFullName(order.getBuyer().getFullName());

        res.setSellerId(order.getSeller().getId());
        res.setSellerUsername(order.getSeller().getUsername());
        res.setSellerFullName(order.getSeller().getFullName());

        if (order.getLocker() != null) {
            res.setLockerId(order.getLocker().getId());
            res.setLockerCode(order.getLocker().getCode());
            res.setLockerName(order.getLocker().getName());
            res.setLockerLocation(order.getLocker().getLocation());
        }

        if (order.getCompartment() != null) {
            res.setCompartmentId(order.getCompartment().getId());
            res.setCompartmentCode(order.getCompartment().getCode());
        }

        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());

        res.setPickupDeadline(order.getPickupDeadline());
        res.setPickupAttempts(order.getPickupAttempts());
        res.setDepositAt(order.getDepositAt());
        res.setPickedUpAt(order.getPickedUpAt());
        res.setCancelledAt(order.getCancelledAt());
        res.setRejectionReason(order.getRejectionReason());
        res.setVersion(order.getVersion());

        // Security check: ONLY the buyer can see the pickup code on their active order
        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP && order.getBuyer().getUsername().equals(username)) {
            res.setPickupCode(activePickupCodes.get(order.getId()));
        }

        return res;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String buyerUsername) {
        User buyer = userRepository.findByUsername(buyerUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE_FOR_ORDER);
        }

        if (product.getOwner().getId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.CANNOT_ORDER_OWN_PRODUCT);
        }

        Set<OrderStatus> activeStatuses = Set.of(
                OrderStatus.PENDING_SELLER,
                OrderStatus.AWAITING_DEPOSIT,
                OrderStatus.READY_FOR_PICKUP
        );
        if (orderRepository.existsByProductIdAndStatusIn(product.getId(), activeStatuses)) {
            throw new AppException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }

        Order order = new Order();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + uniqueSuffix);
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(product.getOwner());
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getBuyingOrders(String buyerUsername, Pageable pageable) {
        User buyer = userRepository.findByUsername(buyerUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findByBuyerId(buyer.getId(), pageable)
                .map(this::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellingOrders(String sellerUsername, Pageable pageable) {
        User seller = userRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findBySellerId(seller.getId(), pageable)
                .map(this::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long id, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getUsername().equals(username);
        boolean isSeller = order.getSeller().getUsername().equals(username);

        if (!isBuyer && !isSeller) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            if (!"ROLE_ADMIN".equals(user.getRole().name()) && !"ADMIN".equals(user.getRole().name())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
            }
        }

        return mapToOrderDetailResponse(order, username);
    }

    @Transactional
    public OrderResponse acceptOrder(Long orderId, OrderAcceptRequest request, String sellerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // Allocate compartment in an ACTIVE locker
        Compartment compartment;
        if (request != null && request.getLockerId() != null) {
            Locker locker = lockerRepository.findById(request.getLockerId())
                    .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));
            if (locker.getStatus() != LockerStatus.ACTIVE) {
                throw new AppException(ErrorCode.LOCKER_NOT_ACTIVE);
            }
            compartment = compartmentRepository.findFirstByLockerIdAndStatus(locker.getId(), CompartmentStatus.AVAILABLE)
                    .orElseThrow(() -> new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT));
        } else {
            List<Compartment> availableList = compartmentRepository.findAvailableInActiveLockers();
            if (availableList.isEmpty()) {
                throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
            }
            compartment = availableList.get(0);
        }

        // Reserve compartment
        compartment.setStatus(CompartmentStatus.RESERVED);
        compartmentRepository.save(compartment);

        order.setLocker(compartment.getLocker());
        order.setCompartment(compartment);
        order.setStatus(OrderStatus.AWAITING_DEPOSIT);

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(Long orderId, OrderRejectRequest request, String sellerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.REJECTED);
        if (request != null && request.getReason() != null) {
            order.setRejectionReason(request.getReason().trim());
        }

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String buyerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse depositOrder(Long orderId, String sellerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Locker locker = order.getLocker();
        Compartment compartment = order.getCompartment();
        if (locker == null || compartment == null) {
            throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
        }

        // Simulate IoT operations
        mockLockerService.openForDeposit(locker, compartment);
        mockLockerService.confirmItemDeposited(locker, compartment);

        // Transition compartment to OCCUPIED
        compartment.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.save(compartment);

        // Generate 6-digit cryptographically secure pickup code
        int codeInt = secureRandom.nextInt(1_000_000);
        String rawPickupCode = String.format("%06d", codeInt);

        // Hash pickup code for database persistence
        order.setPickupCodeHash(passwordEncoder.encode(rawPickupCode));
        order.setPickupDeadline(LocalDateTime.now().plusHours(48));
        order.setDepositAt(LocalDateTime.now());
        order.setPickupAttempts(0);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);

        order = orderRepository.save(order);

        // Keep raw code in transient cache for buyer
        activePickupCodes.put(order.getId(), rawPickupCode);

        // Attempt asynchronous email dispatch (swallowed safely if mail server offline)
        try {
            String subject = "CampusHub - Thông báo nhận hàng: Đơn " + order.getOrderNumber();
            String text = String.format(
                    "Xin chào %s,\n\nNgười bán đã gửi đơn hàng '%s' vào tủ đồ.\n" +
                    "Địa điểm: %s (%s)\n" +
                    "Ngăn tủ: %s\n" +
                    "Mã nhận hàng của bạn là: %s\n" +
                    "Hạn lấy hàng: %s\n\n" +
                    "Vui lòng đến đúng tủ đồ và nhập mã để nhận hàng.",
                    order.getBuyer().getFullName(),
                    order.getProduct().getTitle(),
                    locker.getName(),
                    locker.getLocation(),
                    compartment.getCode(),
                    rawPickupCode,
                    order.getPickupDeadline()
            );
            emailService.sendEmail(order.getBuyer().getEmail(), subject, text);
        } catch (Exception e) {
            log.warn("Could not dispatch email notification to buyer: {}", e.getMessage());
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse pickupOrder(Long orderId, OrderPickupRequest request, String buyerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (order.getPickupDeadline() != null && LocalDateTime.now().isAfter(order.getPickupDeadline())) {
            throw new AppException(ErrorCode.PICKUP_CODE_EXPIRED);
        }

        if (order.getPickupAttempts() >= 5) {
            throw new AppException(ErrorCode.PICKUP_ATTEMPTS_EXCEEDED);
        }

        if (!passwordEncoder.matches(request.getPickupCode(), order.getPickupCodeHash())) {
            order.setPickupAttempts(order.getPickupAttempts() + 1);
            orderRepository.save(order);
            throw new AppException(ErrorCode.INVALID_PICKUP_CODE);
        }

        Locker locker = order.getLocker();
        Compartment compartment = order.getCompartment();

        // Simulate IoT operations
        mockLockerService.openForPickup(locker, compartment);
        mockLockerService.confirmItemPickedUp(locker, compartment);

        // Complete order
        order.setStatus(OrderStatus.COMPLETED);
        order.setPickedUpAt(LocalDateTime.now());

        // Free compartment safely
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.save(compartment);

        // Update product to SOLD
        Product product = order.getProduct();
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);

        // Invalidate transient pickup code
        activePickupCodes.remove(order.getId());

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    // Helper method for integration tests
    public String getRawPickupCodeForTesting(Long orderId) {
        return activePickupCodes.get(orderId);
    }
}
