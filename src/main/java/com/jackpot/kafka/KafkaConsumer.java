package com.jackpot.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
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
  public void consumeBet(BetRequest betRequest) {
    try {
      logger.info("Received bet from Kafka: {}", betRequest);

      // Process the bet contribution
      jackpotService.processContribution(
          betRequest.betId(),
          betRequest.userId(),
          betRequest.jackpotId(),
          betRequest.betAmount()
      );

      logger.info("Successfully processed bet contribution: {}", betRequest.betId());

    } catch (Exception e) {
      logger.error("Failed to process bet from Kafka: {}", betRequest.betId(), e);
    }
  }
}