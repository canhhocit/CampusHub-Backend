package com.campushub.modules.locker.controller;

import com.campushub.modules.locker.dto.*;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.service.KioskService;
import com.campushub.modules.locker.service.LockerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kiosk")
@Tag(name = "Kiosk", description = "Mô phỏng màn hình cảm ứng Smart Locker (IoT Touchscreen Kiosk)")
public class KioskController {

    private final KioskService kioskService;
    private final LockerService lockerService;

    public KioskController(KioskService kioskService, LockerService lockerService) {
        this.kioskService = kioskService;
        this.lockerService = lockerService;
    }

    @GetMapping("/lockers")
    @Operation(summary = "Lấy danh sách các trạm tủ Locker đang hoạt động")
    public ResponseEntity<Page<LockerResponse>> getActiveLockers() {
        Page<LockerResponse> lockers = lockerService.getLockers(LockerStatus.ACTIVE, null, PageRequest.of(0, 50, Sort.by("code").ascending()));
        return ResponseEntity.ok(lockers);
    }

    @GetMapping("/lockers/{lockerId}/status")
    @Operation(summary = "Lấy trạng thái trực quan các ngăn trong tủ đồ để hiển thị lên màn hình Kiosk")
    public ResponseEntity<LockerDetailResponse> getLockerKioskStatus(@PathVariable Long lockerId) {
        return ResponseEntity.ok(lockerService.getLocker(lockerId));
    }

    @GetMapping("/lockers/{lockerId}/compartments")
    @Operation(summary = "Lấy danh sách ngăn tủ đồ")
    public ResponseEntity<List<CompartmentResponse>> getCompartments(@PathVariable Long lockerId) {
        return ResponseEntity.ok(lockerService.getCompartments(lockerId));
    }

    @PostMapping("/lockers/{lockerId}/pickup")
    @Operation(summary = "Nhập mã PIN 6 số tại màn hình Kiosk để mở ngăn nhận đồ")
    public ResponseEntity<KioskActionResponse> pickupByPin(
            @PathVariable Long lockerId,
            @Valid @RequestBody KioskPickupRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        return ResponseEntity.ok(kioskService.processPickupByPin(lockerId, request, clientIp));
    }

    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/lockers/{lockerId}/deposit")
    @Operation(summary = "Nhập mã đơn hàng tại màn hình Kiosk để mở ngăn gửi đồ")
    public ResponseEntity<KioskActionResponse> depositByOrderNumber(
            @PathVariable Long lockerId,
            @Valid @RequestBody KioskDepositRequest request
    ) {
        return ResponseEntity.ok(kioskService.processDepositByOrderNumber(lockerId, request));
    }
}

