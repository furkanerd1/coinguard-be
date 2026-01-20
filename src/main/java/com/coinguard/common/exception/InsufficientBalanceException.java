package com.coinguard.common.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(BigDecimal balance) {
        super("Insufficient balance. Current balance: " + balance);
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}