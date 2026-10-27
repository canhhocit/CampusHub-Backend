package com.campushub;

import com.campushub.common.enums.*;
import com.campushub.common.exception.*;
import com.campushub.common.security.*;
import com.campushub.modules.admin.service.AdminService;
import com.campushub.modules.auth.dto.*;
import com.campushub.modules.auth.repository.*;
import com.campushub.modules.auth.service.AuthService;
import com.campushub.modules.locker.dto.CompartmentStatusUpdateRequest;
import com.campushub.modules.locker.entity.*;
import com.campushub.modules.locker.repository.*;
import com.campushub.modules.locker.service.LockerService;
import com.campushub.modules.marketplace.dto.ProductStatusUpdateRequest;
import com.campushub.modules.marketplace.entity.*;
import com.campushub.modules.marketplace.repository.*;
import com.campushub.modules.marketplace.service.MarketplaceService;
import com.campushub.modules.order.dto.*;
import com.campushub.modules.order.entity.*;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.order.service.*;
import com.campushub.modules.user.dto.*;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:business-regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000")
@AutoConfigureMockMvc
class BusinessRegressionTest {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired LockerRepository lockers;
    @Autowired CompartmentRepository compartments;
    @Autowired OrderService orderService;
    @Autowired MarketplaceService marketplace;
    @Autowired LockerService lockerService;
    @Autowired UserService userService;
    @Autowired AdminService adminService;
    @Autowired AuthService auth;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired VerificationTokenRepository verificationTokens;
    @Autowired JwtProvider jwt;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired(required = false) com.campushub.modules.wallet.repository.WalletTransactionRepository walletTransactions;
    @Autowired(required = false) com.campushub.modules.wallet.repository.WalletRepository wallets;
    User buyer, seller;
    Product product;
    Locker locker;
    Compartment compartment;

    @BeforeEach void setup() {
        if (walletTransactions != null) walletTransactions.deleteAll();
        if (wallets != null) wallets.deleteAll();
        orders.deleteAll(); products.deleteAll(); compartments.deleteAll(); lockers.deleteAll();
        verificationTokens.deleteAll(); refreshTokens.deleteAll(); users.deleteAll(); categories.deleteAll();
        seller = user("seller"); buyer = user("buyer");
        Category category = new Category(); category.setName("General"); category.setSlug("general"); category.setActive(true);
        category = categories.save(category);
        product = new Product(); product.setTitle("Unique item"); product.setPrice(new BigDecimal("12000"));
        product.setCategory(category); product.setOwner(seller); product.setStatus(ProductStatus.ACTIVE);
        product = products.save(product);
        locker = new Locker(); locker.setCode("L1"); locker.setName("Locker"); locker.setLocation("Location"); locker.setStatus(LockerStatus.ACTIVE);
        locker = lockers.save(locker);
        compartment = new Compartment(); compartment.setLocker(locker); compartment.setCode("C1"); compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartment = compartments.save(compartment);
    }

