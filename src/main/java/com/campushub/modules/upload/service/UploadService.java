package com.campushub.modules.upload.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private final Cloudinary cloudinary;

    public UploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10L * 1024 * 1024);
        }

        String contentType = file.getContentType();
        if (contentType == null || !java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp").contains(contentType.toLowerCase(java.util.Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesImageHeader(bytes, contentType.toLowerCase(java.util.Locale.ROOT))) {
                throw new AppException(ErrorCode.INVALID_FILE_TYPE);
            }
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "campushub",
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }

            log.info("Uploaded image to Cloudinary successfully: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
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
