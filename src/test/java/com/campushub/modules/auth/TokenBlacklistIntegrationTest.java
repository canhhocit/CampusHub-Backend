package com.campushub.modules.auth;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.common.security.JwtProvider;
import com.campushub.common.security.TokenBlacklistService;
import com.campushub.infrastructure.redis.RedisResilientService;
import com.campushub.modules.admin.service.AdminService;
import com.campushub.modules.auth.dto.LoginRequest;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TokenBlacklistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisResilientService redisResilientService;

    @Autowired
    private AdminService adminService;

    private User testUser;
    private String rawPassword = "StrongPassword123!";

    @BeforeEach
    public void setup() {
        redisResilientService.clearMemoryStore();

        testUser = userRepository.findByUsername("blacklist_user_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("blacklist_user_test");
            u.setEmail("blacklist_user@test.com");
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setFullName("Blacklist Test User");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    private String loginAndGetToken() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsernameOrEmail(testUser.getUsername());
        loginReq.setPassword(rawPassword);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    public void testLogoutBlacklistsAccessToken() throws Exception {
        String token = loginAndGetToken();

        // 1. Verify access works with valid token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));

        // 2. Call Logout with Authorization Bearer header
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Token is now blacklisted in Redis/In-memory store
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token),
                "Access token should be marked as blacklisted after logout");

        // 4. Request with same token should be rejected with 401 TOKEN_REVOKED
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_REVOKED"));
    }

    @Test
    public void testAdminBanUserRevokesActiveSession() throws Exception {
        String token = loginAndGetToken();

        // Access works
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Admin locks user
        adminService.updateUserStatus(testUser.getId(), UserStatus.LOCKED, "Violation of platform rules");

        // Subsequent access with the existing token is rejected immediately
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}

