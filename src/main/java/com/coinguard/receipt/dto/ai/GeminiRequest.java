package com.coinguard.receipt.dto.ai;

import java.util.List;

public record GeminiRequest(List<Content> contents) {
    public record Content(List<Part> parts) {}
    public record Part(String text, InlineData inlineData) {}
    public record InlineData(String mimeType, String data) {}
}
