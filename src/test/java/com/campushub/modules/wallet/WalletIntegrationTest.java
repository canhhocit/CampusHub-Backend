package com.campushub.modules.wallet;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.ListingType;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.dto.OrderPickupRequest;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.OrderType;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.order.service.OrderService;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.modules.wallet.dto.AdminPayoutActionRequest;
import com.campushub.modules.wallet.dto.UserBankAccountRequest;
import com.campushub.modules.wallet.dto.WithdrawalRequestDto;
import com.campushub.modules.wallet.entity.PayoutRequest;
import com.campushub.modules.wallet.entity.Wallet;
import com.campushub.modules.wallet.enums.PayoutStatus;
import com.campushub.modules.wallet.enums.TransactionType;
import com.campushub.modules.wallet.repository.PayoutRequestRepository;
import com.campushub.modules.wallet.repository.UserBankAccountRepository;
import com.campushub.modules.wallet.repository.WalletRepository;
import com.campushub.modules.wallet.repository.WalletTransactionRepository;
import com.campushub.modules.wallet.service.PayoutAdminService;
import com.campushub.modules.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LockerRepository lockerRepository;

    @Autowired
    private CompartmentRepository compartmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PayoutAdminService payoutAdminService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private UserBankAccountRepository userBankAccountRepository;

    @Autowired
    private PayoutRequestRepository payoutRequestRepository;

    private User buyer;
    private User seller;
    private User admin;
    private Locker locker;
    private Compartment compartment;
    private Category category;

    @BeforeEach
    void setUp() {
        buyer = userRepository.findByUsername("wallet_buyer").orElseGet(() -> {
            User u = new User();
            u.setUsername("wallet_buyer");
            u.setEmail("wallet_buyer@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Wallet Buyer");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        seller = userRepository.findByUsername("wallet_seller").orElseGet(() -> {
            User u = new User();
            u.setUsername("wallet_seller");
            u.setEmail("wallet_seller@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Wallet Seller");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        admin = userRepository.findByUsername("wallet_admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("wallet_admin");
            u.setEmail("wallet_admin@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Wallet Admin");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        walletRepository.findByUserId(seller.getId()).ifPresent(w -> {
            w.setBalance(BigDecimal.ZERO);
            w.setFrozenBalance(BigDecimal.ZERO);
            walletRepository.save(w);
        });
        walletRepository.findByUserId(buyer.getId()).ifPresent(w -> {
            w.setBalance(BigDecimal.ZERO);
            w.setFrozenBalance(BigDecimal.ZERO);
            walletRepository.save(w);
        });

        category = new Category();
        category.setName("Category Wallet " + System.nanoTime());
        category.setSlug("cat-wallet-" + System.nanoTime());
        category.setActive(true);
        category = categoryRepository.save(category);

        locker = new Locker();
        locker.setName("Locker Wallet " + System.nanoTime());
        locker.setCode("LK-W-" + System.nanoTime());
        locker.setLocation("D9 Building");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.save(locker);

        compartment = new Compartment();
        compartment.setLocker(locker);
        compartment.setCode("W-01-" + System.nanoTime());
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartment = compartmentRepository.save(compartment);
    }

    @Test
    void testCreditOrderPayoutOnPurchaseCompletion() {
        Product product = new Product();
        product.setTitle("Calculus Book");
        product.setDescription("Calculus 1");
        product.setPrice(new BigDecimal("200000.00"));
        product.setCategory(category);
        product.setOwner(seller);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        Order order = new Order();
        order.setOrderNumber("ORD-W-P-" + System.nanoTime());
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setPriceSnapshot(product.getPrice());
        order.setPlatformFee(new BigDecimal("10000.00"));
        order.setSellerPayout(new BigDecimal("190000.00"));
        order.setOrderType(OrderType.PURCHASE);
        order.setStatus(OrderStatus.PENDING_SELLER);
        order.setPaymentStatus(PaymentStatus.PAID);
        order = orderRepository.save(order);

        // Generate and record pickup code for testing
        orderService.acceptOrder(order.getId(), null, seller.getUsername());
        orderService.depositOrder(order.getId(), seller.getUsername());
        String pin = orderService.getRawPickupCodeForTesting(order.getId());

        OrderPickupRequest pickupReq = new OrderPickupRequest();
        pickupReq.setPickupCode(pin);

        // Buyer picks up
        orderService.pickupOrder(order.getId(), pickupReq, buyer.getUsername());

        // Verify order is completed
        Order completed = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, completed.getStatus());

        // Verify seller wallet is credited with exact payout
        Wallet sellerWallet = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("190000.00").compareTo(sellerWallet.getBalance()));

        // Verify transaction is recorded
        assertTrue(walletTransactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.ORDER_PAYOUT));

        // Verify idempotency: calling credit again does not double-credit
        walletService.creditOrderPayout(completed);
        sellerWallet = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("190000.00").compareTo(sellerWallet.getBalance()));
    }

    @Test
    @WithMockUser(username = "wallet_seller")
    void testWalletApiAndWithdrawalLifecycle() throws Exception {
        // Ensure seller has wallet with balance
        Wallet wallet = walletService.getOrCreateWallet(seller.getId());
        wallet.setBalance(new BigDecimal("300000.00"));
        walletRepository.save(wallet);

        // 1. GET /api/v1/wallet/me
        mockMvc.perform(get("/api/v1/wallet/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("wallet_seller"))
                .andExpect(jsonPath("$.balance").value(300000.0));

        // 2. Add bank account: POST /api/v1/wallet/bank-accounts
        UserBankAccountRequest bankReq = new UserBankAccountRequest();
        bankReq.setBankCode("VCB");
        bankReq.setBankName("Vietcombank");
        bankReq.setAccountNumber("0123456789");
        bankReq.setAccountName("NGUYEN VAN A");
        bankReq.setDefault(true);

        mockMvc.perform(post("/api/v1/wallet/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankCode").value("VCB"))
                .andExpect(jsonPath("$.accountNumber").value("0123456789"));

        // 3. Request withdrawal: POST /api/v1/wallet/withdraw (100,000 VND)
        WithdrawalRequestDto withdrawReq = new WithdrawalRequestDto();
        withdrawReq.setAmount(new BigDecimal("100000.00"));

        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100000.0))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Check wallet balances: balance should be 200,000, frozenBalance should be 100,000
        Wallet updatedWallet = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("200000.00").compareTo(updatedWallet.getBalance()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(updatedWallet.getFrozenBalance()));

        // 4. Test withdrawal below minimum (5,000 VND < 10,000 VND)
        WithdrawalRequestDto tooLow = new WithdrawalRequestDto();
        tooLow.setAmount(new BigDecimal("5000.00"));
        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLow)))
                .andExpect(status().isBadRequest());

        // 5. Test withdrawal exceeding balance (500,000 VND > 200,000 VND)
        WithdrawalRequestDto excessive = new WithdrawalRequestDto();
        excessive.setAmount(new BigDecimal("500000.00"));
        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessive)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_WALLET_BALANCE"));
    }

    @Test
    @WithMockUser(username = "wallet_admin", roles = {"ADMIN"})
    void testAdminApproveAndRejectPayout() throws Exception {
        // Prepare pending payout for seller
        Wallet wallet = walletService.getOrCreateWallet(seller.getId());
        wallet.setBalance(new BigDecimal("150000.00"));
        wallet.setFrozenBalance(new BigDecimal("50000.00"));
        walletRepository.save(wallet);

        PayoutRequest payout = new PayoutRequest();
        payout.setUser(seller);
        payout.setAmount(new BigDecimal("50000.00"));
        payout.setBankCode("MB");
        payout.setAccountNumber("99998888");
        payout.setAccountName("WALLET SELLER");
        payout.setStatus(PayoutStatus.PENDING);
        payout = payoutRequestRepository.save(payout);

        // 1. Admin views pending payouts
        mockMvc.perform(get("/api/v1/admin/payouts?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 2. Admin approves payout
        AdminPayoutActionRequest approveReq = new AdminPayoutActionRequest();
        approveReq.setReferenceCode("MB-FT-12345678");
        approveReq.setNote("Chuyển khoản thành công");

        mockMvc.perform(post("/api/v1/admin/payouts/" + payout.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.referenceCode").value("MB-FT-12345678"));

        Wallet afterApproval = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(afterApproval.getFrozenBalance()));
        assertEquals(0, new BigDecimal("150000.00").compareTo(afterApproval.getBalance()));

        // 3. Test Admin rejection flow with another request
        afterApproval.setFrozenBalance(new BigDecimal("30000.00"));
        walletRepository.save(afterApproval);

        PayoutRequest rejectTarget = new PayoutRequest();
        rejectTarget.setUser(seller);
        rejectTarget.setAmount(new BigDecimal("30000.00"));
        rejectTarget.setBankCode("MB");
        rejectTarget.setAccountNumber("99998888");
        rejectTarget.setAccountName("WALLET SELLER");
        rejectTarget.setStatus(PayoutStatus.PENDING);
        rejectTarget = payoutRequestRepository.save(rejectTarget);

        AdminPayoutActionRequest rejectReq = new AdminPayoutActionRequest();
        rejectReq.setNote("Sai so tai khoan ngan hang");

        mockMvc.perform(post("/api/v1/admin/payouts/" + rejectTarget.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        Wallet afterRejection = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(afterRejection.getFrozenBalance()));
        // The 30,000 VND should have been returned to available balance (150,000 + 30,000 = 180,000)
        assertEquals(0, new BigDecimal("180000.00").compareTo(afterRejection.getBalance()));
    }

    @Test
    void testCreditOrderPayoutAndDepositRefundOnRentalReturn() {
        Product rentalProduct = new Product();
        rentalProduct.setTitle("Calculus Book Rental");
        rentalProduct.setDescription("Rental");
        rentalProduct.setPrice(new BigDecimal("50000.00"));
        rentalProduct.setCategory(category);
        rentalProduct.setOwner(seller);
        rentalProduct.setStatus(ProductStatus.ACTIVE);
        rentalProduct = productRepository.save(rentalProduct);

        Order rentalOrder = new Order();
        rentalOrder.setOrderNumber("ORD-W-R-" + System.nanoTime());
        rentalOrder.setBuyer(buyer);
        rentalOrder.setSeller(seller);
        rentalOrder.setProduct(rentalProduct);
        rentalOrder.setPriceSnapshot(new BigDecimal("100000.00"));
        rentalOrder.setDepositAmount(new BigDecimal("200000.00"));
        rentalOrder.setPlatformFee(new BigDecimal("10000.00"));
        rentalOrder.setSellerPayout(new BigDecimal("90000.00"));
        rentalOrder.setOrderType(OrderType.RENTAL);
        rentalOrder.setStatus(OrderStatus.RENTING);
        rentalOrder.setPaymentStatus(PaymentStatus.PAID);
        rentalOrder.setDueReturnAt(java.time.LocalDateTime.now().plusDays(2));
        rentalOrder = orderRepository.save(rentalOrder);

        // Ensure compartment is available for return
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartment = compartmentRepository.save(compartment);

        // Buyer initiates return
        var returnReq = new com.campushub.modules.order.dto.OrderReturnRequest();
        returnReq.setLockerId(locker.getId());
        returnReq.setCompartmentId(compartment.getId());
        orderService.initiateReturn(rentalOrder.getId(), returnReq, buyer.getUsername());

        // Buyer deposits return in locker
        orderService.depositReturn(rentalOrder.getId(), buyer.getUsername());
        String sellerPickupCode = orderService.getOrder(rentalOrder.getId(), seller.getUsername()).getPickupCode();

        // Record initial wallet balances
        BigDecimal sellerInitialBalance = walletRepository.findByUserId(seller.getId())
                .map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal buyerInitialBalance = walletRepository.findByUserId(buyer.getId())
                .map(Wallet::getBalance).orElse(BigDecimal.ZERO);

        // Seller picks up returned item
        OrderPickupRequest sellerPickupReq = new OrderPickupRequest();
        sellerPickupReq.setPickupCode(sellerPickupCode);
        orderService.pickupReturn(rentalOrder.getId(), sellerPickupReq, seller.getUsername());

        // Check order completed
        Order completedRental = orderRepository.findById(rentalOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, completedRental.getStatus());

        // Verify seller received payout (90,000 VND)
        Wallet sellerWallet = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, sellerInitialBalance.add(new BigDecimal("90000.00")).compareTo(sellerWallet.getBalance()));
        assertTrue(walletTransactionRepository.existsByOrderIdAndType(rentalOrder.getId(), TransactionType.ORDER_PAYOUT));

        // Verify buyer received deposit refund (200,000 VND)
        Wallet buyerWallet = walletRepository.findByUserId(buyer.getId()).orElseThrow();
        assertEquals(0, buyerInitialBalance.add(new BigDecimal("200000.00")).compareTo(buyerWallet.getBalance()));
        assertTrue(walletTransactionRepository.existsByOrderIdAndType(rentalOrder.getId(), TransactionType.DEPOSIT_REFUND));
    }

    @Test
    @WithMockUser(username = "wallet_buyer", roles = {"STUDENT"})
    void testNonAdminCannotApprovePayout() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payouts/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
