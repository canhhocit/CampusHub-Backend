package com.campushub.modules.order.service;

import com.campushub.common.email.EmailService;
import com.campushub.common.enums.Role;
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
import com.campushub.modules.order.entity.OrderType;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import java.math.BigDecimal;
import com.campushub.common.security.AesEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;
    private final MockLockerService mockLockerService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AesEncryptionService aesEncryptionService;
    private final OrderAttemptService orderAttemptService;
    private final com.campushub.modules.audit.service.AuditService auditService;
    private final com.campushub.modules.admin.service.SystemSettingService systemSettingService;
    private final com.campushub.modules.wallet.service.WalletService walletService;

    // Transient in-memory cache for fast lookup during active session
    private final Map<Long, String> activePickupCodes = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        LockerRepository lockerRepository,
                        CompartmentRepository compartmentRepository,
                        MockLockerService mockLockerService,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        AesEncryptionService aesEncryptionService,
                        OrderAttemptService orderAttemptService,
                        com.campushub.modules.audit.service.AuditService auditService,
                        com.campushub.modules.admin.service.SystemSettingService systemSettingService,
                        com.campushub.modules.wallet.service.WalletService walletService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
        this.mockLockerService = mockLockerService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.aesEncryptionService = aesEncryptionService;
        this.orderAttemptService = orderAttemptService;
        this.auditService = auditService;
        this.systemSettingService = systemSettingService;
        this.walletService = walletService;
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

        if (order.getReturnCompartment() != null) {
            res.setReturnCompartmentId(order.getReturnCompartment().getId());
            res.setReturnCompartmentCode(order.getReturnCompartment().getCode());
            if (order.getReturnCompartment().getLocker() != null) {
                res.setReturnLockerId(order.getReturnCompartment().getLocker().getId());
                res.setReturnLockerName(order.getReturnCompartment().getLocker().getName());
            }
        }

        res.setOrderType(order.getOrderType());
        res.setRentalDurationDays(order.getRentalDurationDays());
        res.setDueReturnAt(order.getDueReturnAt());
        res.setDepositAmount(order.getDepositAmount());
        res.setPlatformFee(order.getPlatformFee());
        res.setSellerPayout(order.getSellerPayout());
        java.math.BigDecimal total1 = (order.getPriceSnapshot() != null ? order.getPriceSnapshot() : java.math.BigDecimal.ZERO)
                .add(order.getDepositAmount() != null ? order.getDepositAmount() : java.math.BigDecimal.ZERO);
        res.setTotalAmount(total1);

        res.setPaymentStatus(order.getPaymentStatus());
        res.setPaymentMethod(order.getPaymentMethod());
        res.setPayosOrderCode(order.getPayosOrderCode());
        res.setPaymentPaidAt(order.getPaymentPaidAt());
        res.setRefundedAt(order.getRefundedAt());

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

        res.setOrderType(order.getOrderType());
        res.setRentalDurationDays(order.getRentalDurationDays());
        res.setDueReturnAt(order.getDueReturnAt());
        res.setDepositAmount(order.getDepositAmount());
        res.setPlatformFee(order.getPlatformFee());
        res.setSellerPayout(order.getSellerPayout());
        java.math.BigDecimal total2 = (order.getPriceSnapshot() != null ? order.getPriceSnapshot() : java.math.BigDecimal.ZERO)
                .add(order.getDepositAmount() != null ? order.getDepositAmount() : java.math.BigDecimal.ZERO);
        res.setTotalAmount(total2);

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

        if (order.getReturnCompartment() != null) {
            res.setReturnCompartmentId(order.getReturnCompartment().getId());
            res.setReturnCompartmentCode(order.getReturnCompartment().getCode());
            if (order.getReturnCompartment().getLocker() != null) {
                res.setReturnLockerId(order.getReturnCompartment().getLocker().getId());
                res.setReturnLockerName(order.getReturnCompartment().getLocker().getName());
            }
        }

        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());

        res.setPickupDeadline(order.getPickupDeadline());
        res.setPickupAttempts(order.getPickupAttempts());
        res.setDepositAt(order.getDepositAt());
        res.setPickedUpAt(order.getPickedUpAt());
        res.setReturnDepositAt(order.getReturnDepositAt());
        res.setReturnPickedUpAt(order.getReturnPickedUpAt());
        res.setCancelledAt(order.getCancelledAt());
        res.setRejectionReason(order.getRejectionReason());
        res.setVersion(order.getVersion());

        // Security check: ONLY the buyer can see the pickup code on their active order
        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP && order.getBuyer().getUsername().equals(username)) {
            if (order.getEncryptedPickupCode() != null) {
                res.setPickupCode(aesEncryptionService.decrypt(order.getEncryptedPickupCode()));
            } else {
                res.setPickupCode(activePickupCodes.get(order.getId()));
            }
        }

        // Security check: ONLY the seller (item owner) can see the return pickup code
        if (order.getStatus() == OrderStatus.READY_FOR_RETURN_PICKUP && order.getSeller().getUsername().equals(username)) {
            if (order.getEncryptedReturnPickupCode() != null) {
                res.setPickupCode(aesEncryptionService.decrypt(order.getEncryptedReturnPickupCode()));
            }
        }

        return res;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String buyerUsername) {
        User buyer = userRepository.findByUsername(buyerUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (buyer.getRole() != com.campushub.common.enums.Role.ADMIN && !buyer.isIdentityApproved()) {
            throw new AppException(ErrorCode.IDENTITY_NOT_VERIFIED);
        }

        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE || !Boolean.TRUE.equals(product.getCategory().getActive())
                || product.getOwner().getStatus() != com.campushub.common.enums.UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE_FOR_ORDER);
        }

        if (product.getOwner().getId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.CANNOT_ORDER_OWN_PRODUCT);
        }

        Set<OrderStatus> activeStatuses = Set.of(
                OrderStatus.PENDING_SELLER,
                OrderStatus.AWAITING_DEPOSIT,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.RENTING,
                OrderStatus.AWAITING_RETURN_DEPOSIT,
                OrderStatus.READY_FOR_RETURN_PICKUP
        );
        if (orderRepository.hasProductReservation(product.getId(), activeStatuses)) {
            throw new AppException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }

        OrderType orderType = request.getOrderType() != null ? request.getOrderType() : OrderType.PURCHASE;

        Order order = new Order();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + uniqueSuffix);
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(product.getOwner());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order.setOrderType(orderType);

        if (orderType == OrderType.RENTAL) {
            int durationDays = (request.getRentalDurationDays() != null && request.getRentalDurationDays() > 0)
                    ? request.getRentalDurationDays() : 1;
            var feeCalc = systemSettingService.calculateFees(
                    OrderType.RENTAL,
                    product.getPrice(),
                    product.getRentPricePerDay(),
                    product.getDepositAmount(),
                    durationDays
            );
            order.setRentalDurationDays(durationDays);
            order.setPriceSnapshot(feeCalc.totalAmount().subtract(feeCalc.depositAmount()));
            order.setDepositAmount(feeCalc.depositAmount());
            order.setPlatformFee(feeCalc.platformFee());
            order.setSellerPayout(feeCalc.sellerPayout());
        } else {
            var feeCalc = systemSettingService.calculateFees(
                    OrderType.PURCHASE,
                    product.getPrice(),
                    null,
                    null,
                    null
            );
            order.setPriceSnapshot(product.getPrice());
            order.setDepositAmount(BigDecimal.ZERO);
            order.setPlatformFee(feeCalc.platformFee());
            order.setSellerPayout(feeCalc.sellerPayout());
        }

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
            if (user.getRole() != Role.ADMIN) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
            }
        }

        return mapToOrderDetailResponse(order, username);
    }

    @Transactional
    public OrderResponse acceptOrder(Long orderId, OrderAcceptRequest request, String sellerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (order.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE_FOR_ORDER);
        }

        // Allocate compartment in an ACTIVE locker with pessimistic lock
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

        // Lock and verify compartment is still available
        lockLocker(compartment.getLocker().getId());
        Compartment lockedCompartment = compartmentRepository.findByIdWithLock(compartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
        entityManager.refresh(lockedCompartment, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (lockedCompartment.getLocker().getStatus() != LockerStatus.ACTIVE) {
            throw new AppException(ErrorCode.LOCKER_NOT_ACTIVE);
        }
        if (lockedCompartment.getStatus() != CompartmentStatus.AVAILABLE) {
            throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
        }

        lockedCompartment.setStatus(CompartmentStatus.RESERVED);
        compartmentRepository.save(lockedCompartment);

        order.setLocker(lockedCompartment.getLocker());
        order.setCompartment(lockedCompartment);
        order.setStatus(OrderStatus.AWAITING_DEPOSIT);

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(Long orderId, OrderRejectRequest request, String sellerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setEncryptedPickupCode(null);
        activePickupCodes.remove(order.getId());
        if (request != null && request.getReason() != null) {
            order.setRejectionReason(request.getReason().trim());
        }

        order = orderRepository.save(order);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            walletService.refundOrderPayment(order, "Hoàn tiền do người bán từ chối đơn hàng");
        }
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String buyerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.PENDING_SELLER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setEncryptedPickupCode(null);
        activePickupCodes.remove(order.getId());
        order.setCancelledAt(LocalDateTime.now());

        order = orderRepository.save(order);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            walletService.refundOrderPayment(order, "Hoàn tiền do người mua hủy đơn hàng");
        }
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse depositOrder(Long orderId, String sellerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
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
        locker = lockLocker(locker.getId());
        compartment = compartmentRepository.findByIdWithLock(compartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
        if (compartment.getStatus() != CompartmentStatus.RESERVED
                || !compartment.getLocker().getId().equals(locker.getId())) {
            throw new AppException(ErrorCode.INVALID_COMPARTMENT_STATUS);
        }

        // Simulate IoT operations
        if (!mockLockerService.openForDeposit(locker, compartment)
                || !mockLockerService.confirmItemDeposited(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        // Transition compartment to OCCUPIED
        compartment.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.save(compartment);

        // Generate 6-digit cryptographically secure pickup code
        int codeInt = secureRandom.nextInt(1_000_000);
        String rawPickupCode = String.format("%06d", codeInt);

        // Hash pickup code with BCrypt and store AES-encrypted version for persistence
        order.setPickupCodeHash(passwordEncoder.encode(rawPickupCode));
        order.setEncryptedPickupCode(aesEncryptionService.encrypt(rawPickupCode));
        order.setPickupDeadline(LocalDateTime.now().plusHours(48));
        order.setDepositAt(LocalDateTime.now());
        order.setPickupAttempts(0);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);

        order = orderRepository.save(order);

        // Keep raw code in transient cache for instant access
        activePickupCodes.put(order.getId(), rawPickupCode);

        // Dispatch asynchronous email safely after transaction commits
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
        sendEmailAfterCommit(order.getBuyer().getEmail(), subject, text);

        return mapToOrderResponse(order);
    }

    @Transactional(noRollbackFor = PickupValidationException.class)
    public OrderResponse pickupOrder(Long orderId, OrderPickupRequest request, String buyerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.ORDER_EXPIRED);
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (order.getPickupDeadline() != null && LocalDateTime.now().isAfter(order.getPickupDeadline())) {
            order.setStatus(OrderStatus.EXPIRED);
            order.setEncryptedPickupCode(null);
            activePickupCodes.remove(order.getId());
            orderRepository.save(order);
            order.setPickupCodeHash(null);
            throw new PickupValidationException(ErrorCode.PICKUP_CODE_EXPIRED);
        }

        if (order.getPickupAttempts() != null && order.getPickupAttempts() >= 5) {
            throw new AppException(ErrorCode.PICKUP_ATTEMPTS_EXCEEDED);
        }

        if (!passwordEncoder.matches(request.getPickupCode(), order.getPickupCodeHash())) {
            int newAttempts = orderAttemptService.recordFailedPickupAttempt(order.getId());
            auditService.log(com.campushub.modules.audit.entity.AuditAction.ORDER_PICKUP_FAILED,
                    com.campushub.modules.audit.entity.AuditEntityType.ORDER, order.getId(),
                    "Invalid pickup PIN attempt", false, ErrorCode.INVALID_PICKUP_CODE.name());
            if (newAttempts >= 5) {
                throw new PickupValidationException(ErrorCode.PICKUP_ATTEMPTS_EXCEEDED);
            }
            throw new PickupValidationException(ErrorCode.INVALID_PICKUP_CODE);
        }

        Locker locker = order.getLocker();
        Compartment compartment = order.getCompartment();

        // Simulate IoT operations
        locker = lockLocker(locker.getId());
        compartment = compartmentRepository.findByIdWithLock(compartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
        if (compartment.getStatus() != CompartmentStatus.OCCUPIED) {
            throw new AppException(ErrorCode.INVALID_COMPARTMENT_STATUS);
        }
        if (!mockLockerService.openForPickup(locker, compartment)
                || !mockLockerService.confirmItemPickedUp(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        // Handle post-pickup state depending on order type (PURCHASE vs RENTAL)
        Product product = order.getProduct();
        order.setPickedUpAt(LocalDateTime.now());
        order.setEncryptedPickupCode(null);
        order.setPickupCodeHash(null);

        // Free compartment safely since buyer took the item
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.save(compartment);

        if (order.getOrderType() == OrderType.RENTAL) {
            order.setStatus(OrderStatus.RENTING);
            int durationDays = order.getRentalDurationDays() != null && order.getRentalDurationDays() > 0 
                    ? order.getRentalDurationDays() 
                    : 1;
            order.setDueReturnAt(order.getPickedUpAt().plusDays(durationDays));
            product.setStatus(ProductStatus.RENTED);

            sendEmailAfterCommit(
                    order.getBuyer().getEmail(),
                    "CampusHub - Bắt đầu thời gian thuê: Đơn " + order.getOrderNumber(),
                    "Bạn đã nhận thành công sản phẩm '" + product.getTitle() + "'. Thời hạn thuê là " + durationDays + " ngày, hạn trả là: " + order.getDueReturnAt()
            );
        } else {
            order.setStatus(OrderStatus.COMPLETED);
            product.setStatus(ProductStatus.SOLD);
            walletService.creditOrderPayout(order);
        }
        productRepository.save(product);

        // Invalidate transient pickup code
        activePickupCodes.remove(order.getId());

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse initiateReturn(Long orderId, OrderReturnRequest request, String buyerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getOrderType() != OrderType.RENTAL || order.getStatus() != OrderStatus.RENTING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Locker locker = lockLocker(request.getLockerId());
        Compartment returnCompartment;
        if (request.getCompartmentId() != null) {
            returnCompartment = compartmentRepository.findByIdWithLock(request.getCompartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
            entityManager.refresh(returnCompartment, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (!returnCompartment.getLocker().getId().equals(locker.getId())) {
                throw new AppException(ErrorCode.COMPARTMENT_NOT_FOUND);
            }
            if (returnCompartment.getStatus() != CompartmentStatus.AVAILABLE) {
                throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
            }
        } else {
            Compartment candidate = compartmentRepository.findFirstByLockerIdAndStatus(locker.getId(), CompartmentStatus.AVAILABLE)
                    .orElseThrow(() -> new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT));
            returnCompartment = compartmentRepository.findByIdWithLock(candidate.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
            entityManager.refresh(returnCompartment, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (returnCompartment.getStatus() != CompartmentStatus.AVAILABLE) {
                throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
            }
        }

        returnCompartment.setStatus(CompartmentStatus.RESERVED);
        compartmentRepository.save(returnCompartment);

        order.setReturnCompartment(returnCompartment);
        order.setStatus(OrderStatus.AWAITING_RETURN_DEPOSIT);

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse depositReturn(Long orderId, String buyerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.AWAITING_RETURN_DEPOSIT) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Compartment compartment = order.getReturnCompartment();
        if (compartment == null) {
            throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
        }

        Locker locker = lockLocker(compartment.getLocker().getId());
        compartment = compartmentRepository.findByIdWithLock(compartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
        if (compartment.getStatus() != CompartmentStatus.RESERVED) {
            throw new AppException(ErrorCode.INVALID_COMPARTMENT_STATUS);
        }

        // Simulate IoT drop-off operations
        if (!mockLockerService.openForDeposit(locker, compartment)
                || !mockLockerService.confirmItemDeposited(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        compartment.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.save(compartment);

        int codeInt = secureRandom.nextInt(1_000_000);
        String rawReturnPickupCode = String.format("%06d", codeInt);

        order.setReturnPickupCodeHash(passwordEncoder.encode(rawReturnPickupCode));
        order.setEncryptedReturnPickupCode(aesEncryptionService.encrypt(rawReturnPickupCode));
        order.setReturnDepositAt(LocalDateTime.now());
        order.setStatus(OrderStatus.READY_FOR_RETURN_PICKUP);

        order = orderRepository.save(order);

        // Notify seller (item owner) that item was returned into locker
        String subject = "CampusHub - Thông báo lấy lại đồ cho thuê: Đơn " + order.getOrderNumber();
        String text = String.format(
                "Xin chào %s,\n\nNgười thuê đã trả sản phẩm '%s' về tủ đồ.\n" +
                "Địa điểm: %s (%s)\n" +
                "Ngăn tủ: %s\n" +
                "Mã mở tủ nhận lại đồ của bạn là: %s\n\n" +
                "Vui lòng đến kiểm tra và nhận lại sản phẩm.",
                order.getSeller().getFullName(),
                order.getProduct().getTitle(),
                locker.getName(),
                locker.getLocation(),
                compartment.getCode(),
                rawReturnPickupCode
        );
        sendEmailAfterCommit(order.getSeller().getEmail(), subject, text);

        return mapToOrderResponse(order);
    }

    @Transactional(noRollbackFor = PickupValidationException.class)
    public OrderResponse pickupReturn(Long orderId, OrderPickupRequest request, String sellerUsername) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUsername().equals(sellerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.READY_FOR_RETURN_PICKUP) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (order.getReturnPickupCodeHash() == null || !passwordEncoder.matches(request.getPickupCode(), order.getReturnPickupCodeHash())) {
            throw new PickupValidationException(ErrorCode.INVALID_PICKUP_CODE);
        }

        Compartment compartment = order.getReturnCompartment();
        if (compartment == null) {
            throw new AppException(ErrorCode.COMPARTMENT_NOT_FOUND);
        }
        Locker locker = lockLocker(compartment.getLocker().getId());
        compartment = compartmentRepository.findByIdWithLock(compartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));

        if (compartment.getStatus() != CompartmentStatus.OCCUPIED) {
            throw new AppException(ErrorCode.INVALID_COMPARTMENT_STATUS);
        }

        if (!mockLockerService.openForPickup(locker, compartment)
                || !mockLockerService.confirmItemPickedUp(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.save(compartment);

        order.setStatus(OrderStatus.COMPLETED);
        order.setReturnPickedUpAt(LocalDateTime.now());
        order.setEncryptedReturnPickupCode(null);
        order.setReturnPickupCodeHash(null);

        Product product = order.getProduct();
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        order = orderRepository.save(order);

        walletService.creditOrderPayout(order);
        walletService.refundRentalDeposit(order);

        // Notify buyer that deposit refund/settlement is completed
        String subject = "CampusHub - Hoàn tất trả đồ thuê: Đơn " + order.getOrderNumber();
        String text = String.format(
                "Xin chào %s,\n\nChủ đồ đã nhận lại sản phẩm '%s'. Đơn thuê của bạn đã hoàn tất.\n" +
                "Tiền cọc (nếu có: %s VNĐ) đã được hoàn trả.\n\nCảm ơn bạn đã sử dụng CampusHub!",
                order.getBuyer().getFullName(),
                product.getTitle(),
                order.getDepositAmount() != null ? order.getDepositAmount().toString() : "0"
        );
        sendEmailAfterCommit(order.getBuyer().getEmail(), subject, text);

        return mapToOrderResponse(order);
    }

    @Transactional
    public void checkAndExpireOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findExpiredOrders(OrderStatus.READY_FOR_PICKUP, now);
        for (Order candidate : expiredOrders) {
            Order order = orderRepository.findByIdWithLock(candidate.getId()).orElseThrow();
            entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (order.getStatus() != OrderStatus.READY_FOR_PICKUP || order.getPickupDeadline() == null
                    || !order.getPickupDeadline().isBefore(now)) {
                continue;
            }
            order.setStatus(OrderStatus.EXPIRED);
            order.setEncryptedPickupCode(null);
            order.setPickupCodeHash(null);
            activePickupCodes.remove(order.getId());
            orderRepository.save(order);

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                walletService.refundOrderPayment(order, "Hoàn tiền do đơn hàng quá hạn nhận");
            }

            sendEmailAfterCommit(
                    order.getBuyer().getEmail(),
                    "CampusHub - Đơn hàng đã quá hạn nhận: " + order.getOrderNumber(),
                    "Đơn hàng của bạn tại tủ " + (order.getLocker() != null ? order.getLocker().getName() : "") +
                    " đã quá hạn lấy đồ. Vui lòng liên hệ ban quản trị để được hỗ trợ xử lý kiện hàng."
            );
            sendEmailAfterCommit(
                    order.getSeller().getEmail(),
                    "CampusHub - Đơn hàng quá hạn nhận: " + order.getOrderNumber(),
                    "Đơn hàng " + order.getOrderNumber() + " đã quá hạn nhận bởi người mua. Kiện hàng đang được bảo quản an toàn trong tủ đồ."
            );
        }
    }

    @Transactional
    public OrderResponse adminClearExpiredOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.CANNOT_CLEAR_NON_EXPIRED_ORDER);
        }
        if (order.getClearedAt() != null) {
            return mapToOrderResponse(order);
        }

        Locker locker = order.getLocker();
        Compartment compartment = order.getCompartment();

        if (locker != null && compartment != null) {
            locker = lockLocker(locker.getId());
            compartment = compartmentRepository.findByIdWithLock(compartment.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
            if (!mockLockerService.openForPickup(locker, compartment)
                    || !mockLockerService.confirmItemPickedUp(locker, compartment)) {
                throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
            }
            compartment.setStatus(CompartmentStatus.AVAILABLE);
            compartmentRepository.save(compartment);
        }

        Product product = order.getProduct();
        if (product != null) {
            product.setStatus(ProductStatus.HIDDEN);
            productRepository.save(product);
        }

        order.setClearedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    private Locker lockLocker(Long id) {
        Locker locker = lockerRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));
        entityManager.refresh(locker, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (locker.getStatus() != LockerStatus.ACTIVE) {
            throw new AppException(ErrorCode.LOCKER_NOT_ACTIVE);
        }
        return locker;
    }

    private void sendEmailAfterCommit(String to, String subject, String text) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        emailService.sendEmail(to, subject, text);
                    } catch (Exception e) {
                        log.warn("Could not dispatch email notification to {}: {}", to, e.getMessage());
                    }
                }
            });
        } else {
            try {
                emailService.sendEmail(to, subject, text);
            } catch (Exception e) {
                log.warn("Could not dispatch email notification to {}: {}", to, e.getMessage());
            }
        }
    }

    // Helper method for integration tests
    public String getRawPickupCodeForTesting(Long orderId) {
        return activePickupCodes.get(orderId);
    }
}
