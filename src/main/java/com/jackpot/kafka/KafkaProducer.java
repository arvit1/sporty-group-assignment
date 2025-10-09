package com.jackpot.kafka;

import com.jackpot.dto.BetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private static final String TOPIC = "jackpot-bets";

    private final KafkaTemplate<String, BetRequest> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, BetRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, BetRequest>> sendBet(BetRequest betRequest, String userId) {
        logger.info("Sending bet to Kafka topic '{}' for user '{}': {}", TOPIC, userId, betRequest);

        // Use composite key: userId-betId to ensure user-specific partitioning
        String key = userId + "-" + betRequest.betId();

        return kafkaTemplate.send(TOPIC, key, betRequest)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Successfully sent bet to Kafka: {} for user {}", betRequest.betId(), userId);
                    } else {
                        logger.error("Failed to send bet to Kafka: {} for user {}", betRequest.betId(), userId, ex);
                    }
                });
    }
}