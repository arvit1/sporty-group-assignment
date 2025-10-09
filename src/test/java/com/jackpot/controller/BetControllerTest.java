package com.jackpot.controller;

import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.kafka.KafkaProducer;
import com.jackpot.model.Contribution;
import com.jackpot.model.User;
import com.jackpot.security.CustomUserDetails;
import com.jackpot.service.JackpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetControllerTest {

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private JackpotService jackpotService;

    @InjectMocks
    private BetController betController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup test user
        User testUser = new User();
        testUser.setId(123L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        userDetails = new CustomUserDetails(testUser);
    }

    @Test
    void testPublishBet_SuccessWithCustomUserDetails() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(100));

        // Act
        ResponseEntity<BetResponse> response = betController.publishBet(betRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("bet123", response.getBody().betId());
        assertEquals("PROCESSED", response.getBody().status());

        verify(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));
    }

    @Test
    void testPublishBet_FailureWithCustomUserDetails() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(100));
        RuntimeException exception = new RuntimeException("Kafka error");
        doThrow(exception).when(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));

        // Act
        ResponseEntity<BetResponse> response = betController.publishBet(betRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("bet123", response.getBody().betId());
        assertEquals("ERROR", response.getBody().status());
        assertTrue(response.getBody().message().contains("Failed to publish bet to Kafka"));

        verify(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));
    }

    @Test
    void testPublishBet_UserNotAuthenticated() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(100));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
            () -> betController.publishBet(betRequest, null));

        assertEquals("User not authenticated", exception.getMessage());
        verify(kafkaProducer, never()).sendBet(any(BetRequest.class), any(Long.class));
    }

    @Test
    void testPublishBet_EdgeCase_MinimumBetAmount() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(0.01));

        // Act
        ResponseEntity<BetResponse> response = betController.publishBet(betRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));
    }

    @Test
    void testPublishBet_EdgeCase_MaximumBetAmount() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(1000000));

        // Act
        ResponseEntity<BetResponse> response = betController.publishBet(betRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));
    }

    @Test
    void testPublishBet_Security_UserCannotOverrideUserId() {
        // Arrange
        // This test ensures that even if someone tries to send a userId in the request body,
        // it gets overridden by the authenticated user's ID
        BetRequest betRequest = new BetRequest("bet123", "jackpot-fixed", BigDecimal.valueOf(100));

        // Act
        ResponseEntity<BetResponse> response = betController.publishBet(betRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        // Verify that the user ID 123L (from authentication) is used, not any value from request
        verify(kafkaProducer).sendBet(any(BetRequest.class), eq(123L));
    }

    @Test
    void testGetContribution_Success() {
        // Arrange
        String betId = "bet123";
        Contribution expectedContribution = new Contribution(betId, 456L, "jackpot-fixed",
            BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(1000));
        when(jackpotService.getContribution(betId)).thenReturn(java.util.Optional.of(expectedContribution));

        // Act
        ResponseEntity<?> response = betController.getContribution(betId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedContribution, response.getBody());
        verify(jackpotService).getContribution(betId);
    }

    @Test
    void testGetContribution_NotFound() {
        // Arrange
        String betId = "nonexistent";
        when(jackpotService.getContribution(betId)).thenReturn(java.util.Optional.empty());

        // Act
        ResponseEntity<?> response = betController.getContribution(betId);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
        verify(jackpotService).getContribution(betId);
    }
}