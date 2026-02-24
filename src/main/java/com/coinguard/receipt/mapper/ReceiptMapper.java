package com.coinguard.receipt.mapper;

import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.entity.Receipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    @Mapping(target = "uploadDate", source = "createdAt")
    ReceiptDto toDto(Receipt receipt);
}