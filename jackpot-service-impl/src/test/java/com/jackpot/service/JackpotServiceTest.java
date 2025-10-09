package com.jackpot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jackpot.model.Contribution;
import com.jackpot.model.Jackpot;
import com.jackpot.model.Reward;
import com.jackpot.repository.ContributionRepository;
import com.jackpot.repository.JackpotRepository;
import com.jackpot.repository.RewardRepository;
import com.jackpot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JackpotServiceTest {

  @Mock
  private ContributionRepository contributionRepository;
  private Jackpot fixedJackpot;
  @Mock
  private JackpotRepository jackpotRepository;
  @InjectMocks
  private JackpotService jackpotService;
  @Mock
  private RewardRepository rewardRepository;
  @Mock
  private UserRepository userRepository;
  private Jackpot variableJackpot;

  @BeforeEach
  void setUp() {
    // Setup fixed jackpot
    fixedJackpot = new Jackpot("jackpot-fixed", BigDecimal.valueOf(1000),
        Jackpot.ContributionType.FIXED, Jackpot.RewardType.FIXED);
    fixedJackpot.setFixedContributionPercentage(BigDecimal.valueOf(5));
    fixedJackpot.setFixedRewardChance(BigDecimal.valueOf(10));

    // Setup variable jackpot
    variableJackpot = new Jackpot("jackpot-variable", BigDecimal.valueOf(2000),
        Jackpot.ContributionType.VARIABLE, Jackpot.RewardType.VARIABLE);
    variableJackpot.setVariableContributionBasePercentage(BigDecimal.valueOf(10));
    variableJackpot.setVariableContributionDecayRate(BigDecimal.valueOf(0.1));
    variableJackpot.setVariableRewardBaseChance(BigDecimal.valueOf(5));
    variableJackpot.setVariableRewardIncrement(BigDecimal.valueOf(0.5));
    variableJackpot.setVariableRewardThreshold(BigDecimal.valueOf(10000));
  }

  @Test
  void testEvaluateReward_AlreadyHasReward() {
    // Arrange
    Reward existingReward = new Reward("bet123", 456L, "jackpot-fixed", BigDecimal.valueOf(1000));
    when(rewardRepository.existsByBetId("bet123")).thenReturn(true);
    when(rewardRepository.findByBetId("bet123")).thenReturn(Optional.of(existingReward));
    // The implementation validates jackpot, user, and bet existence first
    when(jackpotRepository.existsByJackpotId("jackpot-fixed")).thenReturn(true);
    when(userRepository.existsById(456L)).thenReturn(true);
    when(contributionRepository.existsByBetId("bet123")).thenReturn(true);

    // Act
    Optional<Reward> result = jackpotService.evaluateReward("bet123", 456L, "jackpot-fixed");

    // Assert
    assertTrue(result.isPresent());
    assertEquals(existingReward, result.get());
    verify(jackpotRepository).existsByJackpotId("jackpot-fixed");
    verify(userRepository).existsById(456L);
    verify(contributionRepository).existsByBetId("bet123");
    verify(rewardRepository).existsByBetId("bet123");
    verify(rewardRepository).findByBetId("bet123");
    verifyNoMoreInteractions(jackpotRepository, userRepository, contributionRepository, rewardRepository);
  }

  @Test
  void testEvaluateReward_BetNotFound() {
    // Arrange
    when(jackpotRepository.existsByJackpotId("jackpot-fixed")).thenReturn(true);
    when(userRepository.existsById(456L)).thenReturn(true);
    when(contributionRepository.existsByBetId("non-existent-bet")).thenReturn(false);

    // Act
    Optional<Reward> result = jackpotService.evaluateReward("non-existent-bet", 456L, "jackpot-fixed");

    // Assert
    assertTrue(result.isEmpty());
    verify(jackpotRepository).existsByJackpotId("jackpot-fixed");
    verify(userRepository).existsById(456L);
    verify(contributionRepository).existsByBetId("non-existent-bet");
    verifyNoMoreInteractions(jackpotRepository, userRepository, contributionRepository);
    // Note: rewardRepository.existsByBetId is not called when bet validation fails
  }

  @Test
  void testEvaluateReward_JackpotNotFound() {
    // Arrange
    when(jackpotRepository.existsByJackpotId("non-existent")).thenReturn(false);

    // Act
    Optional<Reward> result = jackpotService.evaluateReward("bet123", 456L, "non-existent");

    // Assert
    assertTrue(result.isEmpty());
    verify(jackpotRepository).existsByJackpotId("non-existent");
    verifyNoMoreInteractions(jackpotRepository);
    // Note: rewardRepository.existsByBetId is not called when jackpot validation fails
  }

  @Test
  void testEvaluateReward_UserNotFound() {
    // Arrange
    when(jackpotRepository.existsByJackpotId("jackpot-fixed")).thenReturn(true);
    when(userRepository.existsById(999L)).thenReturn(false);

    // Act
    Optional<Reward> result = jackpotService.evaluateReward("bet123", 999L, "jackpot-fixed");

    // Assert
    assertTrue(result.isEmpty());
    verify(jackpotRepository).existsByJackpotId("jackpot-fixed");
    verify(userRepository).existsById(999L);
    verifyNoMoreInteractions(jackpotRepository, userRepository);
    // Note: rewardRepository.existsByBetId is not called when user validation fails
  }

  @Test
  void testEvaluateReward_ValidationPasses() {
    // Arrange
    when(jackpotRepository.existsByJackpotId("jackpot-fixed")).thenReturn(true);
    when(userRepository.existsById(456L)).thenReturn(true);
    when(contributionRepository.existsByBetId("bet123")).thenReturn(true);
    when(rewardRepository.existsByBetId("bet123")).thenReturn(false);
    when(jackpotRepository.findByJackpotIdWithLock("jackpot-fixed")).thenReturn(Optional.of(fixedJackpot));
    when(rewardRepository.existsByJackpotId("jackpot-fixed")).thenReturn(false);

    // Act
    Optional<Reward> result = jackpotService.evaluateReward("bet123", 456L, "jackpot-fixed");

    // Assert
    // This test verifies that validation passes and the method proceeds to check for jackpot winner
    // The actual outcome depends on random chance, but we can verify the validation logic worked
    verify(jackpotRepository).existsByJackpotId("jackpot-fixed");
    verify(userRepository).existsById(456L);
    verify(contributionRepository).existsByBetId("bet123");
    verify(rewardRepository).existsByBetId("bet123");
    verify(jackpotRepository).findByJackpotIdWithLock("jackpot-fixed");
    verify(rewardRepository).existsByJackpotId("jackpot-fixed");
  }

  @Test
  void testGetContribution() {
    // Arrange
    Contribution contribution = new Contribution("bet123", 456L, "jackpot-fixed",
        BigDecimal.valueOf(100), BigDecimal.valueOf(5), BigDecimal.valueOf(1005));
    when(contributionRepository.findByBetId("bet123")).thenReturn(Optional.of(contribution));

    // Act
    Optional<Contribution> result = jackpotService.getContribution("bet123");

    // Assert
    assertTrue(result.isPresent());
    assertEquals(contribution, result.get());
    verify(contributionRepository).findByBetId("bet123");
  }

  @Test
  void testGetJackpot() {
    // Arrange
    when(jackpotRepository.findByJackpotId("jackpot-fixed")).thenReturn(Optional.of(fixedJackpot));

    // Act
    Optional<Jackpot> result = jackpotService.getJackpot("jackpot-fixed");

    // Assert
    assertTrue(result.isPresent());
    assertEquals(fixedJackpot, result.get());
    verify(jackpotRepository).findByJackpotId("jackpot-fixed");
  }

  @Test
  void testGetReward() {
    // Arrange
    Reward reward = new Reward("bet123", 456L, "jackpot-fixed", BigDecimal.valueOf(1000));
    when(rewardRepository.findByBetId("bet123")).thenReturn(Optional.of(reward));

    // Act
    Optional<Reward> result = jackpotService.getReward("bet123");

    // Assert
    assertTrue(result.isPresent());
    assertEquals(reward, result.get());
    verify(rewardRepository).findByBetId("bet123");
  }

  @Test
  void testProcessContribution_FixedContribution() {
    // Arrange
    when(jackpotRepository.findByJackpotIdWithLock(anyString())).thenReturn(Optional.of(fixedJackpot));
    when(jackpotRepository.save(any(Jackpot.class))).thenReturn(fixedJackpot);
    when(contributionRepository.save(any(Contribution.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Contribution result = jackpotService.processContribution(
        "bet123", 456L, "jackpot-fixed", BigDecimal.valueOf(100)
    );

    // Assert
    assertNotNull(result);
    assertEquals("bet123", result.getBetId());
    assertEquals(456L, result.getUserId());
    assertEquals("jackpot-fixed", result.getJackpotId());
    assertEquals(BigDecimal.valueOf(100), result.getStakeAmount());
    assertEquals(BigDecimal.valueOf(5).compareTo(result.getContributionAmount()), 0); // 5% of 100

    verify(jackpotRepository).findByJackpotIdWithLock("jackpot-fixed");
    verify(jackpotRepository).save(fixedJackpot);
    verify(contributionRepository).save(any(Contribution.class));
  }

  @Test
  void testProcessContribution_JackpotNotFound() {
    // Arrange
    when(jackpotRepository.findByJackpotIdWithLock(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      jackpotService.processContribution("bet123", 456L, "non-existent", BigDecimal.valueOf(100));
    });

    assertEquals("Jackpot not found: non-existent", exception.getMessage());
    verify(jackpotRepository).findByJackpotIdWithLock("non-existent");
    verifyNoMoreInteractions(jackpotRepository, contributionRepository);
  }

  @Test
  void testProcessContribution_VariableContribution() {
    // Arrange
    when(jackpotRepository.findByJackpotIdWithLock(anyString())).thenReturn(Optional.of(variableJackpot));
    when(jackpotRepository.save(any(Jackpot.class))).thenReturn(variableJackpot);
    when(contributionRepository.save(any(Contribution.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Contribution result = jackpotService.processContribution(
        "bet123", 456L, "jackpot-variable", BigDecimal.valueOf(100)
    );

    // Assert
    assertNotNull(result);
    assertEquals("bet123", result.getBetId());
    assertEquals(456L, result.getUserId());
    assertEquals("jackpot-variable", result.getJackpotId());

    // Variable contribution calculation: 10% - (0.1 * 2000/1000) = 10% - 0.2% = 9.8%
    // 9.8% of 100 = 9.80
    assertEquals(BigDecimal.valueOf(9.80).compareTo(result.getContributionAmount()), 0);

    verify(jackpotRepository).findByJackpotIdWithLock("jackpot-variable");
    verify(jackpotRepository).save(variableJackpot);
    verify(contributionRepository).save(any(Contribution.class));
  }
}