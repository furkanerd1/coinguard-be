package com.coinguard.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(Long userId) {
        super("Wallet not found for user ID:" + userId);
    }

    public WalletNotFoundException(String message) {
        super(message);
    }
}
