package com.campushub.modules.notification.controller;

import com.campushub.modules.notification.dto.NotificationResponse;
import com.campushub.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Trung tâm thông báo người dùng")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của người dùng")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số lượng thông báo chưa đọc")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(id, authentication.getName()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(Authentication authentication) {
        int updatedCount = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }
}

