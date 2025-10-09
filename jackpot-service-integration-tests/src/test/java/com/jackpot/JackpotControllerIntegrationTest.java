package com.jackpot;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jackpot.model.User;
import com.jackpot.security.CustomUserDetails;
import com.jackpot.security.JwtUtil;

@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class JackpotControllerIntegrationTest extends BaseIntegrationTest {

  private String authToken;
  private final JwtUtil jwtUtil;
  private final MockMvc mockMvc;

  @Autowired
  public JackpotControllerIntegrationTest(JwtUtil jwtUtil, MockMvc mockMvc) {
    this.jwtUtil = jwtUtil;
    this.mockMvc = mockMvc;
  }

  @BeforeEach
  void setUp() throws Exception {
    authToken = jwtUtil.generateToken(
        new CustomUserDetails(new User("testuser", "$2a$10$DantiWQsE41KFDfGwscAoO3rdA.hAyVPrUXexKqM4tIMHT0E2z7Sq", "test@example.com")));
  }

  @ParameterizedTest
  @CsvSource({
      "jackpot-1, 1000.00, 1000.00, FIXED, FIXED",
      "jackpot-2, 2000.00, 2000.00, FIXED, VARIABLE",
      "jackpot-3, 1500.00, 1500.00, VARIABLE, FIXED",
      "jackpot-4, 3000.00, 3000.00, VARIABLE, VARIABLE"
  })
  void testGetJackpotDetails(String jackpotId, double initialPoolValue, double currentPoolValue,
                             String contributionType, String rewardType) throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/" + jackpotId)
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.jackpotId").value(jackpotId))
        .andExpect(jsonPath("$.initialPoolValue").value(initialPoolValue))
        .andExpect(jsonPath("$.currentPoolValue").value(currentPoolValue))
        .andExpect(jsonPath("$.contributionType").value(contributionType))
        .andExpect(jsonPath("$.rewardType").value(rewardType));
  }

  @Test
  void testGetJackpotWithoutAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/jackpot-1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetNonExistentJackpot() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/non-existent-jackpot")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testGetNonExistentReward() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/jackpot-1/rewards/non-existent-bet")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @CsvSource({
      "jackpot-1, bet-1, 100.00",
      "jackpot-2, bet-2, 200.00",
      "jackpot-3, bet-3, 150.00",
      "jackpot-4, bet-4, 300.00"
  })
  void testGetRewardDetails(String jackpotId, String betId, double expectedRewardAmount) throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/" + jackpotId + "/rewards/" + betId)
            .header("Authorization", "Bearer " + authToken))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.betId").value(betId))
        .andExpect(jsonPath("$.jackpotId").value(jackpotId))
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.jackpotRewardAmount").value(expectedRewardAmount));
  }

  @Test
  void testGetRewardWithoutAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/jackpots/jackpot-1/rewards/bet-1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void testRewardEvaluationLossScenario() throws Exception {
    // Given - bet-loss-test doesn't exist in test data, so validation fails

    // When & Then
    MvcResult result = mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("betId", "bet-loss-test")
            .param("userId", "1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.betId").value("bet-loss-test"))
        .andExpect(jsonPath("$.won").value(false))
        .andExpect(jsonPath("$.rewardAmount").isEmpty())
        .andExpect(jsonPath("$.message").value("Better luck next time!"))
        .andReturn();
  }

  @ParameterizedTest
  @CsvSource({
      "jackpot-1, bet-1, 100.00",
      "jackpot-2, bet-2, 200.00",
      "jackpot-3, bet-3, 150.00",
      "jackpot-4, bet-4, 300.00"
  })
  void testRewardEvaluationWinScenario(String jackpotId, String betId, double expectedRewardAmount) throws Exception {
    // Given - betId has a reward in test data

    // When & Then
    MvcResult result = mockMvc.perform(post("/api/jackpots/" + jackpotId + "/evaluate-reward")
            .param("betId", betId)
            .param("userId", "1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.betId").value(betId))
        .andExpect(jsonPath("$.won").value(true))
        .andExpect(jsonPath("$.rewardAmount").value(expectedRewardAmount))
        .andExpect(jsonPath("$.message").value("Congratulations! You won the jackpot!"))
        .andReturn();
  }

  @Test
  void testRewardEvaluationWithInvalidBetId() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("betId", "invalid-bet-id")
            .param("userId", "1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.won").value(false));
  }

  @Test
  void testRewardEvaluationWithInvalidJackpot() throws Exception {
    // When & Then - use bet-2 which has no reward
    mockMvc.perform(post("/api/jackpots/non-existent-jackpot/evaluate-reward")
            .param("betId", "bet-2")
            .param("userId", "1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk()) // Controller handles missing jackpot gracefully
        .andExpect(jsonPath("$.won").value(false));
  }

  @Test
  void testRewardEvaluationWithInvalidUserId() throws Exception {
    // When & Then - use bet-2 which has no reward
    mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("betId", "bet-2")
            .param("userId", "999") // Non-existent user
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.won").value(false));
  }

  @Test
  void testRewardEvaluationWithMissingParameters() throws Exception {
    // When & Then - missing betId
    mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("userId", "1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isBadRequest());

    // When & Then - missing userId
    mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("betId", "bet-1")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testRewardEvaluationWithoutAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/jackpots/jackpot-1/evaluate-reward")
            .param("betId", "bet-1")
            .param("userId", "1"))
        .andExpect(status().isForbidden());
  }
}