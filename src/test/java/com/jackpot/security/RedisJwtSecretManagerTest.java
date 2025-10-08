package com.jackpot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisJwtSecretManagerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisJwtSecretManager redisJwtSecretManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Initialize the RedisJwtSecretManager with the expected key
        redisJwtSecretManager = new RedisJwtSecretManager("jwt:secret", redisTemplate);
    }

    @Test
    void testGetOrCreateJwtSecret_WhenSecretExists() {
        // Arrange
        String existingSecret = "existing-jwt-secret";
        when(valueOperations.get("jwt:secret")).thenReturn(existingSecret);

        // Act
        String result = redisJwtSecretManager.getOrCreateJwtSecret();

        // Assert
        assertEquals(existingSecret, result);
        verify(valueOperations).get("jwt:secret");
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testGetOrCreateJwtSecret_WhenSecretDoesNotExist() {
        // Arrange
        when(valueOperations.get("jwt:secret")).thenReturn(null);

        // Act
        String result = redisJwtSecretManager.getOrCreateJwtSecret();

        // Assert
        assertNotNull(result);
        assertTrue(result.length() > 0);
        verify(valueOperations).get("jwt:secret");
        verify(valueOperations).set(
            eq("jwt:secret"),
            anyString(),
            eq(30L),
            eq(TimeUnit.DAYS)
        );
    }

    @Test
    void testRefreshJwtSecret() {
        // Act
        redisJwtSecretManager.refreshJwtSecret();

        // Assert
        verify(valueOperations).set(
            eq("jwt:secret"),
            anyString(),
            eq(30L),
            eq(TimeUnit.DAYS)
        );
    }

    @Test
    void testGetCurrentJwtSecret() {
        // Arrange
        String expectedSecret = "current-secret";
        when(valueOperations.get("jwt:secret")).thenReturn(expectedSecret);

        // Act
        String result = redisJwtSecretManager.getCurrentJwtSecret();

        // Assert
        assertEquals(expectedSecret, result);
        verify(valueOperations).get("jwt:secret");
    }
}