package com.jackpot.controller;

import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.kafka.KafkaProducer;
import com.jackpot.security.CustomUserDetails;
import com.jackpot.service.JackpotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BetResponse> publishBet(
            @Valid @RequestBody BetRequest betRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("Received bet request: {}", betRequest);

        if (userDetails == null) {
            throw new SecurityException("User not authenticated");
        }

        Long userId = userDetails.getUserId();
        logger.info("Extracted user ID from authentication: {}", userId);

        try {
            kafkaProducer.sendBet(betRequest, userId);
            BetResponse response = new BetResponse(
                    betRequest.betId(),
                    "PROCESSED",
                    "Bet successfully published to Kafka for processing"
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Failed to publish bet to Kafka: {}", betRequest.betId(), ex);
            BetResponse response = new BetResponse(
                    betRequest.betId(),
                    "ERROR",
                    "Failed to publish bet to Kafka: " + ex.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{betId}/contribution")
    public ResponseEntity<?> getContribution(@PathVariable String betId) {
        return jackpotService.getContribution(betId)
                .map(contribution -> ResponseEntity.ok(contribution))
                .orElse(ResponseEntity.notFound().build());
    }
}