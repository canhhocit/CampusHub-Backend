package com.campushub.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {
    @NotBlank
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
