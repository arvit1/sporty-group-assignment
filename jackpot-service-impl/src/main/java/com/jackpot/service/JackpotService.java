package com.jackpot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jackpot.model.Contribution;
import com.jackpot.model.Jackpot;
import com.jackpot.model.Reward;
import com.jackpot.repository.ContributionRepository;
import com.jackpot.repository.JackpotRepository;
import com.jackpot.repository.RewardRepository;
import com.jackpot.repository.UserRepository;

@Service
public class JackpotService {

  private final ContributionRepository contributionRepository;
  @Value("${jackpot.force-win:false}")
  private boolean forceWin;
  private final JackpotRepository jackpotRepository;
  private final Random random = new Random();
  private final RewardRepository rewardRepository;
  private final UserRepository userRepository;

  public JackpotService(JackpotRepository jackpotRepository,
      ContributionRepository contributionRepository,
      RewardRepository rewardRepository,
      UserRepository userRepository) {
    this.jackpotRepository = jackpotRepository;
    this.contributionRepository = contributionRepository;
    this.rewardRepository = rewardRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Optional<Reward> evaluateReward(String betId, Long userId, String jackpotId) {
    // Validate bet, user, and jackpot existence
    if (!validateBetUserAndJackpot(betId, userId, jackpotId)) {
      return Optional.empty();
    }

    // Check if bet already has a reward
    if (rewardRepository.existsByBetId(betId)) {
      return rewardRepository.findByBetId(betId);
    }

    // Find jackpot with optimistic locking
    Optional<Jackpot> jackpotOpt = jackpotRepository.findByJackpotIdWithLock(jackpotId);
    if (jackpotOpt.isEmpty()) {
      return Optional.empty();
    }
    Jackpot jackpot = jackpotOpt.get();

    // Check if jackpot already has a winner (enforces single winner per jackpot)
    if (rewardRepository.existsByJackpotId(jackpotId)) {
      return Optional.empty();
    }

    // Calculate reward chance based on jackpot configuration
    double rewardChance = calculateRewardChance(jackpot);

    //  for testing forceWin
    boolean winsJackpot = forceWin || random.nextDouble() * 100 < rewardChance;

    if (winsJackpot) {
      // Double-check no winner was created during race condition
      if (rewardRepository.existsByJackpotId(jackpotId)) {
        return Optional.empty();
      }

      // Create reward
      Reward reward = new Reward(betId, userId, jackpotId, jackpot.getCurrentPoolValue());

      // Reset jackpot to initial value
      jackpot.setCurrentPoolValue(jackpot.getInitialPoolValue());
      jackpotRepository.save(jackpot);

      return Optional.of(rewardRepository.save(reward));
    }

    return Optional.empty();
  }

  public Optional<Contribution> getContribution(String betId) {
    return contributionRepository.findByBetId(betId);
  }

  public Optional<Jackpot> getJackpot(String jackpotId) {
    return jackpotRepository.findByJackpotId(jackpotId);
  }

  public Optional<Reward> getReward(String betId) {
    return rewardRepository.findByBetId(betId);
  }

  @Transactional
  public Contribution processContribution(String betId, Long userId, String jackpotId, BigDecimal betAmount) {
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

  private BigDecimal calculateContributionAmount(Jackpot jackpot, BigDecimal betAmount) {
    switch (jackpot.getContributionType()) {

      case FIXED:
        // Formula:
        // contribution = max(0.01, betAmount * fixedContributionPercentage / 100)

        if (jackpot.getFixedContributionPercentage() == null) {
          throw new IllegalStateException("Fixed contribution percentage not configured for jackpot: " + jackpot.getJackpotId());
        }

        BigDecimal fixedContribution = betAmount.multiply(jackpot.getFixedContributionPercentage())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Ensure minimum contribution of 0.01
        return fixedContribution.max(BigDecimal.valueOf(0.01));

      case VARIABLE:
        // Formula:
        // variablePercentage = clamp(1, 100, base - (decay * pool / 1000))
        // contribution = max(0.01, betAmount * variablePercentage / 100)

        if (jackpot.getVariableContributionBasePercentage() == null || jackpot.getVariableContributionDecayRate() == null) {
          throw new IllegalStateException("Variable contribution parameters not configured for jackpot: " + jackpot.getJackpotId());
        }

        BigDecimal basePercentage = jackpot.getVariableContributionBasePercentage();
        BigDecimal decayRate = jackpot.getVariableContributionDecayRate();
        BigDecimal poolSize = jackpot.getCurrentPoolValue();

        // variablePercentage = base - (decay * pool / 1000)
        BigDecimal variablePercentage = basePercentage.subtract(
            decayRate.multiply(poolSize.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP))
        );

        // Clamp variablePercentage to range [1, 100]
        variablePercentage = variablePercentage.min(BigDecimal.valueOf(100)).max(BigDecimal.ONE);

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

  private void validateBetParameters(String betId, Long userId, String jackpotId, BigDecimal betAmount) {
    if (betId == null || betId.trim().isEmpty()) {
      throw new IllegalArgumentException("Bet ID cannot be null or empty");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
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

  private boolean validateBetUserAndJackpot(String betId, Long userId, String jackpotId) {
    // Validate that jackpot exists
    if (!jackpotRepository.existsByJackpotId(jackpotId)) {
      return false;
    }

    // Validate that user exists
    if (!userRepository.existsById(userId)) {
      return false;
    }

    // Validate that bet exists (has a contribution)
    if (!contributionRepository.existsByBetId(betId)) {
      return false;
    }

    return true;
  }
}