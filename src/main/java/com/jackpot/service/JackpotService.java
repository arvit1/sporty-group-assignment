package com.jackpot.service;

import com.jackpot.model.Contribution;
import com.jackpot.model.Jackpot;
import com.jackpot.model.Reward;
import com.jackpot.repository.ContributionRepository;
import com.jackpot.repository.JackpotRepository;
import com.jackpot.repository.RewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Random;

@Service
public class JackpotService {

    private final JackpotRepository jackpotRepository;
    private final ContributionRepository contributionRepository;
    private final RewardRepository rewardRepository;
    private final Random random = new Random();

    public JackpotService(JackpotRepository jackpotRepository,
                         ContributionRepository contributionRepository,
                         RewardRepository rewardRepository) {
        this.jackpotRepository = jackpotRepository;
        this.contributionRepository = contributionRepository;
        this.rewardRepository = rewardRepository;
    }

    @Transactional
    public Contribution processContribution(String betId, String userId, String jackpotId, BigDecimal betAmount) {
        // Validate input parameters
        validateBetParameters(betId, userId, jackpotId, betAmount);

        // Find jackpot with optimistic locking
        Jackpot jackpot = jackpotRepository.findByJackpotIdWithLock(jackpotId)
                .orElseThrow(() -> new RuntimeException("Jackpot not found: " + jackpotId));

        // Calculate contribution amount based on jackpot configuration
        BigDecimal contributionAmount = calculateContributionAmount(jackpot, betAmount);

        // Update jackpot pool
        jackpot.setCurrentPoolValue(jackpot.getCurrentPoolValue().add(contributionAmount));
        jackpot = jackpotRepository.save(jackpot);

        // Create contribution record
        Contribution contribution = new Contribution(
                betId, userId, jackpotId, betAmount, contributionAmount, jackpot.getCurrentPoolValue()
        );

        return contributionRepository.save(contribution);
    }

    private void validateBetParameters(String betId, String userId, String jackpotId, BigDecimal betAmount) {
        if (betId == null || betId.trim().isEmpty()) {
            throw new IllegalArgumentException("Bet ID cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (jackpotId == null || jackpotId.trim().isEmpty()) {
            throw new IllegalArgumentException("Jackpot ID cannot be null or empty");
        }
        if (betAmount == null) {
            throw new IllegalArgumentException("Bet amount cannot be null");
        }
        if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }
    }

    @Transactional
    public Optional<Reward> evaluateReward(String betId, String userId, String jackpotId) {
        // Check if bet already has a reward
        if (rewardRepository.existsByBetId(betId)) {
            return rewardRepository.findByBetId(betId);
        }

        // Find jackpot with optimistic locking
        Jackpot jackpot = jackpotRepository.findByJackpotIdWithLock(jackpotId)
                .orElseThrow(() -> new RuntimeException("Jackpot not found: " + jackpotId));

        // Calculate reward chance based on jackpot configuration
        double rewardChance = calculateRewardChance(jackpot);

        // Determine if bet wins
        boolean winsJackpot = random.nextDouble() * 100 < rewardChance;

        if (winsJackpot) {
            // Create reward
            Reward reward = new Reward(betId, userId, jackpotId, jackpot.getCurrentPoolValue());

            // Reset jackpot to initial value
            jackpot.setCurrentPoolValue(jackpot.getInitialPoolValue());
            jackpotRepository.save(jackpot);

            return Optional.of(rewardRepository.save(reward));
        }

        return Optional.empty();
    }

    private BigDecimal calculateContributionAmount(Jackpot jackpot, BigDecimal betAmount) {
        switch (jackpot.getContributionType()) {
            case FIXED:
                if (jackpot.getFixedContributionPercentage() == null) {
                    throw new IllegalStateException("Fixed contribution percentage not configured for jackpot: " + jackpot.getJackpotId());
                }
                BigDecimal fixedContribution = betAmount.multiply(jackpot.getFixedContributionPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Ensure minimum contribution of 0.01
                return fixedContribution.max(BigDecimal.valueOf(0.01));

            case VARIABLE:
                if (jackpot.getVariableContributionBasePercentage() == null || jackpot.getVariableContributionDecayRate() == null) {
                    throw new IllegalStateException("Variable contribution parameters not configured for jackpot: " + jackpot.getJackpotId());
                }

                BigDecimal basePercentage = jackpot.getVariableContributionBasePercentage();
                BigDecimal decayRate = jackpot.getVariableContributionDecayRate();
                BigDecimal poolSize = jackpot.getCurrentPoolValue();

                // Calculate variable percentage: base - (decay * pool/1000)
                BigDecimal variablePercentage = basePercentage.subtract(
                        decayRate.multiply(poolSize.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP))
                );

                // Ensure minimum contribution of 1% and maximum of 100%
                variablePercentage = variablePercentage.max(BigDecimal.ONE).min(BigDecimal.valueOf(100));

                BigDecimal variableContribution = betAmount.multiply(variablePercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Ensure minimum contribution of 0.01
                return variableContribution.max(BigDecimal.valueOf(0.01));

            default:
                throw new IllegalArgumentException("Unknown contribution type: " + jackpot.getContributionType());
        }
    }

    private double calculateRewardChance(Jackpot jackpot) {
        switch (jackpot.getRewardType()) {
            case FIXED:
                return jackpot.getFixedRewardChance().doubleValue();

            case VARIABLE:
                double baseChance = jackpot.getVariableRewardBaseChance().doubleValue();
                double increment = jackpot.getVariableRewardIncrement().doubleValue();
                double threshold = jackpot.getVariableRewardThreshold().doubleValue();
                double pool = jackpot.getCurrentPoolValue().doubleValue();

                // Linear increase formula: min(100, baseChance + increment * pool/threshold)
                double calculatedChance = baseChance + (increment * pool / threshold);
                return Math.min(100.0, calculatedChance);

            default:
                throw new IllegalArgumentException("Unknown reward type: " + jackpot.getRewardType());
        }
    }

    public Optional<Jackpot> getJackpot(String jackpotId) {
        return jackpotRepository.findByJackpotId(jackpotId);
    }

    public Optional<Contribution> getContribution(String betId) {
        return contributionRepository.findByBetId(betId);
    }

    public Optional<Reward> getReward(String betId) {
        return rewardRepository.findByBetId(betId);
    }
}