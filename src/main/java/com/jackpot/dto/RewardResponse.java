package com.jackpot.dto;

import java.math.BigDecimal;

public record RewardResponse(
    String betId,
    boolean won,
    BigDecimal rewardAmount,
    String message
) {}