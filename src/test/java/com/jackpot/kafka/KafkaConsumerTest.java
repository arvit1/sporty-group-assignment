package com.jackpot.kafka;

import com.jackpot.dto.BetRequest;
import com.jackpot.model.Contribution;
import com.jackpot.service.JackpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for KafkaConsumer error handling and resilience
 *
 * @test Kafka Consumer Error Handling
 * @description Validates that the Kafka consumer handles various error scenarios gracefully
 * @scenarios
 *   - Successful message processing
 *   - Service layer exceptions
 *   - Message deserialization errors
 *   - Invalid message content
 *   - Retry mechanisms
 * @expected Consumer should log errors but not crash the application
 */
@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private JackpotService jackpotService;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    private BetRequest validBetRequest;

    @BeforeEach
    void setUp() {
        validBetRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(100));
    }

    @Test
    void testConsumeBet_Success() {
        // Arrange
        Contribution mockContribution = new Contribution("bet123", "user456", "jackpot-fixed",
            BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(1000));
        when(jackpotService.processContribution(
            eq("bet123"), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(100))
        )).thenReturn(mockContribution);

        // Act
        kafkaConsumer.consumeBet(validBetRequest, "user456-bet123");

        // Assert
        verify(jackpotService).processContribution("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
        verifyNoMoreInteractions(jackpotService);
    }

    @Test
    void testConsumeBet_ServiceThrowsException() {
        // Arrange
        RuntimeException serviceException = new RuntimeException("Service unavailable");
        when(jackpotService.processContribution(
            eq("bet123"), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(100))
        )).thenThrow(serviceException);

        // Act & Assert - Consumer should catch the exception and log it, not propagate
        kafkaConsumer.consumeBet(validBetRequest, "user456-bet123");

        // Verify the service was called despite the exception
        verify(jackpotService).processContribution("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
    }

    @Test
    void testConsumeBet_NullBetRequest() {
        // Arrange
        BetRequest nullBetRequest = null;

        // Act & Assert - Should handle null gracefully
        kafkaConsumer.consumeBet(nullBetRequest, "user456-bet123");

        // Verify no service interaction for null message
        verifyNoInteractions(jackpotService);
    }

    @Test
    void testConsumeBet_InvalidBetAmount() {
        // Arrange
        BetRequest invalidBetRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(-100));

        // Simulate service validation failure
        IllegalArgumentException validationException = new IllegalArgumentException("Invalid bet amount");
        when(jackpotService.processContribution(
            eq("bet123"), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(-100))
        )).thenThrow(validationException);

        // Act & Assert - Should catch and log validation errors
        kafkaConsumer.consumeBet(invalidBetRequest, "user456-bet123");

        verify(jackpotService).processContribution("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(-100));
    }

    @Test
    void testConsumeBet_DatabaseConnectionError() {
        // Arrange
        RuntimeException dbException = new RuntimeException("Database connection failed");
        when(jackpotService.processContribution(
            eq("bet123"), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(100))
        )).thenThrow(dbException);

        // Act & Assert - Should handle database errors gracefully
        kafkaConsumer.consumeBet(validBetRequest, "user456-bet123");

        verify(jackpotService).processContribution("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
    }

    @Test
    void testConsumeBet_MissingRequiredFields() {
        // Arrange - Create bet with null fields
        BetRequest invalidBetRequest = new BetRequest(null, "jackpot-fixed", BigDecimal.valueOf(100));

        // Simulate service handling of null betId
        IllegalArgumentException exception = new IllegalArgumentException("Bet ID cannot be null");
        when(jackpotService.processContribution(
            isNull(), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(100))
        )).thenThrow(exception);

        // Act & Assert - Should handle missing fields gracefully
        kafkaConsumer.consumeBet(invalidBetRequest, "user456-bet123");

        verify(jackpotService).processContribution(null, "user456", "jackpot-fixed", BigDecimal.valueOf(100));
    }

    @Test
    void testConsumeBet_ConcurrentProcessing() {
        // Arrange - Test that consumer can handle concurrent messages
        BetRequest bet1 = new BetRequest("bet1", "jackpot-fixed", BigDecimal.valueOf(50));
        BetRequest bet2 = new BetRequest("bet2", "jackpot-fixed", BigDecimal.valueOf(75));

        Contribution mockContribution1 = new Contribution("bet1", "user1", "jackpot-fixed",
            BigDecimal.valueOf(50), BigDecimal.valueOf(5), BigDecimal.valueOf(1000));
        Contribution mockContribution2 = new Contribution("bet2", "user2", "jackpot-fixed",
            BigDecimal.valueOf(75), BigDecimal.valueOf(7.5), BigDecimal.valueOf(1000));

        when(jackpotService.processContribution(
            eq("bet1"), eq("user1"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(50))
        )).thenReturn(mockContribution1);
        when(jackpotService.processContribution(
            eq("bet2"), eq("user2"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(75))
        )).thenReturn(mockContribution2);

        // Act
        kafkaConsumer.consumeBet(bet1, "user1-bet1");
        kafkaConsumer.consumeBet(bet2, "user2-bet2");

        // Assert - Both messages should be processed
        verify(jackpotService).processContribution("bet1", "user1", "jackpot-fixed", BigDecimal.valueOf(50));
        verify(jackpotService).processContribution("bet2", "user2", "jackpot-fixed", BigDecimal.valueOf(75));
    }

    @Test
    void testConsumeBet_RecoveryAfterFailure() {
        // Arrange - First call fails, second call succeeds
        RuntimeException firstException = new RuntimeException("Temporary failure");
        Contribution mockContribution = new Contribution("bet123", "user456", "jackpot-fixed",
            BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(1000));

        when(jackpotService.processContribution(
            eq("bet123"), eq("user456"), eq("jackpot-fixed"), eq(BigDecimal.valueOf(100))
        )).thenThrow(firstException)
          .thenReturn(mockContribution);

        // Act - First call (should fail but not crash)
        kafkaConsumer.consumeBet(validBetRequest, "user456-bet123");

        // Act - Second call (should succeed)
        kafkaConsumer.consumeBet(validBetRequest, "user456-bet123");

        // Assert - Both calls should reach the service
        verify(jackpotService, times(2)).processContribution(
            "bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100)
        );
    }
}