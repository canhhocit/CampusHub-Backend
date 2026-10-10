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
    private com.campushub.modules.marketplace.repository.ProductRepository productRepository;

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
                
        // Owner should still be able to see their own HIDDEN product
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    public void testAnonymousCannotViewHiddenProduct() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Hidden Item");
        p.setPrice(new BigDecimal("50"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.HIDDEN);
        p = productRepository.save(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "seller2")
    public void testUnauthorizedUpdate() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Seller 1 Item");
        p.setPrice(new BigDecimal("50"));
        p.setCategory(testCategory);
        p.setOwner(testUser); // Seller 1
        p.setStatus(ProductStatus.ACTIVE);
        p = productRepository.save(p);

        ProductUpdateRequest updateReq = new ProductUpdateRequest();
        updateReq.setTitle("Hacked Title");
        updateReq.setPrice(new BigDecimal("10"));
        updateReq.setCategoryId(testCategory.getId());
        updateReq.setVersion(p.getVersion());

        mockMvc.perform(put("/api/v1/products/" + p.getId()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "seller1")
    public void testConcurrentUpdate_RejectsStaleVersionAndPreservesDatabaseState() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Concurrent Item");
        p.setPrice(new BigDecimal("50.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.ACTIVE);
        p = productRepository.saveAndFlush(p);

        Long initialVersion = p.getVersion();

        ProductUpdateRequest updateReq = new ProductUpdateRequest();
        updateReq.setTitle("Overwritten Title");
        updateReq.setPrice(new BigDecimal("99.99"));
        updateReq.setCategoryId(testCategory.getId());
        updateReq.setVersion(initialVersion - 1); // Pass stale/invalid version

        mockMvc.perform(put("/api/v1/products/" + p.getId()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));

        // Verify database state: actual data was NOT silently overwritten and version remains unchanged
        com.campushub.modules.marketplace.entity.Product actualProduct = productRepository.findById(p.getId()).orElseThrow();
        assertEquals("Concurrent Item", actualProduct.getTitle());
        assertEquals(0, new BigDecimal("50.00").compareTo(actualProduct.getPrice()));
        assertEquals(initialVersion, actualProduct.getVersion());
    }

    @Test
    @WithMockUser(username = "seller1")
    public void testConcurrentStatusUpdate_RejectsStaleVersionAndPreservesDatabaseState() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Status Item");
        p.setPrice(new BigDecimal("70.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.ACTIVE);
        p = productRepository.saveAndFlush(p);

        ProductStatusUpdateRequest statusReq = new ProductStatusUpdateRequest();
        statusReq.setStatus(ProductStatus.HIDDEN);
        statusReq.setVersion(p.getVersion() - 1); // Stale version

        mockMvc.perform(patch("/api/v1/products/" + p.getId() + "/status").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));

        com.campushub.modules.marketplace.entity.Product actualProduct = productRepository.findById(p.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, actualProduct.getStatus());
        assertEquals(p.getVersion(), actualProduct.getVersion());
    }

    // --- Product Detail Authorization Matrix (9 Permutations) ---

    // 1. Anonymous + ACTIVE -> 200 OK
    @Test
    public void testDetail_Anonymous_Active() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Anon Active Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.ACTIVE);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Anon Active Product"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.owner.username").value("seller1"));
    }

    // 2. Anonymous + HIDDEN -> 404
    @Test
    public void testDetail_Anonymous_Hidden() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Anon Hidden Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.HIDDEN);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // 3. Anonymous + SOLD -> 404
    @Test
    public void testDetail_Anonymous_Sold() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Anon Sold Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.SOLD);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // 4. Authenticated Owner + ACTIVE -> 200 OK
    @Test
    @WithMockUser(username = "seller1")
    public void testDetail_Owner_Active() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Owner Active Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.ACTIVE);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Owner Active Product"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // 5. Authenticated Owner + HIDDEN -> 200 OK
    @Test
    @WithMockUser(username = "seller1")
    public void testDetail_Owner_Hidden() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Owner Hidden Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.HIDDEN);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Owner Hidden Product"))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    // 6. Authenticated Owner + SOLD -> 200 OK
    @Test
    @WithMockUser(username = "seller1")
    public void testDetail_Owner_Sold() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Owner Sold Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser);
        p.setStatus(ProductStatus.SOLD);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Owner Sold Product"))
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    // 7. Other User + HIDDEN -> 404
    @Test
    @WithMockUser(username = "seller2")
    public void testDetail_OtherUser_Hidden() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Seller 1 Hidden Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser); // Owned by seller1
        p.setStatus(ProductStatus.HIDDEN);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // 8. Other User + SOLD -> 404
    @Test
    @WithMockUser(username = "seller2")
    public void testDetail_OtherUser_Sold() throws Exception {
        com.campushub.modules.marketplace.entity.Product p = new com.campushub.modules.marketplace.entity.Product();
        p.setTitle("Seller 1 Sold Product");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(testCategory);
        p.setOwner(testUser); // Owned by seller1
        p.setStatus(ProductStatus.SOLD);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // 9. Non-existent Product -> 404
    @Test
    public void testDetail_ProductNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // Validation: Description > 2000 chars -> 400 Bad Request
    @Test
    @WithMockUser(username = "seller1")
    public void testCreateProduct_DescriptionTooLong_ReturnsBadRequest() throws Exception {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setTitle("Valid Title");
        req.setPrice(new BigDecimal("50.00"));
        req.setCategoryId(testCategory.getId());
        req.setDescription("A".repeat(2001));

        mockMvc.perform(post("/api/v1/products").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").exists());
    }
}
