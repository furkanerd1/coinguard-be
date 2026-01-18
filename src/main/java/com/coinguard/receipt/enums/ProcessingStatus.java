package com.coinguard.receipt.enums;

public enum ProcessingStatus {
    UPLOADED,      // Yüklendi, kuyrukta bekliyor
    PROCESSING,    // Gemini işliyor
    COMPLETED,     // Başarıyla işlendi
    FAILED,        // İşlenemedi
    MANUAL_REVIEW  // Manuel kontrol gerekiyor
}
