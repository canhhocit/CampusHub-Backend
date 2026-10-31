package com.campushub.modules.locker;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.redis.RedisResilientService;
import com.campushub.modules.locker.dto.KioskPickupRequest;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.locker.service.KioskRateLimitService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KioskRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LockerRepository lockerRepository;

    @Autowired
    private CompartmentRepository compartmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KioskRateLimitService rateLimitService;

    @Autowired
    private RedisResilientService redisResilientService;

    private Locker locker;
    private Order order;

    @BeforeEach
    public void setup() {
        redisResilientService.clearMemoryStore();

        User seller = userRepository.findByUsername("seller_kiosk_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("seller_kiosk_test");
            u.setEmail("seller_kiosk@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Seller Kiosk");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        User buyer = userRepository.findByUsername("buyer_kiosk_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("buyer_kiosk_test");
            u.setEmail("buyer_kiosk@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Buyer Kiosk");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        locker = new Locker();
        locker.setCode("LCK_RL_" + System.nanoTime());
        locker.setName("Rate Limit Locker");
        locker.setLocation("Hall A");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.save(locker);

        Compartment comp = new Compartment();
        comp.setLocker(locker);
        comp.setCode("A01");
        comp.setStatus(CompartmentStatus.OCCUPIED);
        comp = compartmentRepository.save(comp);

        List<Category> cats = categoryRepository.findByIsActiveTrueOrderByNameAsc();
        Category cat = cats.isEmpty() ? null : cats.get(0);
        if (cat == null) {
            cat = new Category();
            cat.setName("Kiosk Cat " + System.nanoTime());
            cat.setSlug("kiosk-cat-" + System.nanoTime());
            cat.setActive(true);
            cat = categoryRepository.save(cat);
        }

        Product product = new Product();
        product.setTitle("Kiosk Item");
        product.setDescription("Desc");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setCategory(cat);
        product.setOwner(seller);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        order = new Order();
        order.setOrderNumber("ORD-KIOSK-" + System.nanoTime());
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setLocker(locker);
        order.setCompartment(comp);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setPickupCodeHash(passwordEncoder.encode("123456"));
        order = orderRepository.save(order);
    }

    @Test
    public void testExceededFailedAttemptsTriggersRateLimit() throws Exception {
        KioskPickupRequest wrongReq = new KioskPickupRequest();
        wrongReq.setPinCode("999999"); // Wrong PIN

        // Send 5 failed attempts
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/kiosk/lockers/" + locker.getId() + "/pickup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ErrorCode.INVALID_PICKUP_CODE.name()));
        }

        // 6th attempt should be blocked by Rate Limiter (HTTP 429 TOO_MANY_REQUESTS)
        mockMvc.perform(post("/api/v1/kiosk/lockers/" + locker.getId() + "/pickup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value(ErrorCode.KIOSK_RATE_LIMIT_EXCEEDED.name()));
    }

    @Test
    public void testValidPinResetsRateLimit() throws Exception {
        KioskPickupRequest wrongReq = new KioskPickupRequest();
        wrongReq.setPinCode("999999");

        // Send 2 failed attempts
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/kiosk/lockers/" + locker.getId() + "/pickup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isBadRequest());
        }

        // Send valid PIN ("123456") -> Success & Resets counter
        KioskPickupRequest validReq = new KioskPickupRequest();
        validReq.setPinCode("123456");

        mockMvc.perform(post("/api/v1/kiosk/lockers/" + locker.getId() + "/pickup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

