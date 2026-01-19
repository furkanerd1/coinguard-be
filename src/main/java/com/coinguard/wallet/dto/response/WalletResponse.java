package com.coinguard.wallet.dto.response;


import com.coinguard.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WalletResponse(
        Long id,
        Long userId,
        String userFullName,
        BigDecimal balance,
        Currency currency,
        BigDecimal dailyLimit,
        BigDecimal dailySpent,
        LocalDate lastResetDate,
        boolean isFrozen
){}
