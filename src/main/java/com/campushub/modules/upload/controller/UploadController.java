package com.campushub.modules.upload.controller;

import com.campushub.modules.upload.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "Upload", description = "Media upload APIs (Images & Videos)")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tải ảnh lên hệ thống", description = "Tải file ảnh (JPG, PNG, WEBP, GIF) lên Cloudinary qua máy chủ an toàn và nhận đường dẫn ảnh")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tải video lên hệ thống", description = "Tải file video (MP4, WEBM, MOV) lên Cloudinary qua máy chủ an toàn và nhận đường dẫn video")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadVideo(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/status")
    @Operation(summary = "Kiểm tra cấu hình Cloudinary", description = "Kiểm tra xem dịch vụ Cloudinary đã được cấu hình API Key/Secret hay chưa")
    public ResponseEntity<Map<String, Object>> getUploadStatus() {
        boolean isConfigured = uploadService.isCloudinaryConfigured();
        return ResponseEntity.ok(Map.of(
                "cloudinaryConfigured", isConfigured,
                "provider", isConfigured ? "CLOUDINARY" : "LOCAL_FALLBACK"
        ));
    }
}
