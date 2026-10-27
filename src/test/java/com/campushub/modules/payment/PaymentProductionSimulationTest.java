package com.campushub.modules.payment;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.TestPropertySource;

@ActiveProfiles({"test", "prod"})
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "PICKUP_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef",
    "JWT_SECRET=test_production_secret_key_at_least_32_characters_long"
})
public class PaymentProductionSimulationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User buyer;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = userRepository.findByUsername("buyer_prod_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("buyer_prod_test");
            u.setEmail("buyer_prod_test@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Buyer Prod");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        User seller = userRepository.findByUsername("seller_prod_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("seller_prod_test");
            u.setEmail("seller_prod_test@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Seller Prod");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        Category category = new Category();
        category.setName("Category Prod " + System.nanoTime());
        category.setSlug("category-prod-" + System.nanoTime());
        category.setActive(true);
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setTitle("Product Prod");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("200000"));
        product.setCategory(category);
        product.setOwner(seller);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        order = new Order();
        order.setOrderNumber("ORD-PROD-" + System.nanoTime());
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.AWAITING_DEPOSIT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order = orderRepository.save(order);
    }

    @Test
    @WithMockUser(username = "buyer_prod_test")
    void testSimulationEndpointBlockedInProductionProfile() throws Exception {
        // Attempting to simulate PayOS success in production profile must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay/simulate-success"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SIMULATION_NOT_ALLOWED"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.UNPAID, updated.getPaymentStatus());
    }
}
