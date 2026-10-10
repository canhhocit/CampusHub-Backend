package com.campushub.modules.auth.service;

import com.campushub.modules.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void register(RegisterRequest request, HttpServletRequest httpRequest);
    void verifyEmail(VerifyEmailRequest request);
    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    TokenResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);
    void resetPassword(ResetPasswordRequest request);
}
