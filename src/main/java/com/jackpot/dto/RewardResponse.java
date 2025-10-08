package com.jackpot.dto;

import java.math.BigDecimal;

public record RewardResponse(
    String betId,
    boolean winsJackpot,
    BigDecimal rewardAmount,
    String message
) {}