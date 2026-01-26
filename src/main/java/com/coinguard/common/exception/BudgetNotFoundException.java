package com.coinguard.common.exception;

public class BudgetNotFoundException extends RuntimeException{

    public BudgetNotFoundException(Long budgetId) {
        super("Budget with ID " + budgetId + " not found.");
    }
}
