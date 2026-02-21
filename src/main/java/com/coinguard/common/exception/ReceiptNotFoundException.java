package com.coinguard.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReceiptNotFoundException extends RuntimeException {
    public ReceiptNotFoundException(Long receiptId) {
        super("Receipt not found with id: " + receiptId);
    }
}
