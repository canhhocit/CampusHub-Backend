package com.campushub.modules.admin.controller;

import com.campushub.common.enums.UserStatus;
import com.campushub.modules.admin.dto.DashboardStatsResponse;
import com.campushub.modules.admin.dto.UpdateUserStatusRequest;
import com.campushub.modules.admin.dto.UserManagementResponse;
import com.campushub.modules.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Admin dashboard and user management APIs")
public class AdminController {

    private final AdminService adminService;
    private final com.campushub.modules.admin.service.SystemSettingService systemSettingService;

    public AdminController(AdminService adminService, com.campushub.modules.admin.service.SystemSettingService systemSettingService) {
        this.adminService = adminService;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/settings/fees")
    @Operation(summary = "Get platform fee configuration", description = "Get commission rates for sales and rentals")
    public ResponseEntity<com.campushub.modules.admin.dto.FeeConfigResponse> getFeeConfig() {
        return ResponseEntity.ok(systemSettingService.getFeeConfig());
    }

    @PutMapping("/settings/fees")
    @Operation(summary = "Update platform fee configuration", description = "Update commission rates for sales and rentals")
    public ResponseEntity<com.campushub.modules.admin.dto.FeeConfigResponse> updateFeeConfig(
            @Valid @RequestBody com.campushub.modules.admin.dto.UpdateFeeConfigRequest request) {
        return ResponseEntity.ok(systemSettingService.updateFeeConfig(request));
    }

    @GetMapping("/settings/media")
    @Operation(summary = "Get product media settings", description = "Get media limit settings for images and videos")
    public ResponseEntity<com.campushub.modules.admin.dto.ProductMediaSettingsResponse> getMediaConfig() {
        return ResponseEntity.ok(systemSettingService.getMediaConfig());
    }

    @PutMapping("/settings/media")
    @Operation(summary = "Update product media settings", description = "Update media limit settings for images and videos")
    public ResponseEntity<com.campushub.modules.admin.dto.ProductMediaSettingsResponse> updateMediaConfig(
            @Valid @RequestBody com.campushub.modules.admin.dto.UpdateProductMediaSettingsRequest request) {
        return ResponseEntity.ok(systemSettingService.updateMediaConfig(request));
    }

    @PatchMapping("/users/{userId}/identity")
    public ResponseEntity<UserManagementResponse> reviewIdentity(@PathVariable Long userId,
            @Valid @RequestBody com.campushub.modules.admin.dto.IdentityReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewIdentity(userId, request.approved()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", 
               description = "Get comprehensive dashboard statistics for admin panel")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", 
               description = "Get paginated list of all users with filters")
    public ResponseEntity<Page<UserManagementResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        com.campushub.common.model.PageRequests.validateSort(sort, direction, java.util.Set.of("createdAt", "username", "fullName", "email", "status"));
        if (size > 100) size = 100;

        Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(dir, sort));

        Page<UserManagementResponse> users = adminService.getAllUsers(search, status, pageable);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user status", 
               description = "Update user status (ACTIVE, PENDING, LOCKED)")
    public ResponseEntity<UserManagementResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        UserManagementResponse response = adminService.updateUserStatus(
                userId, request.getStatus(), request.getReason());

        return ResponseEntity.ok(response);
    }
}
