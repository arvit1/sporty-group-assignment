package com.jackpot.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisJwtSecretManager {

  private static final long SECRET_TTL_DAYS = 30;
  private RedisTemplate<String, String> redisTemplate;
  private final String secret;

  public RedisJwtSecretManager(@Value("${jwt.secret}") String secret, RedisTemplate<String, String> redisTemplate) {
    this.secret = secret;
    this.redisTemplate = redisTemplate;
  }

  /**
   * Get the current JWT secret without creating a new one
   */
  public String getCurrentJwtSecret() {
    return redisTemplate.opsForValue().get(secret);
  }

  /**
   * Get or create JWT secret from Redis
   * If secret doesn't exist, generate a new one and store it in Redis
   */
  public String getOrCreateJwtSecret() {
    String currentSecret = redisTemplate.opsForValue().get(secret);

    if (currentSecret == null) {
      currentSecret = generateSecureSecret();
      redisTemplate.opsForValue().set(
          secret,
          currentSecret,
          SECRET_TTL_DAYS,
          TimeUnit.DAYS
      );
    }

    return currentSecret;
  }

  /**
   * Refresh the JWT secret (useful for key rotation)
   */
  public void refreshJwtSecret() {
    String newSecret = generateSecureSecret();
    redisTemplate.opsForValue().set(
        secret,
        newSecret,
        SECRET_TTL_DAYS,
        TimeUnit.DAYS
    );
  }

  /**
   * Generate a secure random secret
   */
  private String generateSecureSecret() {
    try {
      // Generate a secure random string
      byte[] randomBytes = new byte[64];
      new java.security.SecureRandom().nextBytes(randomBytes);

      // Hash it for additional security
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedBytes = digest.digest(randomBytes);

      return Base64.getEncoder().encodeToString(hashedBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate JWT secret", e);
    }
  }
}