    private User user(String name) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test");
        user.setFullName(name); user.setPasswordHash("hash"); user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE); user.setIdentityVerified(true); user.setIdentityReviewedAt(java.time.LocalDateTime.now());
        return users.save(user);
    }
    private OrderCreateRequest request() { OrderCreateRequest r = new OrderCreateRequest(); r.setProductId(product.getId()); return r; }
    private Long create() { return orderService.createOrder(request(), buyer.getUsername()).getId(); }
    private Long deposited() {
        Long id = create(); orderService.acceptOrder(id, null, seller.getUsername()); orderService.depositOrder(id, seller.getUsername()); return id;
    }
    private OrderPickupRequest pin(String value) { OrderPickupRequest r = new OrderPickupRequest(); r.setPickupCode(value); return r; }
    private void error(ErrorCode code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(AppException.class, action).getErrorCode());
    }
    private void expire(Long id) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Order o = orders.findById(id).orElseThrow(); o.setPickupDeadline(LocalDateTime.now().minusMinutes(1)); orders.save(o);
        });
        orderService.checkAndExpireOrders();
    }

    @Test void concurrentPurchasesHaveOneWinner() throws Exception {
        List<Object> results = race(() -> orderService.createOrder(request(), "buyer"), () -> orderService.createOrder(request(), "buyer"));
        assertEquals(1, results.stream().filter(OrderResponse.class::isInstance).count());
        assertEquals(1, orders.count());
        assertTrue(results.stream().anyMatch(r -> r instanceof AppException e && e.getErrorCode() == ErrorCode.ACTIVE_ORDER_EXISTS));
    }

    @Test void concurrentAcceptanceAndPickupHaveOneWinner() throws Exception {
        Long id = create();
        List<Object> accepted = race(() -> orderService.acceptOrder(id, null, "seller"), () -> orderService.acceptOrder(id, null, "seller"));
        assertEquals(1, accepted.stream().filter(OrderResponse.class::isInstance).count());
        orderService.depositOrder(id, "seller");
        String code = orderService.getRawPickupCodeForTesting(id);
        List<Object> picked = race(() -> orderService.pickupOrder(id, pin(code), "buyer"), () -> orderService.pickupOrder(id, pin(code), "buyer"));
        assertEquals(1, picked.stream().filter(OrderResponse.class::isInstance).count());
        assertEquals(ProductStatus.SOLD, products.findById(product.getId()).orElseThrow().getStatus());
        assertEquals(CompartmentStatus.AVAILABLE, compartments.findById(compartment.getId()).orElseThrow().getStatus());
    }

    @Test void differentOrdersCannotAllocateSameCompartment() throws Exception {
        Long first = create();
        Product second = new Product(); second.setTitle("Second item"); second.setPrice(BigDecimal.ONE);
        second.setOwner(seller); second.setCategory(product.getCategory()); second.setStatus(ProductStatus.ACTIVE);
        second = products.save(second);
        OrderCreateRequest r = new OrderCreateRequest(); r.setProductId(second.getId());
        Long other = orderService.createOrder(r, "buyer").getId();
        List<Object> allocated = race(() -> orderService.acceptOrder(first, null, "seller"), () -> orderService.acceptOrder(other, null, "seller"));
        assertEquals(1, allocated.stream().filter(OrderResponse.class::isInstance).count());
        assertEquals(1, orders.countByStatus(OrderStatus.AWAITING_DEPOSIT));
    }

    @Test void unhealthyDeviceLeavesOrderAndCompartmentUnchanged() {
        Long id = create(); orderService.acceptOrder(id, null, "seller");
        locker.setStatus(LockerStatus.OFFLINE); lockers.save(locker);
        error(ErrorCode.LOCKER_NOT_ACTIVE, () -> orderService.depositOrder(id, "seller"));
        assertEquals(OrderStatus.AWAITING_DEPOSIT, orders.findById(id).orElseThrow().getStatus());
        assertEquals(CompartmentStatus.RESERVED, compartments.findById(compartment.getId()).orElseThrow().getStatus());
    }

    @Test void refreshRotationDetectsReuseAndRevokesReplacement() throws Exception {
        var rt = new com.campushub.modules.auth.entity.RefreshToken(); rt.setUser(buyer);
        rt.setTokenHash(hash("refresh-secret")); rt.setExpiresAt(LocalDateTime.now().plusDays(1)); refreshTokens.save(rt);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "refresh-secret"));
        assertNotNull(auth.refreshToken(request, new MockHttpServletResponse()).getAccessToken());
        error(ErrorCode.TOKEN_REUSE_DETECTED, () -> auth.refreshToken(request, new MockHttpServletResponse()));
        assertTrue(refreshTokens.findAll().stream().allMatch(com.campushub.modules.auth.entity.RefreshToken::isRevoked));
    }

    @Test void concurrentRefreshDoesNotIssueTwoSuccessfulRotations() throws Exception {
        var rt = new com.campushub.modules.auth.entity.RefreshToken(); rt.setUser(buyer);
        rt.setTokenHash(hash("refresh-secret")); rt.setExpiresAt(LocalDateTime.now().plusDays(1)); refreshTokens.save(rt);
        Callable<Object> refresh = () -> {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "refresh-secret"));
            return auth.refreshToken(request, new MockHttpServletResponse());
        };
        List<Object> rotated = race(refresh, refresh);
        assertEquals(1, rotated.stream().filter(TokenResponse.class::isInstance).count());
    }

    @Test void emailVerificationCannotReactivateLockedAccount() throws Exception {
        buyer.setStatus(UserStatus.LOCKED); users.save(buyer);
        var token = new com.campushub.modules.auth.entity.VerificationToken(); token.setUser(buyer);
        token.setTokenHash(hash("email-secret")); token.setType(TokenType.EMAIL_VERIFY); token.setExpiresAt(LocalDateTime.now().plusHours(1)); verificationTokens.save(token);
        VerifyEmailRequest r = new VerifyEmailRequest(); r.setToken("email-secret");
        error(ErrorCode.INVALID_TOKEN, () -> auth.verifyEmail(r));
        assertEquals(UserStatus.LOCKED, users.findById(buyer.getId()).orElseThrow().getStatus());
    }

    private String hash(String token) throws Exception {
        return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test void wrongPinsCommitAcrossTransactionsAndBlockCorrectPinAfterFive() {
        Long id = deposited(); String code = orderService.getRawPickupCodeForTesting(id);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 1; i <= 5; i++) {
            error(i == 5 ? ErrorCode.PICKUP_ATTEMPTS_EXCEEDED : ErrorCode.INVALID_PICKUP_CODE,
                    () -> orderService.pickupOrder(id, pin(wrong), "buyer"));
            assertEquals(i, orders.findById(id).orElseThrow().getPickupAttempts());
        }
        error(ErrorCode.PICKUP_ATTEMPTS_EXCEEDED, () -> orderService.pickupOrder(id, pin(code), "buyer"));
        assertEquals(CompartmentStatus.OCCUPIED, compartments.findById(compartment.getId()).orElseThrow().getStatus());
    }

    @Test void pickupExpiryCommitsAndRetainsOccupiedCompartment() {
        Long id = deposited();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Order o = orders.findById(id).orElseThrow(); o.setPickupDeadline(LocalDateTime.now().minusMinutes(1)); orders.save(o);
        });
        error(ErrorCode.PICKUP_CODE_EXPIRED, () -> orderService.pickupOrder(id, pin("000000"), "buyer"));
        Order expired = orders.findById(id).orElseThrow();
        assertEquals(OrderStatus.EXPIRED, expired.getStatus()); assertNull(expired.getEncryptedPickupCode());
        assertEquals(CompartmentStatus.OCCUPIED, compartments.findById(compartment.getId()).orElseThrow().getStatus());
    }

    @Test void repeatedClearCannotFreeReusedCompartment() {
        Long id = deposited(); expire(id);
        error(ErrorCode.ACTIVE_ORDER_EXISTS, this::create);
        orderService.adminClearExpiredOrder(id);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Compartment c = compartments.findById(compartment.getId()).orElseThrow(); c.setStatus(CompartmentStatus.RESERVED); compartments.save(c);
        });
        orderService.adminClearExpiredOrder(id);
        assertEquals(CompartmentStatus.RESERVED, compartments.findById(compartment.getId()).orElseThrow().getStatus());
    }

    @Test void occupiedCannotBeFreedThroughErrorState() {
        Long id = deposited();
        CompartmentStatusUpdateRequest r = new CompartmentStatusUpdateRequest(); r.setStatus(CompartmentStatus.ERROR);
        error(ErrorCode.UNSAFE_COMPARTMENT_STATUS_TRANSITION, () -> lockerService.updateCompartmentStatus(locker.getId(), compartment.getId(), r));
        expire(id);
        r.setStatus(CompartmentStatus.AVAILABLE);
        error(ErrorCode.UNSAFE_COMPARTMENT_STATUS_TRANSITION, () -> lockerService.updateCompartmentStatus(locker.getId(), compartment.getId(), r));
    }

    @Test void soldCannotHideAndReactivateAndReservedCannotChange() {
        Long id = create(); ProductStatusUpdateRequest r = new ProductStatusUpdateRequest(); r.setVersion(product.getVersion()); r.setStatus(ProductStatus.HIDDEN);
        error(ErrorCode.ACTIVE_ORDER_EXISTS, () -> marketplace.updateProductStatus(product.getId(), r, "seller"));
        orderService.cancelOrder(id, "buyer"); r.setStatus(ProductStatus.SOLD);
        marketplace.updateProductStatus(product.getId(), r, "seller"); r.setStatus(ProductStatus.HIDDEN);
        error(ErrorCode.INVALID_PRODUCT_STATUS, () -> marketplace.updateProductStatus(product.getId(), r, "seller"));
    }

    @Test void ownershipAndInvalidTransitionsAreRejected() {
        Long id = create();
        error(ErrorCode.UNAUTHORIZED_ACTION, () -> orderService.acceptOrder(id, null, "buyer"));
        error(ErrorCode.INVALID_ORDER_STATUS, () -> orderService.depositOrder(id, "seller"));
        orderService.acceptOrder(id, null, "seller");
        error(ErrorCode.INVALID_ORDER_STATUS, () -> orderService.cancelOrder(id, "buyer"));
        error(ErrorCode.INVALID_ORDER_STATUS, () -> orderService.rejectOrder(id, null, "seller"));
    }

    @Test void submissionDoesNotVerifyIdentity() {
        buyer.setIdentityVerified(false); users.save(buyer);
        VerifyIdentityRequest r = new VerifyIdentityRequest(); r.setCitizenId("123456789012"); r.setIdCardFrontUrl("https://example.test/id.png");
        assertFalse(userService.verifyIdentity("buyer", r).getIdentityVerified());
        error(ErrorCode.IDENTITY_NOT_VERIFIED, this::create);
        adminService.reviewIdentity(buyer.getId(), true);
        assertNotNull(create());
    }

    @Test void suspendedJwtCannotAccessProtectedApi() throws Exception {
        String token = jwt.generateAccessTokenFromUsernameAndIdAndRole("buyer", buyer.getId(), "ROLE_USER");
        buyer.setStatus(UserStatus.LOCKED); users.save(buyer);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void expiredSessionProbeReturns401SoFrontendCanRefresh() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void legacySelfVerifiedFlagDoesNotGrantTransactionAccess() {
        buyer.setCitizenId("123456789012"); buyer.setIdentityReviewedAt(null); users.save(buyer);
        error(ErrorCode.IDENTITY_NOT_VERIFIED, this::create);
        assertEquals("PENDING", userService.getProfile("buyer").getIdentityReviewStatus());
    }

    @Test void concurrentProfileUpdateCannotUndoSuspension() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(); request.setFullName("Updated Name");
        race(() -> userService.updateProfile("buyer", request),
                () -> adminService.updateUserStatus(buyer.getId(), UserStatus.LOCKED, "Test suspension"));
        User updated = users.findById(buyer.getId()).orElseThrow();
        assertEquals(UserStatus.LOCKED, updated.getStatus());
        assertEquals("Updated Name", updated.getFullName());
    }

    @Test void resetTokenIsSingleUseUnderConcurrentRequests() throws Exception {
        var token = new com.campushub.modules.auth.entity.VerificationToken(); token.setUser(buyer);
        token.setTokenHash(hash("reset-secret")); token.setType(TokenType.PASSWORD_RESET);
        token.setExpiresAt(LocalDateTime.now().plusHours(1)); verificationTokens.save(token);
        var request = new ResetPasswordRequest(); request.setToken("reset-secret"); request.setNewPassword("new-test-password");
        Callable<Object> reset = () -> { auth.resetPassword(request); return "success"; };
        List<Object> results = race(reset, reset);
        assertEquals(1, results.stream().filter("success"::equals).count());
        assertTrue(results.stream().anyMatch(r -> r instanceof AppException e && e.getErrorCode() == ErrorCode.INVALID_TOKEN));
    }

    @Test @WithMockUser(roles = "USER") void ordinaryUserCannotApproveIdentity() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/" + buyer.getId() + "/identity").contentType("application/json").content("{\"approved\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test void invalidPaginationReturns400AndServerErrorsHideDetails() throws Exception {
        mvc.perform(get("/api/v1/products").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products").param("size", "0")).andExpect(status().isBadRequest());
        var response = new GlobalExceptionHandler().handleGlobalException(new RuntimeException("internal password detail"));
        assertFalse(response.getBody().get("message").contains("password"));
    }

    private List<Object> race(Callable<?> left, Callable<?> right) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<?> call : List.of(left, right)) futures.add(pool.submit(() -> {
                start.await(); try { return call.call(); } catch (AppException e) { return e; }
            }));
            start.countDown();
            return List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
