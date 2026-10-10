package com.campushub.modules.order;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.dto.OrderAcceptRequest;
import com.campushub.modules.order.dto.OrderCreateRequest;
import com.campushub.modules.order.dto.OrderPickupRequest;
import com.campushub.modules.order.dto.OrderRejectRequest;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderIntegrationTest {

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

    private User seller;
    private User buyer;
    private User otherUser;
    private Category category;
    private Locker locker;
    private Compartment compartment;
    private Product product;

    @BeforeEach
    public void setup() {
        if (!userRepository.existsByUsername("seller_order_test")) {
            seller = new User();
            seller.setUsername("seller_order_test");
            seller.setEmail("seller_order@test.com");
            seller.setPasswordHash("hash");
            seller.setFullName("Seller Test");
            seller.setRole(Role.STUDENT);
            seller.setStatus(UserStatus.ACTIVE);
            seller = userRepository.save(seller);
        } else {
            seller = userRepository.findByUsername("seller_order_test").get();
        }

        if (!userRepository.existsByUsername("buyer_order_test")) {
            buyer = new User();
            buyer.setUsername("buyer_order_test");
            buyer.setEmail("buyer_order@test.com");
            buyer.setPasswordHash("hash");
            buyer.setFullName("Buyer Test");
            buyer.setRole(Role.STUDENT);
            buyer.setStatus(UserStatus.ACTIVE);
            buyer = userRepository.save(buyer);
        } else {
            buyer = userRepository.findByUsername("buyer_order_test").get();
        }

        if (!userRepository.existsByUsername("other_order_test")) {
            otherUser = new User();
            otherUser.setUsername("other_order_test");
            otherUser.setEmail("other_order@test.com");
            otherUser.setPasswordHash("hash");
            otherUser.setFullName("Other User");
            otherUser.setRole(Role.STUDENT);
            otherUser.setStatus(UserStatus.ACTIVE);
            otherUser = userRepository.save(otherUser);
        } else {
            otherUser = userRepository.findByUsername("other_order_test").get();
        }

        List<Category> activeCategories = categoryRepository.findByIsActiveTrueOrderByNameAsc();
        if (!activeCategories.isEmpty()) {
            category = activeCategories.get(0);
        } else {
            category = new Category();
            category.setName("Order Test Category " + System.nanoTime());
            category.setSlug("cat-order-test-" + System.nanoTime());
            category.setActive(true);
            category = categoryRepository.save(category);
        }

        locker = new Locker();
        locker.setCode("LK-ORD-" + System.nanoTime());
        locker.setName("Order Test Locker");
        locker.setLocation("Campus Station");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.save(locker);

        compartment = new Compartment();
        compartment.setLocker(locker);
        compartment.setCode("CP-01");
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartment = compartmentRepository.save(compartment);

        product = new Product();
        product.setTitle("Calculus Textbook");
        product.setDescription("Calculus 1 & 2 Textbook in good condition");
        product.setPrice(new BigDecimal("150000.00"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product.setOwner(seller);
        product = productRepository.save(product);
    }

    @Test
    public void testAnonymousCannotAccessOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/buying"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/orders/selling"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "buyer_order_test", roles = "STUDENT")
    public void testBuyerCreateOrderSuccess() throws Exception {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductId(product.getId());

        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING_SELLER"))
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.priceSnapshot").value(150000.00))
                .andExpect(jsonPath("$.buyerUsername").value("buyer_order_test"))
                .andExpect(jsonPath("$.sellerUsername").value("seller_order_test"));
    }

    @Test
    @WithMockUser(username = "seller_order_test", roles = "STUDENT")
    public void testCannotOrderOwnProduct() throws Exception {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductId(product.getId());

        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_ORDER_OWN_PRODUCT"));
    }

    @Test
    @WithMockUser(username = "buyer_order_test", roles = "STUDENT")
    public void testCannotOrderNonActiveProduct() throws Exception {
        product.setStatus(ProductStatus.SOLD);
        productRepository.saveAndFlush(product);

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductId(product.getId());

        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_AVAILABLE_FOR_ORDER"));
    }

    @Test
    @WithMockUser(username = "buyer_order_test", roles = "STUDENT")
    public void testDuplicateActiveOrderRejected() throws Exception {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductId(product.getId());

        // First order
        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second order for same product
        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACTIVE_ORDER_EXISTS"));
    }

    @Test
    @WithMockUser(username = "seller_order_test", roles = "STUDENT")
    public void testSellerAcceptOrderAllocatesCompartment() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-ACCEPT");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        OrderAcceptRequest acceptReq = new OrderAcceptRequest();
        acceptReq.setLockerId(locker.getId());

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/accept").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_DEPOSIT"))
                .andExpect(jsonPath("$.lockerId").value(locker.getId()))
                .andExpect(jsonPath("$.compartmentCode").value("CP-01"));

        Compartment updatedCompartment = compartmentRepository.findById(compartment.getId()).get();
        assertEquals(CompartmentStatus.RESERVED, updatedCompartment.getStatus());
    }

    @Test
    @WithMockUser(username = "other_order_test", roles = "STUDENT")
    public void testNonSellerCannotAcceptOrder() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-FORBIDDEN");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/accept").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
    }

    @Test
    @WithMockUser(username = "seller_order_test", roles = "STUDENT")
    public void testSellerRejectOrder() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-REJECT");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        OrderRejectRequest rejectReq = new OrderRejectRequest();
        rejectReq.setReason("Out of stock or unavailable");

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/reject").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "buyer_order_test", roles = "STUDENT")
    public void testBuyerCancelOrder() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-CANCEL");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    public void testCompleteDepositAndPickupFlow() throws Exception {
        // 1. Create order
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-FLOW-" + System.currentTimeMillis());
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        // 2. Seller accepts order
        OrderAcceptRequest acceptReq = new OrderAcceptRequest();
        acceptReq.setLockerId(locker.getId());
        orderService.acceptOrder(order.getId(), acceptReq, "seller_order_test");

        // 3. Seller deposits item
        orderService.depositOrder(order.getId(), "seller_order_test");

        // Verify compartment is OCCUPIED
        Compartment occComp = compartmentRepository.findById(compartment.getId()).get();
        assertEquals(CompartmentStatus.OCCUPIED, occComp.getStatus());

        // 4. Buyer views order detail and receives pickup code
        String rawPickupCode = orderService.getRawPickupCodeForTesting(order.getId());
        assertNotNull(rawPickupCode);
        assertEquals(6, rawPickupCode.length());

        // 5. Buyer picks up item with code
        OrderPickupRequest pickupReq = new OrderPickupRequest();
        pickupReq.setPickupCode(rawPickupCode);
        orderService.pickupOrder(order.getId(), pickupReq, "buyer_order_test");

        // Verify final statuses
        Order completedOrder = orderRepository.findById(order.getId()).get();
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());

        Compartment freedComp = compartmentRepository.findById(compartment.getId()).get();
        assertEquals(CompartmentStatus.AVAILABLE, freedComp.getStatus());

        Product soldProduct = productRepository.findById(product.getId()).get();
        assertEquals(ProductStatus.SOLD, soldProduct.getStatus());
    }

    @Test
    public void testNoAvailableCompartmentFailsGracefully() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-NO-COMPARTMENT");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        // Make compartment occupied so none is available
        compartment.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.saveAndFlush(compartment);

        OrderAcceptRequest acceptReq = new OrderAcceptRequest();
        acceptReq.setLockerId(locker.getId());

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/accept").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NO_AVAILABLE_COMPARTMENT"));
    }

    @Test
    public void testDoublePickupRejected() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-DOUBLE-PICKUP-" + System.currentTimeMillis());
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.PENDING_SELLER);
        order = orderRepository.saveAndFlush(order);

        orderService.acceptOrder(order.getId(), null, "seller_order_test");
        orderService.depositOrder(order.getId(), "seller_order_test");

        String rawCode = orderService.getRawPickupCodeForTesting(order.getId());
        OrderPickupRequest pickupReq = new OrderPickupRequest();
        pickupReq.setPickupCode(rawCode);

        // First pickup -> Success
        orderService.pickupOrder(order.getId(), pickupReq, "buyer_order_test");

        // Second pickup attempt -> Rejected
        try {
            orderService.pickupOrder(order.getId(), pickupReq, "buyer_order_test");
        } catch (Exception e) {
            assertEquals("INVALID_ORDER_STATUS", e.getMessage());
        }
    }
}
