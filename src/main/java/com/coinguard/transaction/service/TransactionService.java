package com.coinguard.transaction.service;

import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;

public interface TransactionService {

    /**
     * Perform internal money transfer between users
     * @param senderId Sender's User ID
     * @param request Transfer details (receiver, amount, description)
     * @return Completed transaction response
     * @throws RuntimeException if balance is insufficient
     */
    TransactionResponse transfer(Long senderId, TransferRequest request);

    /**
     * Process deposit from external source (e.g., Credit Card)
     * @param userId User ID performing the deposit
     * @param request Deposit details (amount, card info)
     * @return Completed transaction response
     */
    TransactionResponse deposit(Long userId, DepositRequest request);

    /**
     * Process withdrawal to external account (e.g., IBAN)
     * @param userId User ID performing the withdrawal
     * @param request Withdrawal details (amount, IBAN)
     * @return Completed transaction response
     * @throws RuntimeException if balance is insufficient
     */
    TransactionResponse withdraw(Long userId, WithdrawRequest request);

    /**
     * Retrieve paginated transaction history for a user
     * @param userId User ID
     * @param page Page number (zero-based)
     * @param size Page size
     * @return Page of transaction responses
     */
    Page<TransactionResponse> getTransactionHistory(Long userId, int page, int size);

    /**
     * Retrieve transaction details by unique reference number
     * @param referenceNo Unique transaction reference code
     * @return Transaction response DTO
     * @throws RuntimeException if transaction not found
     */
    TransactionResponse getByReference(String referenceNo);
}
