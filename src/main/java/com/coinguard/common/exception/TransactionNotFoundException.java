package com.coinguard.common.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String referenceNo) {
        super("Transaction not found with reference: " + referenceNo);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}