package com.campushub.modules.locker.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.service.AuditService;
import com.campushub.modules.locker.dto.KioskActionResponse;
import com.campushub.modules.locker.dto.KioskDepositRequest;
import com.campushub.modules.locker.dto.KioskPickupRequest;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.notification.service.NotificationService;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.OrderType;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.common.security.AesEncryptionService;
import com.campushub.modules.order.service.MockLockerService;
import com.campushub.modules.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KioskService {

    private static final Logger log = LoggerFactory.getLogger(KioskService.class);

    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MockLockerService mockLockerService;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public KioskService(LockerRepository lockerRepository,
                        CompartmentRepository compartmentRepository,
                        OrderRepository orderRepository,
                        ProductRepository productRepository,
                        MockLockerService mockLockerService,
                        PasswordEncoder passwordEncoder,
                        AesEncryptionService aesEncryptionService,
                        WalletService walletService,
                        NotificationService notificationService,
                        AuditService auditService) {
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.mockLockerService = mockLockerService;
        this.passwordEncoder = passwordEncoder;
        this.aesEncryptionService = aesEncryptionService;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public KioskActionResponse processPickupByPin(Long lockerId, KioskPickupRequest request) {
        Locker locker = lockerRepository.findById(lockerId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        String pinCode = request.getPinCode();

        // 1. Search candidate orders for this locker
        List<Order> candidateOrders = orderRepository.findAll().stream()
                .filter(o -> o.getLocker() != null && o.getLocker().getId().equals(lockerId))
                .filter(o -> o.getStatus() == OrderStatus.READY_FOR_PICKUP || o.getStatus() == OrderStatus.READY_FOR_RETURN_PICKUP)
                .toList();

        Order matchedOrder = null;
        boolean isReturnPickup = false;

        for (Order o : candidateOrders) {
            if (o.getStatus() == OrderStatus.READY_FOR_PICKUP && o.getPickupCodeHash() != null) {
                if (passwordEncoder.matches(pinCode, o.getPickupCodeHash())) {
                    matchedOrder = o;
                    isReturnPickup = false;
                    break;
                }
            } else if (o.getStatus() == OrderStatus.READY_FOR_RETURN_PICKUP && o.getReturnPickupCodeHash() != null) {
                if (passwordEncoder.matches(pinCode, o.getReturnPickupCodeHash())) {
                    matchedOrder = o;
                    isReturnPickup = true;
                    break;
                }
            }
        }

        if (matchedOrder == null) {
            log.warn("Kiosk PIN validation failed at locker #{} for pin: {}", lockerId, pinCode);
            auditService.log(AuditAction.ORDER_PICKUP_FAILED, AuditEntityType.LOCKER, lockerId,
                    "Kiosk PIN validation failed", false, ErrorCode.INVALID_PICKUP_CODE.name());
            throw new AppException(ErrorCode.INVALID_PICKUP_CODE, "Mã PIN không đúng hoặc đơn hàng không thuộc tủ này");
        }

        Compartment compartment = isReturnPickup ? matchedOrder.getReturnCompartment() : matchedOrder.getCompartment();
        if (compartment == null) {
            throw new AppException(ErrorCode.COMPARTMENT_NOT_FOUND);
        }

        // Simulate IoT door open
        if (!mockLockerService.openForPickup(locker, compartment) || !mockLockerService.confirmItemPickedUp(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        // Free compartment
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.save(compartment);

        KioskActionResponse response = new KioskActionResponse();
        response.setSuccess(true);
        response.setLockerId(locker.getId());
        response.setLockerCode(locker.getCode());
        response.setCompartmentId(compartment.getId());
        response.setCompartmentCode(compartment.getCode());
        response.setOrderNumber(matchedOrder.getOrderNumber());
        if (matchedOrder.getProduct() != null) {
            response.setProductTitle(matchedOrder.getProduct().getTitle());
        }

        if (!isReturnPickup) {
            // Buyer regular pickup
            matchedOrder.setPickedUpAt(LocalDateTime.now());
            matchedOrder.setEncryptedPickupCode(null);
            matchedOrder.setPickupCodeHash(null);

            Product product = matchedOrder.getProduct();
            if (matchedOrder.getOrderType() == OrderType.RENTAL) {
                matchedOrder.setStatus(OrderStatus.RENTING);
                int duration = matchedOrder.getRentalDurationDays() != null && matchedOrder.getRentalDurationDays() > 0 
                        ? matchedOrder.getRentalDurationDays() 
                        : 1;
                matchedOrder.setDueReturnAt(matchedOrder.getPickedUpAt().plusDays(duration));
                product.setStatus(ProductStatus.RENTED);
            } else {
                matchedOrder.setStatus(OrderStatus.COMPLETED);
                product.setStatus(ProductStatus.SOLD);
                walletService.creditOrderPayout(matchedOrder);
            }
            productRepository.save(product);
            orderRepository.save(matchedOrder);

            response.setActionType("BUYER_PICKUP");
            response.setMessage(String.format("Xác thực mã PIN thành công! Ngăn tủ %s đã mở. Vui lòng lấy món đồ '%s' và đóng cửa tủ.",
                    compartment.getCode(), product.getTitle()));

            notificationService.createNotification(
                    matchedOrder.getSeller(),
                    "Người mua đã nhận hàng qua Kiosk Locker",
                    String.format("Người mua đã nhập PIN và nhận hàng '%s' từ tủ %s (ngăn %s).",
                            product.getTitle(), locker.getName(), compartment.getCode()),
                    "ORDER_PICKED_UP",
                    "ORDER",
                    matchedOrder.getId()
            );
        } else {
            // Seller return pickup
            matchedOrder.setStatus(OrderStatus.COMPLETED);
            matchedOrder.setReturnPickedUpAt(LocalDateTime.now());
            matchedOrder.setEncryptedReturnPickupCode(null);
            matchedOrder.setReturnPickupCodeHash(null);

            Product product = matchedOrder.getProduct();
            product.setStatus(ProductStatus.ACTIVE);
            productRepository.save(product);

            walletService.creditOrderPayout(matchedOrder);
            walletService.refundRentalDeposit(matchedOrder);
            orderRepository.save(matchedOrder);

            response.setActionType("RETURN_PICKUP");
            response.setMessage(String.format("Nhận lại đồ thuê thành công! Ngăn tủ %s đã mở. Tiền cọc đã được tự động hoàn trả cho người thuê.",
                    compartment.getCode()));

            notificationService.createNotification(
                    matchedOrder.getBuyer(),
                    "Đơn thuê hoàn tất",
                    String.format("Người cho thuê đã nhận lại sản phẩm '%s'. Tiền đặt cọc đã được hoàn về ví của bạn.", product.getTitle()),
                    "ORDER_COMPLETED",
                    "ORDER",
                    matchedOrder.getId()
            );
        }

        log.info("Kiosk pickup completed at locker #{} (compartment {}) for order #{}",
                lockerId, compartment.getCode(), matchedOrder.getId());
        return response;
    }

    @Transactional
    public KioskActionResponse processDepositByOrderNumber(Long lockerId, KioskDepositRequest request) {
        Locker locker = lockerRepository.findById(lockerId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        String orderNum = request.getOrderNumber().trim();
        Order order = orderRepository.findByOrderNumber(orderNum)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng với mã: " + orderNum));

        if (!order.getLocker().getId().equals(lockerId)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, "Đơn hàng này được chỉ định ở trạm tủ khác");
        }

        if (order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, "Đơn hàng không ở trạng thái chờ nạp đồ vào tủ");
        }

        Compartment compartment = order.getCompartment();
        if (compartment == null) {
            throw new AppException(ErrorCode.NO_AVAILABLE_COMPARTMENT);
        }

        // Simulate IoT operations
        if (!mockLockerService.openForDeposit(locker, compartment) || !mockLockerService.confirmItemDeposited(locker, compartment)) {
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }

        compartment.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.save(compartment);

        // Generate 6-digit pickup code for buyer
        int codeInt = secureRandom.nextInt(1_000_000);
        String rawPickupCode = String.format("%06d", codeInt);

        order.setPickupCodeHash(passwordEncoder.encode(rawPickupCode));
        order.setEncryptedPickupCode(aesEncryptionService.encrypt(rawPickupCode));
        order.setPickupDeadline(LocalDateTime.now().plusHours(48));
        order.setDepositAt(LocalDateTime.now());
        order.setPickupAttempts(0);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);

        // Notify buyer
        notificationService.createNotification(
                order.getBuyer(),
                "Hàng đã được gửi vào tủ!",
                String.format("Người bán đã gửi hàng '%s' vào tủ %s (Ngăn %s). Mã PIN lấy đồ: %s",
                        order.getProduct().getTitle(), locker.getName(), compartment.getCode(), rawPickupCode),
                "ORDER_DEPOSITED",
                "ORDER",
                order.getId()
        );

        KioskActionResponse response = new KioskActionResponse();
        response.setSuccess(true);
        response.setActionType("SELLER_DEPOSIT");
        response.setLockerId(locker.getId());
        response.setLockerCode(locker.getCode());
        response.setCompartmentId(compartment.getId());
        response.setCompartmentCode(compartment.getCode());
        response.setOrderNumber(order.getOrderNumber());
        response.setProductTitle(order.getProduct().getTitle());
        response.setMessage(String.format("Mở ngăn tủ %s thành công! Vui lòng đặt món hàng vào trong và đóng chặt cửa tủ.", compartment.getCode()));

        log.info("Kiosk deposit completed at locker #{} (compartment {}) for order #{}",
                lockerId, compartment.getCode(), order.getId());
        return response;
    }
}

