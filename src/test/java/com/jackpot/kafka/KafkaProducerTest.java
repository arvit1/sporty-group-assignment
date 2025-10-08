package com.jackpot.kafka;

import com.jackpot.dto.BetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, BetRequest> kafkaTemplate;

    @InjectMocks
    private KafkaProducer kafkaProducer;

    @Test
    void testSendBet_Success() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
        SendResult<String, BetRequest> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, BetRequest>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("jackpot-bets"), eq("bet123"), eq(betRequest))).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, BetRequest>> result = kafkaProducer.sendBet(betRequest);

        // Assert
        assertNotNull(result);
        verify(kafkaTemplate).send("jackpot-bets", "bet123", betRequest);
    }

    @Test
    void testSendBet_Failure() {
        // Arrange
        BetRequest betRequest = new BetRequest("bet123", "user456", "jackpot-fixed", BigDecimal.valueOf(100));
        RuntimeException exception = new RuntimeException("Kafka error");
        CompletableFuture<SendResult<String, BetRequest>> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(kafkaTemplate.send(eq("jackpot-bets"), eq("bet123"), eq(betRequest))).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, BetRequest>> result = kafkaProducer.sendBet(betRequest);

        // Assert
        assertNotNull(result);
        verify(kafkaTemplate).send("jackpot-bets", "bet123", betRequest);
    }
}