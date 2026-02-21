package com.coinguard.receipt.service;

import com.coinguard.common.exception.AiProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GeminiOcrServiceTest {

    @InjectMocks
    private GeminiOcrService geminiOcrService;

    @Test
    @DisplayName("Should throw AiProcessingException when image file does not exist")
    void extractDataFromImage_FileNotFound_ThrowsException() {
        // GIVEN
        String invalidPath = "src/test/resources/non_existent_image.jpg";

        // WHEN & THEN
        AiProcessingException exception = assertThrows(AiProcessingException.class, () -> {geminiOcrService.extractDataFromImage(invalidPath);});

        assertTrue(exception.getMessage().contains("AI processing failed"));
    }

    @Test
    @DisplayName("Should throw AiProcessingException when API URL is invalid")
    void extractDataFromImage_InvalidApiUrl_ThrowsException() {
        // GIVEN
        ReflectionTestUtils.setField(geminiOcrService, "apiKey", "dummy_key");
        ReflectionTestUtils.setField(geminiOcrService, "apiUrl", "http://invalid-url.com/api");

        String tempFilePath = "src/test/resources/temp_test.jpg";
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("src/test/resources"));
            java.nio.file.Files.write(java.nio.file.Paths.get(tempFilePath), "fake-image".getBytes());

            // WHEN & THEN
            assertThrows(AiProcessingException.class, () -> {
                geminiOcrService.extractDataFromImage(tempFilePath);
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFilePath));
            } catch (Exception ignored) {}
        }
    }
}