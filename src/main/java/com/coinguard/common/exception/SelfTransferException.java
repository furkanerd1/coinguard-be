package com.coinguard.common.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException(Long userId) {
        super("You cannot transfer money to yourself (User ID: " + userId + ")");
    }

    public SelfTransferException(String message) {
        super(message);
    }
}