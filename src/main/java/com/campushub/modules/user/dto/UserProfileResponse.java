package com.campushub.modules.user.dto;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import java.time.LocalDateTime;

public class UserProfileResponse {
    private String identityReviewStatus;
    public String getIdentityReviewStatus() { return identityReviewStatus; }
    public void setIdentityReviewStatus(String value) { identityReviewStatus = value; }


    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private UserStatus status;
    private java.time.LocalDate dateOfBirth;
    private Boolean identityVerified;
    private String citizenId;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private LocalDateTime identityVerifiedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Boolean getIdentityVerified() { return identityVerified; }
    public void setIdentityVerified(Boolean identityVerified) { this.identityVerified = identityVerified; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getIdCardFrontUrl() { return idCardFrontUrl; }
    public void setIdCardFrontUrl(String idCardFrontUrl) { this.idCardFrontUrl = idCardFrontUrl; }
    public String getIdCardBackUrl() { return idCardBackUrl; }
    public void setIdCardBackUrl(String idCardBackUrl) { this.idCardBackUrl = idCardBackUrl; }
    public LocalDateTime getIdentityVerifiedAt() { return identityVerifiedAt; }
    public void setIdentityVerifiedAt(LocalDateTime identityVerifiedAt) { this.identityVerifiedAt = identityVerifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
