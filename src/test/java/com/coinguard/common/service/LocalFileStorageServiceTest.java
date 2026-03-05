package com.coinguard.common.service;

import com.coinguard.common.exception.FileValidationException;
import com.coinguard.common.service.impl.LocalFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    private LocalFileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        fileStorageService.init();
    }

    @Test
    @DisplayName("Should store valid file successfully")
    void storeFile_Success() {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        // WHEN
        String storedPath = fileStorageService.storeFile(file, "receipts");

        // THEN
        assertNotNull(storedPath);
        assertTrue(Files.exists(Path.of(storedPath))); // Dosya gerçekten oluştu mu?
        assertTrue(storedPath.endsWith(".jpg")); // Uzantısı doğru mu?
    }

    @Test
    @DisplayName("Should throw exception for empty file")
    void storeFile_EmptyFile() {
        // GIVEN
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        // WHEN & THEN
        assertThrows(FileValidationException.class, () -> fileStorageService.storeFile(emptyFile, "receipts"));
    }

    @Test
    @DisplayName("Should throw exception for invalid path sequence (..)")
    void storeFile_PathTraversal() {
        // GIVEN
        MockMultipartFile maliciousFile = new MockMultipartFile("file", "../hack.exe", "application/octet-stream", "malware".getBytes());

        // WHEN & THEN
        assertThrows(FileValidationException.class, () -> fileStorageService.storeFile(maliciousFile, "receipts"));
    }
}
