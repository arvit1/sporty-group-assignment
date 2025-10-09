package com.jackpot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "bet_id", nullable = false)
    private String betId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "jackpot_id", nullable = false)
    private String jackpotId;

    @NotNull
    @Positive
    @Column(name = "jackpot_reward_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal jackpotRewardAmount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    // Constructors
    public Reward() {}

    public Reward(String betId, Long userId, String jackpotId, BigDecimal jackpotRewardAmount) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.jackpotRewardAmount = jackpotRewardAmount;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBetId() { return betId; }
    public void setBetId(String betId) { this.betId = betId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getJackpotId() { return jackpotId; }
    public void setJackpotId(String jackpotId) { this.jackpotId = jackpotId; }

    public BigDecimal getJackpotRewardAmount() { return jackpotRewardAmount; }
    public void setJackpotRewardAmount(BigDecimal jackpotRewardAmount) { this.jackpotRewardAmount = jackpotRewardAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}