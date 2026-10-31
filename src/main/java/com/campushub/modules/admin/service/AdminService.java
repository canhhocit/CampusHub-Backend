package com.campushub.modules.admin.service;

import com.campushub.common.enums.UserStatus;
import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.admin.dto.DashboardStatsResponse;
import com.campushub.modules.admin.dto.UserManagementResponse;
import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.service.AuditService;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;
    private final AuditService auditService;
    private final com.campushub.common.security.TokenBlacklistService tokenBlacklistService;

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            LockerRepository lockerRepository,
            CompartmentRepository compartmentRepository,
            AuditService auditService,
            com.campushub.common.security.TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
        this.auditService = auditService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        stats.setUserStats(getUserStats());
        stats.setProductStats(getProductStats());
        stats.setOrderStats(getOrderStats());
        stats.setLockerStats(getLockerStats());
        stats.setSecurityStats(getSecurityStats());

        auditService.log(AuditAction.ADMIN_ACCESS_DASHBOARD, AuditEntityType.SYSTEM, null, "Admin accessed dashboard");

        return stats;
    }

    private DashboardStatsResponse.UserStats getUserStats() {
        DashboardStatsResponse.UserStats stats = new DashboardStatsResponse.UserStats();

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);

        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByStatus(UserStatus.ACTIVE));
        stats.setPendingUsers(userRepository.countByStatus(UserStatus.PENDING));
        stats.setLockedUsers(userRepository.countByStatus(UserStatus.LOCKED));
        stats.setNewUsersToday(userRepository.countByCreatedAtAfter(today));
        stats.setNewUsersThisWeek(userRepository.countByCreatedAtAfter(weekAgo));
        long verified = userRepository.countByIdentityVerifiedTrue();
        stats.setVerifiedUsers(verified);
        stats.setUnverifiedUsers(Math.max(0, stats.getTotalUsers() - verified));

        return stats;
    }

    private DashboardStatsResponse.ProductStats getProductStats() {
        DashboardStatsResponse.ProductStats stats = new DashboardStatsResponse.ProductStats();

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);

        stats.setTotalProducts(productRepository.count());
        stats.setActiveProducts(productRepository.countByStatus(ProductStatus.ACTIVE));
        stats.setSoldProducts(productRepository.countByStatus(ProductStatus.SOLD));
        stats.setHiddenProducts(productRepository.countByStatus(ProductStatus.HIDDEN));
        stats.setNewProductsToday(productRepository.countByCreatedAtAfter(today));
        stats.setNewProductsThisWeek(productRepository.countByCreatedAtAfter(weekAgo));

        return stats;
    }

    private DashboardStatsResponse.OrderStats getOrderStats() {
        DashboardStatsResponse.OrderStats stats = new DashboardStatsResponse.OrderStats();

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);

        stats.setTotalOrders(orderRepository.count());
        stats.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING_SELLER));
        stats.setAwaitingDepositOrders(orderRepository.countByStatus(OrderStatus.AWAITING_DEPOSIT));
        stats.setReadyForPickupOrders(orderRepository.countByStatus(OrderStatus.READY_FOR_PICKUP));
        stats.setCompletedOrders(orderRepository.countByStatus(OrderStatus.COMPLETED));
        stats.setExpiredOrders(orderRepository.countByStatus(OrderStatus.EXPIRED));
        stats.setCancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED));
        stats.setRejectedOrders(orderRepository.countByStatus(OrderStatus.REJECTED));
        stats.setOrdersToday(orderRepository.countByCreatedAtAfter(today));
        stats.setOrdersThisWeek(orderRepository.countByCreatedAtAfter(weekAgo));
        stats.setCompletedOrdersToday(orderRepository.countByStatusAndPickedUpAtAfter(OrderStatus.COMPLETED, today));

        return stats;
    }

    private DashboardStatsResponse.LockerStats getLockerStats() {
        DashboardStatsResponse.LockerStats stats = new DashboardStatsResponse.LockerStats();

        long totalLockers = lockerRepository.count();
        long activeLockers = lockerRepository.countByStatus(LockerStatus.ACTIVE);
        long totalCompartments = compartmentRepository.count();
        long availableCompartments = compartmentRepository.countByStatus(CompartmentStatus.AVAILABLE);
        long reservedCompartments = compartmentRepository.countByStatus(CompartmentStatus.RESERVED);
        long occupiedCompartments = compartmentRepository.countByStatus(CompartmentStatus.OCCUPIED);

        stats.setTotalLockers(totalLockers);
        stats.setActiveLockers(activeLockers);
        stats.setTotalCompartments(totalCompartments);
        stats.setAvailableCompartments(availableCompartments);
        stats.setReservedCompartments(reservedCompartments);
        stats.setOccupiedCompartments(occupiedCompartments);

        // Calculate utilization rate
        if (totalCompartments > 0) {
            double utilizationRate = ((double) (reservedCompartments + occupiedCompartments) / totalCompartments) * 100;
            stats.setUtilizationRate(Math.round(utilizationRate * 100.0) / 100.0);
        } else {
            stats.setUtilizationRate(0.0);
        }

        return stats;
    }

    private DashboardStatsResponse.SecurityStats getSecurityStats() {
        DashboardStatsResponse.SecurityStats stats = new DashboardStatsResponse.SecurityStats();

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);

        // Get failed login attempts from audit logs
        long failedLoginsToday = auditService.countFailedActionSince(AuditAction.USER_LOGIN, today);
        long failedLoginsThisWeek = auditService.countFailedActionSince(AuditAction.USER_LOGIN, weekAgo);

        long failedPickupsToday = auditService.countFailedActionSince(AuditAction.ORDER_PICKUP_FAILED, today);

        stats.setFailedLoginsToday(failedLoginsToday);
        stats.setFailedLoginsThisWeek(failedLoginsThisWeek);
        stats.setFailedPickupAttemptsToday(failedPickupsToday);
        stats.setExpiredTokensCleanedUp(0); // Will be updated by TokenCleanupScheduler

        return stats;
    }

    @Transactional(readOnly = true)
    public Page<UserManagementResponse> getAllUsers(String search, UserStatus status, Pageable pageable) {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim().toLowerCase() + "%";

        auditService.log(AuditAction.ADMIN_VIEW_USERS, AuditEntityType.SYSTEM, null, 
                "Admin viewed user list with filters: search=" + search + ", status=" + status);

        return userRepository.findAllWithFilters(searchPattern, status, pageable)
                .map(this::mapToUserManagementResponse);
    }

    @Transactional
    public UserManagementResponse updateUserStatus(Long userId, UserStatus newStatus, String reason) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == com.campushub.common.enums.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user = userRepository.save(user);

        if (newStatus == UserStatus.LOCKED) {
            tokenBlacklistService.blacklistUser(userId, java.time.Duration.ofDays(7));
        }

        String details = String.format("Changed user status from %s to %s. Reason: %s", 
                oldStatus, newStatus, reason != null ? reason : "No reason provided");

        auditService.log(AuditAction.USER_STATUS_CHANGE, AuditEntityType.USER, userId, details);

        return mapToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse reviewIdentity(Long userId, boolean approved) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getCitizenId() == null || user.getIdCardFrontUrl() == null) {
            throw new AppException(ErrorCode.INVALID_CITIZEN_ID);
        }
        user.setIdentityVerified(approved);
        user.setIdentityVerifiedAt(approved ? LocalDateTime.now() : null);
        user.setIdentityReviewedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(AuditAction.USER_IDENTITY_REVIEW, AuditEntityType.USER, userId,
                approved ? "Identity approved after manual review" : "Identity verification rejected/revoked");
        return mapToUserManagementResponse(user);
    }

    private UserManagementResponse mapToUserManagementResponse(User user) {
        UserManagementResponse response = new UserManagementResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setDateOfBirth(user.getDateOfBirth());
        boolean isVerified = user.isIdentityApproved();
        response.setIdentityVerified(isVerified);
        response.setIdentityReviewStatus(user.getIdentityReviewStatus());
        response.setCitizenId(user.getCitizenId());
        response.setIdCardFrontUrl(user.getIdCardFrontUrl());
        response.setIdCardBackUrl(user.getIdCardBackUrl());
        response.setIdentityVerifiedAt(user.getIdentityVerifiedAt());
        response.setCreatedAt(user.getCreatedAt());

        // Count user's products and orders
        response.setTotalProducts(productRepository.countByOwnerId(user.getId()));
        response.setTotalOrders(orderRepository.countByBuyerIdOrSellerId(user.getId(), user.getId()));

        return response;
    }
}
