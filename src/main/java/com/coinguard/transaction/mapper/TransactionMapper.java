package com.coinguard.transaction.mapper;

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
