package com.campushub;

import com.campushub.modules.upload.service.UploadService;
import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class UploadValidationTest {
    // No provider should be contacted for a rejected file.
    private final UploadService upload = new UploadService(null, null);

    @Test void rejectsSvgEvenIfItClaimsToBeAnImage() {
        var file = new MockMultipartFile("file", "active.svg", "image/svg+xml", "<svg><script>alert(1)</script></svg>".getBytes());
        assertEquals(ErrorCode.INVALID_FILE_TYPE, assertThrows(AppException.class, () -> upload.uploadImage(file)).getErrorCode());
    }

    @Test void rejectsMismatchedMimeAndBytes() {
        var file = new MockMultipartFile("file", "fake.png", "image/png", "this is not an image file".getBytes());
        assertEquals(ErrorCode.INVALID_FILE_TYPE, assertThrows(AppException.class, () -> upload.uploadImage(file)).getErrorCode());
    }

    @Test void rejectsOversizeBeforeContactingProvider() {
        var file = new MockMultipartFile("file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]);
        assertThrows(org.springframework.web.multipart.MaxUploadSizeExceededException.class, () -> upload.uploadImage(file));
    }
}
