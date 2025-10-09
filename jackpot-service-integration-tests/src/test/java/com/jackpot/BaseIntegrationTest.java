package com.jackpot;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

  static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
      .withReuse(true);
  static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("jackpot_test")
      .withUsername("testuser")
      .withPassword("testpass")
      .withReuse(true);
  static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379)
      .withReuse(true);

  static {
    postgreSQLContainer.start();
    kafkaContainer.start();
    redisContainer.start();

    // Create Kafka topics after container starts
    createKafkaTopics();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL configuration
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    // Disable schema.sql initialization (MySQL-specific)
    registry.add("spring.sql.init.mode", () -> "never");
    registry.add("spring.sql.init.schema-locations", () -> "");

    // Kafka configuration
    registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);
    registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);

    // Redis configuration
    registry.add("spring.data.redis.host", redisContainer::getHost);
    registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

    // Application specific properties
    registry.add("jackpot.kafka.topic.bets", () -> "jackpot-bets-test");
    registry.add("jackpot.kafka.topic.contributions", () -> "jackpot-contributions-test");
    registry.add("jackpot.kafka.topic.rewards", () -> "jackpot-rewards-test");

    // Security configuration for testing
    registry.add("jwt.secret", () -> "test-jwt-secret-key-that-is-long-enough-for-hmac-sha256");
    registry.add("jwt.expiration", () -> "3600000"); // 1 hour
  }

  private static void createKafkaTopics() {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());

    try (AdminClient adminClient = AdminClient.create(props)) {
      // Create test topics
      NewTopic betsTopic = new NewTopic("jackpot-bets-test", 1, (short) 1);

      adminClient.createTopics(Set.of(betsTopic)).all().get();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException("Failed to create Kafka topics", e);
    }
  }
}