package com.jackpot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "bet_id", nullable = false)
    private String betId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Column(name = "jackpot_id", nullable = false)
    private String jackpotId;

    @NotNull
    @Positive
    @Column(name = "stake_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal stakeAmount;

    @NotNull
    @Positive
    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contributionAmount;

    @NotNull
    @Positive
    @Column(name = "current_jackpot_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentJackpotAmount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    // Constructors
    public Contribution() {}

    public Contribution(String betId, String userId, String jackpotId,
                       BigDecimal stakeAmount, BigDecimal contributionAmount,
                       BigDecimal currentJackpotAmount) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.stakeAmount = stakeAmount;
        this.contributionAmount = contributionAmount;
        this.currentJackpotAmount = currentJackpotAmount;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBetId() { return betId; }
    public void setBetId(String betId) { this.betId = betId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getJackpotId() { return jackpotId; }
    public void setJackpotId(String jackpotId) { this.jackpotId = jackpotId; }

    public BigDecimal getStakeAmount() { return stakeAmount; }
    public void setStakeAmount(BigDecimal stakeAmount) { this.stakeAmount = stakeAmount; }

    public BigDecimal getContributionAmount() { return contributionAmount; }
    public void setContributionAmount(BigDecimal contributionAmount) { this.contributionAmount = contributionAmount; }

    public BigDecimal getCurrentJackpotAmount() { return currentJackpotAmount; }
    public void setCurrentJackpotAmount(BigDecimal currentJackpotAmount) { this.currentJackpotAmount = currentJackpotAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}