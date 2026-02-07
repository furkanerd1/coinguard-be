package com.coinguard.transaction.mapper;

import com.coinguard.transaction.dto.response.ReceiptResponse;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

    @Mapping(target = "senderName", source = "transaction", qualifiedByName = "mapSenderName")
    @Mapping(target = "receiverName", source = "transaction", qualifiedByName = "mapReceiverName")
    @Mapping(source = "createdAt", target = "transactionDate")
    TransactionResponse toTransactionResponse(Transaction transaction);

    @Mapping(target = "transactionId", source = "id")
    @Mapping(target = "senderName", source = "fromWallet.user.fullName")
    @Mapping(target = "senderAccount", source = "fromWallet.user.username")
    @Mapping(target = "receiverName", source = "toWallet.user.fullName")
    @Mapping(target = "receiverAccount", source = "toWallet.user.username")
    @Mapping(target = "transactionDate", source = "createdAt")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "transactionFee", constant = "0.00")
    @Mapping(target = "totalDeducted", source = "amount")
    ReceiptResponse toReceiptResponse(Transaction transaction);

    @Named("mapSenderName")
    default String mapSenderName(Transaction transaction) {
        if (transaction.getFromWallet() == null) {
            return "System / External";
        }
        return transaction.getFromWallet().getUser().getFullName();
    }

    @Named("mapReceiverName")
    default String mapReceiverName(Transaction transaction) {
        if (transaction.getToWallet() == null) {
            return "Bank Account";
        }
        return transaction.getToWallet().getUser().getFullName();
    }
}
