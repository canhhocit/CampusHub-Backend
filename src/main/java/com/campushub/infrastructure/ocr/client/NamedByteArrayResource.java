package com.campushub.infrastructure.ocr.client;

import org.springframework.core.io.ByteArrayResource;

import java.io.File;

/**
 * Custom ByteArrayResource that supplies a non-null, sanitized filename.
 * Required by Spring's FormHttpMessageConverter to treat the multipart part as a file upload
 * rather than a simple form text field.
 */
public class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    public NamedByteArrayResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = sanitizeFilename(filename);
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "upload.jpg";
        }
        // Extract base name to protect against directory traversal attacks
        String baseName = new File(raw).getName();
        String cleaned = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "upload.jpg" : cleaned;
    }
}
