package com.jackpot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackpot.dto.AuthRequest;
import com.jackpot.dto.AuthResponse;

@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

  private AuthRequest invalidAuthRequest;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  private AuthRequest validAuthRequest;

  @BeforeEach
  void setUp() {
    // Use a simple password that matches the BCrypt hash in test data
    validAuthRequest = new AuthRequest("testuser", "password");
    invalidAuthRequest = new AuthRequest("testuser", "wrongpassword");
  }

  @Test
  void testEmptyCredentials() throws Exception {
    AuthRequest emptyCredentials = new AuthRequest("", "");

    // When & Then
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(emptyCredentials)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Incorrect username or password"));
  }

  @Test
  void testInvalidCredentials() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidAuthRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Incorrect username or password"));
  }

  @Test
  void testJwtTokenGeneration() throws Exception {
    // When
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validAuthRequest)))
        .andExpect(status().isOk())
        .andReturn();

    // Then
    String responseContent = result.getResponse().getContentAsString();
    AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);

    // Verify JWT token structure
    String jwt = authResponse.token();
    String[] jwtParts = jwt.split("\\.");

    assertThat(jwtParts).hasSize(3);
    assertThat(jwtParts[0]).isNotEmpty(); // Header
    assertThat(jwtParts[1]).isNotEmpty(); // Payload
    assertThat(jwtParts[2]).isNotEmpty(); // Signature

    // Verify the token can be used for subsequent requests
    // This would be tested in other controller tests that require authentication
  }

  @Test
  void testNonExistentUser() throws Exception {
    AuthRequest nonExistentUser = new AuthRequest("nonexistent", "password123");

    // When & Then
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(nonExistentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Incorrect username or password"));
  }

  @Test
  void testSuccessfulLogin() throws Exception {
    // When & Then
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validAuthRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.token").isString())
        .andReturn();

    // Verify JWT token structure
    String responseContent = result.getResponse().getContentAsString();
    AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);

    assertThat(authResponse.token()).isNotNull();
    assertThat(authResponse.token()).isNotEmpty();
    assertThat(authResponse.token().split("\\.")).hasSize(3); // JWT should have 3 parts
  }
}