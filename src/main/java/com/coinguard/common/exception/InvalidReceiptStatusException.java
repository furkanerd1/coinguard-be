package com.coinguard.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidReceiptStatusException extends RuntimeException {
    public InvalidReceiptStatusException(String message) {
        super(message);
    }
}
