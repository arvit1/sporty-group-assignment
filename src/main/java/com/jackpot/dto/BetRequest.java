package com.jackpot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BetRequest(
    @NotNull
    String betId,

    @NotNull
    String jackpotId,

    @NotNull
    @Positive
    BigDecimal betAmount
) {}