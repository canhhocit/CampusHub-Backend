package com.campushub.modules.order;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.dispute.dto.DisputeCreateRequest;
import com.campushub.modules.dispute.dto.DisputeResolveRequest;
import com.campushub.modules.dispute.dto.DisputeResponse;
import com.campushub.modules.dispute.entity.OrderDispute;
import com.campushub.modules.dispute.enums.DisputeReason;
import com.campushub.modules.dispute.enums.DisputeStatus;
import com.campushub.modules.dispute.repository.OrderDisputeRepository;
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
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.repository.OrderRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DisputeIsolationIntegrationTest {

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
    private OrderDisputeRepository disputeRepository;

    private User seller;
    private User buyer;
    private User admin;
    private Locker locker;
    private Compartment compartment;
    private Product product;

    @BeforeEach
    public void setup() {
        if (!userRepository.existsByUsername("seller_dispute_test")) {
            seller = new User();
            seller.setUsername("seller_dispute_test");
            seller.setEmail("seller_dispute@test.com");
            seller.setPasswordHash("hash");
            seller.setFullName("Seller Dispute Test");
            seller.setRole(Role.STUDENT);
            seller.setStatus(UserStatus.ACTIVE);
            seller.setIdentityVerified(true);
            seller = userRepository.save(seller);
        } else {
            seller = userRepository.findByUsername("seller_dispute_test").get();
        }

        if (!userRepository.existsByUsername("buyer_dispute_test")) {
            buyer = new User();
            buyer.setUsername("buyer_dispute_test");
            buyer.setEmail("buyer_dispute@test.com");
            buyer.setPasswordHash("hash");
            buyer.setFullName("Buyer Dispute Test");
            buyer.setRole(Role.STUDENT);
            buyer.setStatus(UserStatus.ACTIVE);
            buyer.setIdentityVerified(true);
            buyer = userRepository.save(buyer);
        } else {
            buyer = userRepository.findByUsername("buyer_dispute_test").get();
        }

        if (!userRepository.existsByUsername("admin_dispute_test")) {
            admin = new User();
            admin.setUsername("admin_dispute_test");
            admin.setEmail("admin_dispute@test.com");
            admin.setPasswordHash("hash");
            admin.setFullName("Admin Dispute Test");
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin = userRepository.save(admin);
        } else {
            admin = userRepository.findByUsername("admin_dispute_test").get();
        }

        List<Category> cats = categoryRepository.findByIsActiveTrueOrderByNameAsc();
        Category cat = cats.isEmpty() ? null : cats.get(0);
        if (cat == null) {
            cat = new Category();
            cat.setName("Dispute Category " + System.nanoTime());
            cat.setSlug("cat-dispute-" + System.nanoTime());
            cat.setActive(true);
            cat = categoryRepository.save(cat);
        }

        locker = new Locker();
        locker.setCode("LCK_DISP_" + System.nanoTime());
        locker.setName("Dispute Isolation Locker");
        locker.setLocation("Building D");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.save(locker);

        compartment = new Compartment();
        compartment.setLocker(locker);
        compartment.setCode("D101");
        compartment.setStatus(CompartmentStatus.AVAILABLE);
        compartment = compartmentRepository.save(compartment);

        product = new Product();
        product.setOwner(seller);
        product.setCategory(cat);
        product.setTitle("Dispute Test Item");
        product.setDescription("Item description");
        product.setPrice(BigDecimal.valueOf(100000));
        product.setStatus(ProductStatus.ACTIVE);
        product.setLocker(locker);
        product = productRepository.save(product);
    }

    private Order createTestOrder(OrderStatus orderStatus) {
        Order order = new Order();
        order.setOrderNumber("ORD-DISP-" + System.nanoTime());
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setLocker(locker);
        order.setCompartment(compartment);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(orderStatus);
        return orderRepository.save(order);
    }

    @Test
    @WithMockUser(username = "buyer_dispute_test", roles = {"STUDENT"})
    public void testEmptyCompartmentDisputeAutoIsolatesCompartment() throws Exception {
        Order order = createTestOrder(OrderStatus.READY_FOR_PICKUP);

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason(DisputeReason.EMPTY_COMPARTMENT);
        request.setDescription("Mo tu ra khong co hang ben trong.");

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/disputes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("EMPTY_COMPARTMENT"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.isCompartmentIsolated").value(true))
                .andExpect(jsonPath("$.compartmentCode").value("D101"));

        // Compartment in DB must be ERROR (isolated)
        Compartment updatedCompartment = compartmentRepository.findById(compartment.getId()).orElseThrow();
        assertEquals(CompartmentStatus.ERROR, updatedCompartment.getStatus(),
                "Compartment should be automatically isolated with status ERROR");
    }

    @Test
    @WithMockUser(username = "admin_dispute_test", roles = {"ADMIN"})
    public void testAdminResolvesDisputeAndReleasesCompartment() throws Exception {
        Order order = createTestOrder(OrderStatus.READY_FOR_PICKUP);

        // Manually isolate compartment as if EMPTY_COMPARTMENT happened
        compartment.setStatus(CompartmentStatus.ERROR);
        compartmentRepository.save(compartment);

        OrderDispute dispute = new OrderDispute();
        dispute.setOrder(order);
        dispute.setCreator(buyer);
        dispute.setReason(DisputeReason.EMPTY_COMPARTMENT);
        dispute.setDescription("Tu rong khong co hang.");
        dispute.setStatus(DisputeStatus.OPEN);
        dispute = disputeRepository.save(dispute);

        // Admin resolves with releaseCompartment = true
        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution(DisputeStatus.RESOLVED_REFUND_BUYER);
        resolveRequest.setAdminNote("Xac nhan tu rong. Hoan tien nguoi mua va giai phong tu.");
        resolveRequest.setReleaseCompartment(true);

        mockMvc.perform(post("/api/v1/admin/disputes/" + dispute.getId() + "/resolve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_REFUND_BUYER"))
                .andExpect(jsonPath("$.isCompartmentIsolated").value(false));

        // Compartment in DB must be AVAILABLE
        Compartment releasedCompartment = compartmentRepository.findById(compartment.getId()).orElseThrow();
        assertEquals(CompartmentStatus.AVAILABLE, releasedCompartment.getStatus(),
                "Compartment should be released back to AVAILABLE");
    }

    @Test
    @WithMockUser(username = "admin_dispute_test", roles = {"ADMIN"})
    public void testAdminResolvesDisputeWithoutReleasingCompartmentKeepsIsolation() throws Exception {
        Order order = createTestOrder(OrderStatus.READY_FOR_PICKUP);

        // Compartment is isolated
        compartment.setStatus(CompartmentStatus.ERROR);
        compartmentRepository.save(compartment);

        OrderDispute dispute = new OrderDispute();
        dispute.setOrder(order);
        dispute.setCreator(buyer);
        dispute.setReason(DisputeReason.EMPTY_COMPARTMENT);
        dispute.setDescription("Tu rong.");
        dispute.setStatus(DisputeStatus.OPEN);
        dispute = disputeRepository.save(dispute);

        // Admin resolves with releaseCompartment = false
        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution(DisputeStatus.RESOLVED_REFUND_BUYER);
        resolveRequest.setAdminNote("Hoan tien nguoi mua nhung can ky thuat kiem tra o khoa.");
        resolveRequest.setReleaseCompartment(false);

        mockMvc.perform(post("/api/v1/admin/disputes/" + dispute.getId() + "/resolve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_REFUND_BUYER"))
                .andExpect(jsonPath("$.isCompartmentIsolated").value(true));

        // Compartment in DB remains ERROR
        Compartment retainedCompartment = compartmentRepository.findById(compartment.getId()).orElseThrow();
        assertEquals(CompartmentStatus.ERROR, retainedCompartment.getStatus(),
                "Compartment should remain isolated in ERROR status");
    }
}
