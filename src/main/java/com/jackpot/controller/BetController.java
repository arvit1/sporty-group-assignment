package com.jackpot.controller;

import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.kafka.KafkaProducer;
import com.jackpot.service.JackpotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/bets")
public class BetController {

    private static final Logger logger = LoggerFactory.getLogger(BetController.class);

    private final KafkaProducer kafkaProducer;
    private final JackpotService jackpotService;

    public BetController(KafkaProducer kafkaProducer, JackpotService jackpotService) {
        this.kafkaProducer = kafkaProducer;
        this.jackpotService = jackpotService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<BetResponse>> publishBet(@Valid @RequestBody BetRequest betRequest) {
        logger.info("Received bet request: {}", betRequest);

        return kafkaProducer.sendBet(betRequest)
                .thenApply(result -> {
                    BetResponse response = new BetResponse(
                            betRequest.betId(),
                            "PROCESSED",
                            "Bet successfully published to Kafka for processing"
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to publish bet to Kafka: {}", betRequest.betId(), ex);
                    BetResponse response = new BetResponse(
                            betRequest.betId(),
                            "ERROR",
                            "Failed to publish bet to Kafka: " + ex.getMessage()
                    );
                    return ResponseEntity.internalServerError().body(response);
                });
    }

    @GetMapping("/{betId}/contribution")
    public ResponseEntity<?> getContribution(@PathVariable String betId) {
        return jackpotService.getContribution(betId)
                .map(contribution -> ResponseEntity.ok(contribution))
                .orElse(ResponseEntity.notFound().build());
    }
}