package com.coinguard.common.exception;

import com.coinguard.receipt.enums.ReceiptCategory;

public class ActiveBudgetAlreadyExistsException extends RuntimeException {

    public ActiveBudgetAlreadyExistsException(ReceiptCategory receiptCategory) {
        super("An active budget already exists for category: " + receiptCategory);
    }
}
