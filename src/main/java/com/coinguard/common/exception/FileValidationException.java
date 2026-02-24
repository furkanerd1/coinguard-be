package com.coinguard.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 400
public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}