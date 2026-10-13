package com.campushub.modules.ping.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Health & Ping", description = "APIs kiểm tra trạng thái và giữ máy chủ Render luôn thức")
public class PingController {

    @GetMapping({"/api/v1/ping", "/ping"})
    @Operation(summary = "Ping máy chủ", description = "API mở không cần token, dùng cho cronjob (cron-job.org, UptimeRobot,...) để giữ server Render không bị sleep")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Pong! CampusHub Backend is awake and healthy.",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping({"/api/v1/health", "/health"})
    @Operation(summary = "Kiểm tra sức khỏe hệ thống", description = "Kiểm tra trạng thái sẵn sàng của dịch vụ Backend")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "HEALTHY",
                "service", "campushub-backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
