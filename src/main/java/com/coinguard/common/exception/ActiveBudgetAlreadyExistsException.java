package com.coinguard.common.exception;

import com.coinguard.common.enums.TransactionCategory;

public class ActiveBudgetAlreadyExistsException extends RuntimeException {

    public ActiveBudgetAlreadyExistsException(TransactionCategory transactionCategory) {
        super("An active budget already exists for category: " + transactionCategory);
    }
}
