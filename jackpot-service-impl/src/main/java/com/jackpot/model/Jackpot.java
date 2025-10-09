package com.jackpot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
@Table(name = "jackpots")
public class Jackpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "jackpot_id", nullable = false, unique = true)
    private String jackpotId;

    @NotNull
    @Positive
    @Column(name = "initial_pool_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPoolValue;

    @NotNull
    @Column(name = "current_pool_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPoolValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type", nullable = false)
    private ContributionType contributionType;

    @Positive
    @Column(name = "fixed_contribution_percentage", precision = 5, scale = 2)
    private BigDecimal fixedContributionPercentage;

    @Positive
    @Column(name = "variable_contribution_base_percentage", precision = 5, scale = 2)
    private BigDecimal variableContributionBasePercentage;

    @Positive
    @Column(name = "variable_contribution_decay_rate", precision = 5, scale = 2)
    private BigDecimal variableContributionDecayRate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @Positive
    @Column(name = "fixed_reward_chance", precision = 5, scale = 2)
    private BigDecimal fixedRewardChance;

    @Positive
    @Column(name = "variable_reward_base_chance", precision = 5, scale = 2)
    private BigDecimal variableRewardBaseChance;

    @Positive
    @Column(name = "variable_reward_increment", precision = 5, scale = 2)
    private BigDecimal variableRewardIncrement;

    @Positive
    @Column(name = "variable_reward_threshold", precision = 19, scale = 2)
    private BigDecimal variableRewardThreshold;

    @Version
    private Long version;

    // Enums
    public enum ContributionType {
        FIXED, VARIABLE
    }

    public enum RewardType {
        FIXED, VARIABLE
    }

    // Constructors
    public Jackpot() {}

    public Jackpot(String jackpotId, BigDecimal initialPoolValue, ContributionType contributionType, RewardType rewardType) {
        this.jackpotId = jackpotId;
        this.initialPoolValue = initialPoolValue;
        this.currentPoolValue = initialPoolValue;
        this.contributionType = contributionType;
        this.rewardType = rewardType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJackpotId() { return jackpotId; }
    public void setJackpotId(String jackpotId) { this.jackpotId = jackpotId; }

    public BigDecimal getInitialPoolValue() { return initialPoolValue; }
    public void setInitialPoolValue(BigDecimal initialPoolValue) { this.initialPoolValue = initialPoolValue; }

    public BigDecimal getCurrentPoolValue() { return currentPoolValue; }
    public void setCurrentPoolValue(BigDecimal currentPoolValue) { this.currentPoolValue = currentPoolValue; }

    public ContributionType getContributionType() { return contributionType; }
    public void setContributionType(ContributionType contributionType) { this.contributionType = contributionType; }

    public BigDecimal getFixedContributionPercentage() { return fixedContributionPercentage; }
    public void setFixedContributionPercentage(BigDecimal fixedContributionPercentage) { this.fixedContributionPercentage = fixedContributionPercentage; }

    public BigDecimal getVariableContributionBasePercentage() { return variableContributionBasePercentage; }
    public void setVariableContributionBasePercentage(BigDecimal variableContributionBasePercentage) { this.variableContributionBasePercentage = variableContributionBasePercentage; }

    public BigDecimal getVariableContributionDecayRate() { return variableContributionDecayRate; }
    public void setVariableContributionDecayRate(BigDecimal variableContributionDecayRate) { this.variableContributionDecayRate = variableContributionDecayRate; }

    public RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardType rewardType) { this.rewardType = rewardType; }

    public BigDecimal getFixedRewardChance() { return fixedRewardChance; }
    public void setFixedRewardChance(BigDecimal fixedRewardChance) { this.fixedRewardChance = fixedRewardChance; }

    public BigDecimal getVariableRewardBaseChance() { return variableRewardBaseChance; }
    public void setVariableRewardBaseChance(BigDecimal variableRewardBaseChance) { this.variableRewardBaseChance = variableRewardBaseChance; }

    public BigDecimal getVariableRewardIncrement() { return variableRewardIncrement; }
    public void setVariableRewardIncrement(BigDecimal variableRewardIncrement) { this.variableRewardIncrement = variableRewardIncrement; }

    public BigDecimal getVariableRewardThreshold() { return variableRewardThreshold; }
    public void setVariableRewardThreshold(BigDecimal variableRewardThreshold) { this.variableRewardThreshold = variableRewardThreshold; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}