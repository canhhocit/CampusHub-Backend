package com.campushub.modules.upload.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.admin.service.SystemSettingService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-matroska"
    );

    private final Cloudinary cloudinary;
    private final SystemSettingService systemSettingService;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    public UploadService(Cloudinary cloudinary, SystemSettingService systemSettingService) {
        this.cloudinary = cloudinary;
        this.systemSettingService = systemSettingService;
    }

    public boolean isCloudinaryConfigured() {
        return cloudName != null && !cloudName.trim().isEmpty()
                && apiKey != null && !apiKey.trim().isEmpty();
    }

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10L * 1024 * 1024);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesImageHeader(bytes, contentType.toLowerCase(Locale.ROOT))) {
                throw new AppException(ErrorCode.INVALID_FILE_TYPE);
            }

            if (isCloudinaryConfigured()) {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        bytes,
                        ObjectUtils.asMap(
                                "folder", "campushub/images",
                                "resource_type", "image"
                        )
                );

                String secureUrl = (String) uploadResult.get("secure_url");
                if (secureUrl == null || secureUrl.isBlank()) {
                    throw new AppException(ErrorCode.UPLOAD_FAILED);
                }

                log.info("Uploaded image to Cloudinary successfully: {}", secureUrl);
                return secureUrl;
            } else {
                log.info("Cloudinary credentials not set. Storing uploaded image to local storage.");
                return storeLocally(file, "images");
            }
        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    public String uploadVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        var mediaConfig = systemSettingService.getMediaConfig();
        if (!Boolean.TRUE.equals(mediaConfig.getAllowProductVideo())) {
            throw new AppException(ErrorCode.VIDEO_UPLOAD_NOT_ALLOWED);
        }

        long maxBytes = (long) mediaConfig.getMaxVideoSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new AppException(ErrorCode.VIDEO_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_VIDEO_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (isCloudinaryConfigured()) {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        bytes,
                        ObjectUtils.asMap(
                                "folder", "campushub/videos",
                                "resource_type", "video"
                        )
                );

                String secureUrl = (String) uploadResult.get("secure_url");
                if (secureUrl == null || secureUrl.isBlank()) {
                    throw new AppException(ErrorCode.UPLOAD_FAILED);
                }

                log.info("Uploaded video to Cloudinary successfully: {}", secureUrl);
                return secureUrl;
            } else {
                log.info("Cloudinary credentials not set. Storing uploaded video to local storage.");
                return storeLocally(file, "videos");
            }
        } catch (IOException e) {
            log.error("Failed to upload video: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    private String storeLocally(MultipartFile file, String subDir) throws IOException {
        String uploadBasePath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + subDir;
        Path dirPath = Paths.get(uploadBasePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }

        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = dirPath.resolve(uniqueFilename);
        Files.write(targetPath, file.getBytes());

        log.info("Stored file locally at: {}", targetPath.toAbsolutePath());
        return "/uploads/" + subDir + "/" + uniqueFilename;
    }

    private boolean matchesImageHeader(byte[] bytes, String contentType) {
        if (bytes.length < 12) return false;
        return switch (contentType) {
            case "image/jpeg" -> (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
            case "image/png" -> java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case "image/gif" -> new String(bytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a");
            case "image/webp" -> new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
}
