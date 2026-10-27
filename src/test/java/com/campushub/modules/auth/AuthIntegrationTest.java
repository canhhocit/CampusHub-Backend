package com.campushub.modules.auth;

import com.campushub.modules.auth.dto.LoginRequest;
import com.campushub.modules.auth.dto.RegisterRequest;
import com.campushub.modules.auth.entity.VerificationToken;
import com.campushub.modules.auth.repository.RefreshTokenRepository;
import com.campushub.modules.auth.repository.VerificationTokenRepository;
import com.campushub.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback after each test
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    public void setup() {
        refreshTokenRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testFullAuthFlow() throws Exception {
        // 1. Register User
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("testuser");
        registerReq.setEmail("test@campushub.com");
        registerReq.setPassword("password123");
        registerReq.setFullName("Test User");
        registerReq.setDateOfBirth(java.time.LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        assertTrue(userRepository.existsByUsername("testuser"));
        
        // Ensure token was created
        assertEquals(1, verificationTokenRepository.count());
        assertNotNull(verificationTokenRepository.findAll().get(0).getTokenHash());
        
        // 2. Login fails because PENDING
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsernameOrEmail("testuser");
        loginReq.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden()); // USER_PENDING

        // 3. Verify Email (Wait, we can't easily get the raw token because it's hashed in DB)
        // Since it's hashed in DB, we can't test the actual verify endpoint without the raw token.
        // I will manually activate the user in DB for the rest of the flow.
        userRepository.findByUsername("testuser").ifPresent(user -> {
            user.setStatus(com.campushub.common.enums.UserStatus.ACTIVE);
            userRepository.save(user);
        });

        // 4. Login successful
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refresh_token");
        assertNotNull(refreshTokenCookie);
        
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // 5. Access Protected Endpoint /me
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
                
        // 6. Access without token fails
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // 7. Refresh Token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();
                
        Cookie newRefreshTokenCookie = refreshResult.getResponse().getCookie("refresh_token");
        assertNotEquals(refreshTokenCookie.getValue(), newRefreshTokenCookie.getValue());
        
        // Test Token Reuse Attack (Use old token again)
        mockMvc.perform(post("/api/v1/auth/refresh").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .cookie(refreshTokenCookie))
                .andExpect(status().isUnauthorized()); // TOKEN_REUSE_DETECTED
    }

    @Test
    public void testRegistrationUnder13Fails() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("kiduser");
        registerReq.setEmail("kid@campushub.com");
        registerReq.setPassword("password123");
        registerReq.setFullName("Kid User");
        registerReq.setDateOfBirth(java.time.LocalDate.now().minusYears(10)); // 10 years old < 13

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest());
    }
}
