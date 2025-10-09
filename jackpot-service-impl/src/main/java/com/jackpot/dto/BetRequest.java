package com.jackpot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BetRequest(
        @NotNull(message = "Bet ID cannot be null")
        String betId,

        @NotNull(message = "Jackpot ID cannot be null")
        String jackpotId,

        @NotNull(message = "Bet amount cannot be null")
        @Positive(message = "Bet amount must be positive")
        BigDecimal betAmount
) {}