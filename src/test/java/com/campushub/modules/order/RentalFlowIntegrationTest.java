package com.campushub.modules.order;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.admin.dto.UpdateFeeConfigRequest;
import com.campushub.modules.admin.entity.FeeMode;
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
import com.campushub.modules.order.dto.OrderAcceptRequest;
import com.campushub.modules.order.dto.OrderCreateRequest;
import com.campushub.modules.order.dto.OrderPickupRequest;
import com.campushub.modules.order.dto.OrderReturnRequest;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.OrderType;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.order.service.OrderService;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RentalFlowIntegrationTest {

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

    private User admin;
    private User seller;
    private User buyer;
    private Category category;
    private Locker locker;
    private Compartment compartment1;
    private Compartment compartment2;
    private Product rentalProduct;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin_user");
        admin.setPasswordHash("hashed");
        admin.setEmail("admin@campushub.vn");
        admin.setFullName("Admin User");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        seller = new User();
        seller.setUsername("seller_rental");
        seller.setPasswordHash("hashed");
        seller.setEmail("seller_rental@campushub.vn");
        seller.setFullName("Seller Rental");
        seller.setRole(Role.USER);
        seller.setStatus(UserStatus.ACTIVE);
        seller.setIdentityVerified(true);
        seller.setIdentityReviewedAt(LocalDateTime.now());
        seller = userRepository.save(seller);

        buyer = new User();
        buyer.setUsername("buyer_rental");
        buyer.setPasswordHash("hashed");
        buyer.setEmail("buyer_rental@campushub.vn");
        buyer.setFullName("Buyer Rental");
        buyer.setRole(Role.USER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setIdentityVerified(true);
        buyer.setIdentityReviewedAt(LocalDateTime.now());
        buyer = userRepository.save(buyer);

        category = new Category();
        category.setName("Rental Gadgets " + System.currentTimeMillis());
        category.setSlug("rental-gadgets-" + System.currentTimeMillis());
        category.setActive(true);
        category = categoryRepository.save(category);

        locker = new Locker();
        locker.setCode("LK-RENTAL-" + System.nanoTime());
        locker.setName("Rental Smart Locker");
        locker.setLocation("Library Floor 1");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.save(locker);

        compartment1 = new Compartment();
        compartment1.setLocker(locker);
        compartment1.setCode("C-01");
        compartment1.setStatus(CompartmentStatus.AVAILABLE);
        compartment1 = compartmentRepository.save(compartment1);

        compartment2 = new Compartment();
        compartment2.setLocker(locker);
        compartment2.setCode("C-02");
        compartment2.setStatus(CompartmentStatus.AVAILABLE);
        compartment2 = compartmentRepository.save(compartment2);

        rentalProduct = new Product();
        rentalProduct.setTitle("Canon EOS 70D Camera for Rent");
        rentalProduct.setDescription("Professional DSLR camera with lens");
        rentalProduct.setPrice(new BigDecimal("10000000.00"));
        rentalProduct.setListingType(ListingType.BOTH);
        rentalProduct.setRentPricePerDay(new BigDecimal("50000.00"));
        rentalProduct.setDepositAmount(new BigDecimal("500000.00"));
        rentalProduct.setStatus(ProductStatus.ACTIVE);
        rentalProduct.setCategory(category);
        rentalProduct.setOwner(seller);
        rentalProduct = productRepository.save(rentalProduct);
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testAdminFeeSettingsCrud() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleCommissionPercent").exists())
                .andExpect(jsonPath("$.rentalFeeMode").exists());

        UpdateFeeConfigRequest updateReq = new UpdateFeeConfigRequest();
        updateReq.setSaleCommissionPercent(new BigDecimal("5.00"));
        updateReq.setRentalFeeMode(FeeMode.PERCENT);
        updateReq.setRentalFeeValue(new BigDecimal("10.00"));

        mockMvc.perform(put("/api/v1/admin/settings/fees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleCommissionPercent").value(5.00))
                .andExpect(jsonPath("$.rentalFeeValue").value(10.00))
                .andExpect(jsonPath("$.rentalFeeMode").value("PERCENT"));
    }

    @Test
    @WithMockUser(username = "buyer_rental", roles = {"USER"})
    void testCompleteRentalLifecycle() throws Exception {
        // 1. Buyer creates rental order for 3 days
        OrderCreateRequest orderReq = new OrderCreateRequest();
        orderReq.setProductId(rentalProduct.getId());
        orderReq.setOrderType(OrderType.RENTAL);
        orderReq.setRentalDurationDays(3);

        MvcResult createRes = mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderType").value("RENTAL"))
                .andExpect(jsonPath("$.rentalDurationDays").value(3))
                .andExpect(jsonPath("$.depositAmount").value(500000.00))
                .andExpect(jsonPath("$.priceSnapshot").value(150000.00)) // 3 * 50k
                .andExpect(jsonPath("$.totalAmount").value(650000.00))   // 150k + 500k deposit
                .andReturn();

        Long orderId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Seller accepts order and reserves a compartment in locker
        OrderAcceptRequest acceptReq = new OrderAcceptRequest();
        acceptReq.setLockerId(locker.getId());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/accept")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller_rental").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_DEPOSIT"));

        Order orderAfterAccept = orderRepository.findById(orderId).orElseThrow();
        Long assignedCompartmentId = orderAfterAccept.getCompartment().getId();

        // 3. Seller deposits item
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deposit")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller_rental").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_PICKUP"));

        // 4. Buyer picks up the rental item
        String pickupCode = orderService.getRawPickupCodeForTesting(orderId);
        assertNotNull(pickupCode);

        OrderPickupRequest pickupReq = new OrderPickupRequest();
        pickupReq.setPickupCode(pickupCode);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pickup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pickupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENTING"))
                .andExpect(jsonPath("$.dueReturnAt").isNotEmpty());

        // Check assigned compartment is freed and product is RENTED
        Compartment cAssigned = compartmentRepository.findById(assignedCompartmentId).orElseThrow();
        assertEquals(CompartmentStatus.AVAILABLE, cAssigned.getStatus());

        Product prodRented = productRepository.findById(rentalProduct.getId()).orElseThrow();
        assertEquals(ProductStatus.RENTED, prodRented.getStatus());

        // 5. Buyer initiates return into the other compartment
        Long returnCompartmentId = assignedCompartmentId.equals(compartment1.getId()) ? compartment2.getId() : compartment1.getId();
        OrderReturnRequest returnReq = new OrderReturnRequest();
        returnReq.setLockerId(locker.getId());
        returnReq.setCompartmentId(returnCompartmentId);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/initiate-return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_RETURN_DEPOSIT"));

        Compartment cReturn = compartmentRepository.findById(returnCompartmentId).orElseThrow();
        assertEquals(CompartmentStatus.RESERVED, cReturn.getStatus());

        // 6. Buyer deposits item back into return compartment
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deposit-return")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RETURN_PICKUP"));

        // 7. Seller gets order details and views return pickup PIN
        MvcResult sellerDetailRes = mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller_rental").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RETURN_PICKUP"))
                .andExpect(jsonPath("$.pickupCode").isNotEmpty())
                .andReturn();

        String returnPickupCode = objectMapper.readTree(sellerDetailRes.getResponse().getContentAsString()).get("pickupCode").asText();

        // 8. Seller picks up returned item
        OrderPickupRequest returnPickupReq = new OrderPickupRequest();
        returnPickupReq.setPickupCode(returnPickupCode);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pickup-return")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller_rental").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnPickupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify product is ACTIVE again and return compartment is AVAILABLE
        Compartment cReturnFinal = compartmentRepository.findById(returnCompartmentId).orElseThrow();
        assertEquals(CompartmentStatus.AVAILABLE, cReturnFinal.getStatus());

        Product prodActive = productRepository.findById(rentalProduct.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, prodActive.getStatus());
    }
}
