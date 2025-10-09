package com.jackpot.dto;

public record AuthRequest(
    String username,
    String password
) {}