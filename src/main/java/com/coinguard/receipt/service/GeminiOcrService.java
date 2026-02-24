package com.coinguard.receipt.service;

import com.coinguard.common.exception.AiProcessingException;
import com.coinguard.receipt.dto.ai.ExtractedReceiptData;
import com.coinguard.receipt.dto.ai.GeminiRequest;
import com.coinguard.receipt.dto.ai.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiOcrService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private ObjectMapper objectMapper = new ObjectMapper();


    private static final String PROMPT = """
            Analyze this receipt image and extract the following details in JSON format:
            1. merchantName (String): Name of the store/merchant.
            2. date (String): Date of transaction in YYYY-MM-DD format. If not found, return null.
            3. amount (BigDecimal): Total amount paid.
            4. category (String): Predict the category from this list: [FOOD_BEVERAGE, GROCERY, TRANSPORT, SHOPPING, HEALTHCARE, UTILITIES, EDUCATION, TECHNOLOGY, OTHER].
            5. confidence (Double): Your confidence score (0.0 to 1.0) about this extraction.
            
            Return ONLY the raw JSON object. Do not include markdown formatting like ```json ... ```.
            """;

    public ExtractedReceiptData extractDataFromImage(String imagePath) {
        try {
            // read image and convert to base64
            String base64Image = encodeImageToBase64(imagePath);

            // pre
            GeminiRequest request = createRequest(base64Image);

            // send it to gemini API
            RestClient restClient = RestClient.create();
            GeminiResponse response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            // parse response and return structured data
            return parseResponse(response);

        } catch (Exception e) {
            log.error("Gemini OCR error: ", e);
            throw new AiProcessingException("AI processing failed: " + e.getMessage());
        }
    }

    private String encodeImageToBase64(String path) throws IOException {
        byte[] imageBytes = Files.readAllBytes(Paths.get(path));
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private GeminiRequest createRequest(String base64Image) {
        GeminiRequest.InlineData imagePart = new GeminiRequest.InlineData("image/jpeg", base64Image);

        GeminiRequest.Part textPart = new GeminiRequest.Part(PROMPT, null);
        GeminiRequest.Part filePart = new GeminiRequest.Part(null, imagePart);

        // before prompt then file, because we want AI to read prompt first and then analyze the image
        return new GeminiRequest(List.of(
                new GeminiRequest.Content(List.of(textPart, filePart))
        ));
    }

    private ExtractedReceiptData parseResponse(GeminiResponse response) {
        try {
            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new AiProcessingException("No response from AI");
            }

            // raw JSON String from AI response
            String rawJson = response.candidates().get(0).content().parts().get(0).text();

            // clear any markdown formatting if exists, we want only raw JSON
            rawJson = rawJson.replace("```json", "").replace("```", "").trim();

            log.info("Gemini Raw Response: {}", rawJson);

            return objectMapper.readValue(rawJson, ExtractedReceiptData.class);

        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            // if error occurs then return null data
            return new ExtractedReceiptData(null, null, null, "OTHER", 0.0);
        }
    }
}