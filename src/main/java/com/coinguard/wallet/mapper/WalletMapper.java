package com.coinguard.wallet.mapper;

import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WalletMapper {


    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    WalletResponse toWalletResponse(Wallet wallet);
}
