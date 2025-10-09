package com.jackpot.controller;

import com.jackpot.dto.BetRequest;
import com.jackpot.dto.BetResponse;
import com.jackpot.kafka.KafkaProducer;
import com.jackpot.model.User;
import com.jackpot.security.CustomUserDetails;
import com.jackpot.service.JackpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BetController that test the full Spring Boot application context
 * Note: These tests use @WithMockUser which works with Spring Security test support
 * but may not work with JWT authentication filters. For comprehensive authentication
 * testing, see BetControllerSecurityTest for unit tests that mock authentication directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaProducer kafkaProducer;

    @MockBean
    private JackpotService jackpotService;

    private CustomUserDetails testUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        User userEntity = new User();
        userEntity.setId(456L);
        userEntity.setUsername("testuser");
        userEntity.setPassword("password");
        userEntity.setEnabled(true);
        testUser = new CustomUserDetails(userEntity);
    }

    @Test
    void testPublishBet_Integration_AuthenticationRequired() throws Exception {
        // This test verifies that authentication is required for the bet endpoint
        // The JWT filter should block unauthenticated requests

        // Arrange
        String requestBody = """
        {
            "betId": "test-bet-001",
            "jackpotId": "jackpot-fixed",
            "betAmount": 75
        }
        """;

        // Act & Assert - Unauthenticated requests should be blocked
        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden()); // JWT filter blocks unauthenticated requests
    }

    @Test
    void testPublishBet_Integration_WithoutAuthentication() throws Exception {
        // This test is redundant with testPublishBet_Integration_AuthenticationRequired
        // but kept for clarity - both test the same authentication requirement

        // Arrange
        String requestBody = """
        {
            "betId": "unauth-bet-123",
            "jackpotId": "jackpot-fixed",
            "betAmount": 150
        }
        """;

        // Act & Assert - Should be forbidden (403) since authentication is required
        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }
}