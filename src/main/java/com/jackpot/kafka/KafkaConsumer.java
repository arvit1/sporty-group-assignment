package com.jackpot.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.jackpot.dto.BetRequest;
import com.jackpot.service.JackpotService;

@Component
public class KafkaConsumer {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

  private final JackpotService jackpotService;

  public KafkaConsumer(JackpotService jackpotService) {
    this.jackpotService = jackpotService;
  }

  @KafkaListener(topics = "jackpot-bets", groupId = "jackpot-service-group")
  public void consumeBet(BetRequest betRequest, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    try {
      if (betRequest == null) {
        logger.warn("Received null bet request from Kafka - skipping processing");
        return;
      }

      logger.info("Received bet from Kafka with key '{}': {}", key, betRequest);

      // Extract userId from the composite key (format: userId-betId)
      Long userId = extractUserIdFromKey(key, betRequest.betId());

      // Process the bet contribution
      jackpotService.processContribution(
          betRequest.betId(),
          userId,
          betRequest.jackpotId(),
          betRequest.betAmount()
      );

      logger.info("Successfully processed bet contribution: {} for user {}", betRequest.betId(), userId);

    } catch (Exception e) {
      logger.error("Failed to process bet from Kafka: {}", betRequest != null ? betRequest.betId() : "null", e);
    }
  }

  private Long extractUserIdFromKey(String key, String betId) {
    if (key == null || !key.contains("-")) {
      throw new IllegalArgumentException("Invalid Kafka message key format. Expected format: userId-betId");
    }

    // Extract userId from composite key (format: userId-betId)
    String userId = key.substring(0, key.indexOf("-" + betId));

    if (userId.isEmpty()) {
      throw new IllegalArgumentException("User ID cannot be empty in Kafka message key");
    }

    return Long.valueOf(userId);
  }
}