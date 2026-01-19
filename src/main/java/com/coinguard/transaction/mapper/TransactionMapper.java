package com.coinguard.transaction.mapper;

import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

    @Mapping(source = "fromWallet.user.fullName", target = "senderName")
    @Mapping(source = "toWallet.user.fullName", target = "receiverName")
    @Mapping(source = "createdAt", target = "transactionDate")
    TransactionResponse toTransactionResponse(Transaction transaction);
}
