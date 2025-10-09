package com.jackpot.service;

import com.jackpot.model.Contribution;
import com.jackpot.model.Jackpot;
import com.jackpot.model.Reward;
import com.jackpot.repository.ContributionRepository;
import com.jackpot.repository.JackpotRepository;
import com.jackpot.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JackpotServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private JackpotService jackpotService;

    private Jackpot fixedJackpot;
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
    void testEvaluateReward_AlreadyHasReward() {
        // Arrange
        Reward existingReward = new Reward("bet123", 456L, "jackpot-fixed", BigDecimal.valueOf(1000));
        when(rewardRepository.existsByBetId("bet123")).thenReturn(true);
        when(rewardRepository.findByBetId("bet123")).thenReturn(Optional.of(existingReward));

        // Act
        Optional<Reward> result = jackpotService.evaluateReward("bet123", 456L, "jackpot-fixed");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existingReward, result.get());
        verify(rewardRepository).existsByBetId("bet123");
        verify(rewardRepository).findByBetId("bet123");
        verifyNoInteractions(jackpotRepository);
    }

    @Test
    void testEvaluateReward_JackpotNotFound() {
        // Arrange
        when(rewardRepository.existsByBetId("bet123")).thenReturn(false);
        when(jackpotRepository.findByJackpotIdWithLock(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jackpotService.evaluateReward("bet123", 456L, "non-existent");
        });

        assertEquals("Jackpot not found: non-existent", exception.getMessage());
        verify(rewardRepository).existsByBetId("bet123");
        verify(jackpotRepository).findByJackpotIdWithLock("non-existent");
        verifyNoMoreInteractions(rewardRepository, jackpotRepository);
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
}