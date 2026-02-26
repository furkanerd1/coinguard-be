package com.coinguard.transaction.service;

import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransactionFilterRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.ReceiptResponse;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

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
     * @param pageable Pageable object containing page, size, and sort information
     * @return Page of transaction responses
     */
    Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable);

    /**
     * Retrieve filtered and paginated transaction history for a user
     * @param userId User ID
     * @param filterRequest Filter criteria
     * @param pageable Pageable object containing page, size, and sort information
     * @return Page of transaction responses
     */
    Page<TransactionResponse> getFilteredTransactionHistory(Long userId, TransactionFilterRequest filterRequest, Pageable pageable);

    /**
     * Retrieve transaction details by unique reference number
     * @param referenceNo Unique transaction reference code
     * @return Transaction response DTO
     * @throws RuntimeException if transaction not found
     */
    TransactionResponse getByReference(String referenceNo, Long currentUserId);

    /**
     * Get transaction statistics for a user over a specified period
     * @param userId User ID
     * @param period Predefined period (e.g., "DAILY", "WEEKLY", "MONTHLY")
     * @param startDate Custom start date (optional)
     * @param endDate Custom end date (optional)
     * @return Transaction statistics response
     */
    TransactionStatsResponse getTransactionStats(Long userId, String period, LocalDate startDate, LocalDate endDate);


    /**
     * Generate a receipt for a completed transaction using its reference number
     * @param referenceNo
     * @param currentUserId
     * @return
     */
    ReceiptResponse getTransactionReceipt(String referenceNo, Long currentUserId);
}
