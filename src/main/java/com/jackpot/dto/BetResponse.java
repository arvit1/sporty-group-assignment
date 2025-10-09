package com.jackpot.dto;

public record BetResponse(
    String betId,
    String status,
    String message
) {}