package com.jackpot.controller;

import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.kafka.KafkaProducer;
import com.jackpot.service.JackpotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetControllerTest {

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private JackpotService jackpotService;

    @InjectMocks
    private BetController betController;

    @Test
    void testPublishBet_Success() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
        SendResult<String, BetRequest> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, BetRequest>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaProducer.sendBet(any(BetRequest.class))).thenReturn(future);

        // Act
        CompletableFuture<ResponseEntity<BetResponse>> result = betController.publishBet(betRequest);

        // Assert
        assertNotNull(result);

        // Wait for completion and verify response
        ResponseEntity<BetResponse> response = result.join();
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("bet123", response.getBody().betId());
        assertEquals("PROCESSED", response.getBody().status());

        verify(kafkaProducer).sendBet(betRequest);
    }

    @Test
    void testPublishBet_Failure() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
        RuntimeException exception = new RuntimeException("Kafka error");
        CompletableFuture<SendResult<String, BetRequest>> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(kafkaProducer.sendBet(any(BetRequest.class))).thenReturn(future);

        // Act
        CompletableFuture<ResponseEntity<BetResponse>> result = betController.publishBet(betRequest);

        // Assert
        assertNotNull(result);

        // Wait for completion and verify error response
        ResponseEntity<BetResponse> response = result.join();
        assertEquals(500, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("bet123", response.getBody().betId());
        assertEquals("ERROR", response.getBody().status());
        assertTrue(response.getBody().message().contains("Failed to publish bet to Kafka"));

        verify(kafkaProducer).sendBet(betRequest);
    }
}