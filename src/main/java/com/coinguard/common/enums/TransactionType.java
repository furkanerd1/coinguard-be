package com.coinguard.common.enums;

public enum TransactionType {
    TRANSFER,      // Kullanıcılar arası transfer
    DEPOSIT,       // Para yatırma (dışarıdan)
    WITHDRAWAL,    // Para çekme (dışarıya)
    FEE,           // İşlem ücreti
    RECEIPT_EXPENSE
}
