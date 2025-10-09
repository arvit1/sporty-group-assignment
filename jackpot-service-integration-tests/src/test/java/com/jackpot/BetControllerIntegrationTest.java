package com.jackpot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.model.User;
import com.jackpot.security.CustomUserDetails;
import com.jackpot.security.JwtUtil;

@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BetControllerIntegrationTest extends BaseIntegrationTest {

  private String authToken;
  private final JwtUtil jwtUtil;
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private BetRequest validBetRequest;

  @Autowired
  public BetControllerIntegrationTest(JwtUtil jwtUtil, MockMvc mockMvc, ObjectMapper objectMapper) {
    this.jwtUtil = jwtUtil;
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  @BeforeEach
  void setUp() throws Exception {
    authToken = jwtUtil.generateToken(
        new CustomUserDetails(new User("testuser", "$2a$10$DantiWQsE41KFDfGwscAoO3rdA.hAyVPrUXexKqM4tIMHT0E2z7Sq", "test@example.com")));
    validBetRequest = new BetRequest("test-bet-1", "jackpot-1", new BigDecimal("50.00"));
  }

  @Test
  void testBetSubmissionWithAuthentication() throws Exception {
    // When & Then
    MvcResult result = mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validBetRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.betId").value("test-bet-1"))
        .andExpect(jsonPath("$.status").value("PROCESSED"))
        .andExpect(jsonPath("$.message").value("Bet successfully published to Kafka for processing"))
        .andReturn();

    // Verify response object
    String responseContent = result.getResponse().getContentAsString();
    BetResponse betResponse = objectMapper.readValue(responseContent, BetResponse.class);

    assertThat(betResponse.betId()).isEqualTo("test-bet-1");
    assertThat(betResponse.status()).isEqualTo("PROCESSED");
    assertThat(betResponse.message()).contains("successfully published to Kafka");
  }

  @Test
  void testBetSubmissionWithInvalidBetRequest() throws Exception {
    BetRequest invalidBetRequest = new BetRequest(null, null, new BigDecimal("-50.00"));

    // When & Then
    mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidBetRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testBetSubmissionWithInvalidToken() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer invalid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validBetRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void testBetSubmissionWithLargeAmount() throws Exception {
    BetRequest largeAmountBet = new BetRequest("test-bet-3", "jackpot-1", new BigDecimal("1000000.00"));

    // When & Then
    mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(largeAmountBet)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROCESSED"));
  }

  @Test
  void testBetSubmissionWithZeroAmount() throws Exception {
    BetRequest zeroAmountBet = new BetRequest("test-bet-2", "jackpot-1", BigDecimal.ZERO);

    // When & Then
    mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(zeroAmountBet)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testBetSubmissionWithoutAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/bets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validBetRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetContributionForExistingBet() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/bets/bet-1/contribution")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value("1"))
        .andExpect(jsonPath("$.betId").value("bet-1"))
        .andExpect(jsonPath("$.jackpotId").value("jackpot-1"))
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.contributionAmount").value(5.00));
  }

  @Test
  void testGetContributionForNonExistentBet() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/bets/non-existent-bet/contribution")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testGetContributionWithoutAuthentication() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/bets/bet-1/contribution"))
        .andExpect(status().isForbidden());
  }

  @Test
  void testKafkaMessagePublishingIntegration() throws Exception {
    // This test verifies that the bet submission triggers Kafka message publishing
    // The actual Kafka consumption would be tested in a separate consumer test

    // When
    MvcResult result = mockMvc.perform(post("/api/bets")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validBetRequest)))
        .andExpect(status().isOk())
        .andReturn();

    // Then
    String responseContent = result.getResponse().getContentAsString();
    BetResponse betResponse = objectMapper.readValue(responseContent, BetResponse.class);

    // Verify the response indicates successful Kafka publishing
    assertThat(betResponse.status()).isEqualTo("PROCESSED");
    assertThat(betResponse.message()).contains("Kafka");

    // Note: In a real integration test, you would also verify that the message
    // was actually published to Kafka and consumed by the appropriate consumer
  }
}