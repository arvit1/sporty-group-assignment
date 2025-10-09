package com.jackpot.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jackpot.dto.RewardResponse;
import com.jackpot.model.Reward;
import com.jackpot.service.JackpotService;

@RestController
@RequestMapping("/api/jackpots")
public class JackpotController {

  private static final Logger logger = LoggerFactory.getLogger(JackpotController.class);

  private final JackpotService jackpotService;

  public JackpotController(JackpotService jackpotService) {
    this.jackpotService = jackpotService;
  }

  @PostMapping("/{jackpotId}/evaluate-reward")
  public ResponseEntity<RewardResponse> evaluateReward(
      @PathVariable String jackpotId,
      @RequestParam String betId,
      @RequestParam Long userId) {

    logger.info("Evaluating reward for bet: {}, user: {}, jackpot: {}", betId, userId, jackpotId);
    try {
      Optional<Reward> reward = jackpotService.evaluateReward(betId, userId, jackpotId);

      if (reward.isPresent()) {
        RewardResponse response = new RewardResponse(
            betId,
            true,
            reward.get().getJackpotRewardAmount(),
            "Congratulations! You won the jackpot!"
        );
        logger.info("Bet {} won jackpot reward: {}", betId, reward.get().getJackpotRewardAmount());
        return ResponseEntity.ok(response);
      } else {
        RewardResponse response = new RewardResponse(
            betId,
            false,
            null,
            "Better luck next time!"
        );
        logger.info("Bet {} did not win jackpot", betId);
        return ResponseEntity.ok(response);
      }

    } catch (Exception e) {
      logger.error("Error evaluating reward for bet: {}", betId, e);
      RewardResponse response = new RewardResponse(
          betId,
          false,
          null,
          "Error evaluating reward: " + e.getMessage()
      );
      return ResponseEntity.internalServerError().body(response);
    }
  }

  @GetMapping("/{jackpotId}")
  public ResponseEntity<?> getJackpot(@PathVariable String jackpotId) {
    return jackpotService.getJackpot(jackpotId)
        .map(jackpot -> ResponseEntity.ok(jackpot))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{jackpotId}/rewards/{betId}")
  public ResponseEntity<?> getReward(@PathVariable String jackpotId, @PathVariable String betId) {
    return jackpotService.getReward(betId)
        .map(reward -> ResponseEntity.ok(reward))
        .orElse(ResponseEntity.notFound().build());
  }
}