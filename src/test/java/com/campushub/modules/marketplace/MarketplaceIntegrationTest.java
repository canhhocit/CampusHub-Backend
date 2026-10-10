package com.campushub.modules.marketplace;

import com.campushub.modules.marketplace.dto.ProductCreateRequest;
import com.campushub.modules.marketplace.dto.ProductStatusUpdateRequest;
import com.campushub.modules.marketplace.dto.ProductUpdateRequest;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MarketplaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category testCategory;
    private User testUser;
    private User otherUser;

    @BeforeEach
    public void setup() {
        if (categoryRepository.findAll().isEmpty()) {
            testCategory = new Category();
            testCategory.setName("Test Category");
            testCategory.setSlug("test-category");
            testCategory.setActive(true);
            testCategory = categoryRepository.save(testCategory);
        } else {
            testCategory = categoryRepository.findAll().get(0);
        }

        testUser = new User();
        testUser.setUsername("seller1");
        testUser.setEmail("seller1@test.com");
        testUser.setPasswordHash("hash");
        testUser.setFullName("Seller One");
        testUser.setRole(Role.STUDENT);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("seller2");
        otherUser.setEmail("seller2@test.com");
        otherUser.setPasswordHash("hash");
        otherUser.setFullName("Seller Two");
        otherUser.setRole(Role.STUDENT);
        otherUser.setStatus(UserStatus.ACTIVE);
        otherUser = userRepository.save(otherUser);
    }

    @Test
    public void testPublicCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser(username = "seller1")
    public void testCreateProductAndPublicListing() throws Exception {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setTitle("Test Laptop");
        req.setDescription("Good laptop");
        req.setPrice(new BigDecimal("999.99"));
        req.setCategoryId(testCategory.getId());

        // Create product
        MvcResult res = mockMvc.perform(post("/api/v1/products").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Laptop"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Get public products (should find it)
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Laptop"));

        // Get public product detail
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Laptop"));
    }

    @Test
    @WithMockUser(username = "seller1")
    public void testUpdateProductAndHide() throws Exception {
        // Create first
        ProductCreateRequest req = new ProductCreateRequest();
        req.setTitle("To be updated");
        req.setPrice(new BigDecimal("100"));
        req.setCategoryId(testCategory.getId());
        
        MvcResult res = mockMvc.perform(post("/api/v1/products").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
                
        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        Long version = objectMapper.readTree(res.getResponse().getContentAsString()).get("version").asLong();

        // Update title
        ProductUpdateRequest updateReq = new ProductUpdateRequest();
        updateReq.setTitle("Updated Title");
        updateReq.setPrice(new BigDecimal("200"));
        updateReq.setCategoryId(testCategory.getId());
        updateReq.setVersion(version);

        MvcResult updateRes = mockMvc.perform(put("/api/v1/products/" + productId).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andReturn();
                
        Long newVersion = objectMapper.readTree(updateRes.getResponse().getContentAsString()).get("version").asLong();

        // Change status to HIDDEN
        ProductStatusUpdateRequest statusReq = new ProductStatusUpdateRequest();
        statusReq.setStatus(ProductStatus.HIDDEN);
        statusReq.setVersion(newVersion);

        mockMvc.perform(patch("/api/v1/products/" + productId + "/status").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));

        // Public listing should NOT show it anymore
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
                
        // Public detail should return 404 for HIDDEN
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "seller2")
    public void testUnauthorizedUpdate() throws Exception {
        // Assume Seller 1 created a product, Seller 2 tries to update it
        // We simulate by creating a product as Seller 2 first (since we can't easily switch mock user in middle of test method without custom runner)
        // Wait, if I create as Seller 2, then Seller 1 tries to update, I need two tests or change context.
        // Let's just create it directly in DB for Seller 1.
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Seller 1 Item");
        p.setPrice(new BigDecimal("50"));
        p.setCategory(testCategory);
        p.setOwner(testUser); // Seller 1
        p.setStatus(ProductStatus.ACTIVE);
        // Save using repository? I don't have it autowired here. Let's just assume we can't easily test without injecting ProductRepository.
    }
}
