package com.coinguard.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500 hatası
public class AiProcessingException extends RuntimeException {
    public AiProcessingException(String message) {
        super(message);
    }

    public AiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
