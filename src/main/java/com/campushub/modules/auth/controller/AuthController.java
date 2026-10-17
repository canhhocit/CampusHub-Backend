package com.campushub.modules.auth.controller;

import com.campushub.modules.auth.dto.*;
import com.campushub.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and account management APIs")
public class AuthController {

    private final AuthService authService;
    private final com.campushub.modules.user.repository.UserRepository userRepository;

    public AuthController(AuthService authService, com.campushub.modules.user.repository.UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account. Email verification required.")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        authService.register(request, httpRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email with token sent via email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user with username/email and password. Returns JWT access token and sets refresh token cookie.")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, 
                                               HttpServletRequest httpRequest, 
                                               HttpServletResponse httpResponse) {
        TokenResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token from cookie")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        TokenResponse response = authService.refreshToken(httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout user and revoke refresh token")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset token via email")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Get basic info of currently authenticated user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        userRepository.findByUsername(authentication.getName()).ifPresent(u -> {
            boolean isVerified = u.isIdentityApproved();
            response.put("identityVerified", isVerified);
            response.put("identityReviewStatus", u.getIdentityReviewStatus());
            response.put("fullName", u.getFullName());
            response.put("email", u.getEmail());
            response.put("citizenId", u.getCitizenId());
            response.put("dateOfBirth", u.getDateOfBirth());
        });
        return ResponseEntity.ok(response);
    }
}
