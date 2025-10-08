# Jackpot Service

A Spring Boot application for managing jackpot contributions and rewards with Kafka integration.

## Features

- JWT-based authentication
- Bet processing via Kafka
- Jackpot reward evaluation
- Redis for JWT secret management
- H2 in-memory database

## Quick Start

### Using Docker

1. **Build the Docker image:**
   ```bash
   docker build -t jackpot-service .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 jackpot-service
   ```

### Using Maven

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/jackpot-service-1.0.0.jar
   ```

## API Usage with curl

### 1. Authentication

First, you need to authenticate to get a JWT token.

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Save the token for subsequent requests:
```bash
export JWT_TOKEN="your_jwt_token_here"
```

### 2. Bet Management

#### Submit a Bet

```bash
# Submit a new bet
curl -X POST http://localhost:8080/api/bets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "betId": "bet-123",
    "userId": "user1",
    "jackpotId": "jackpot-fixed-fixed",
    "betAmount": 100.50
  }'
```

**Response:**
```json
{
  "betId": "bet-123",
  "status": "PROCESSED",
  "message": "Bet successfully published to Kafka for processing"
}
```

#### Get Contribution for a Bet

```bash
# Get contribution details for a specific bet
curl -X GET http://localhost:8080/api/bets/bet-123/contribution \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### 3. Jackpot Management

#### Get Jackpot Details

```bash
# Get jackpot information
curl -X GET http://localhost:8080/api/jackpots/jackpot-789 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### Evaluate Reward

```bash
# Evaluate if a bet wins the jackpot
curl -X POST "http://localhost:8080/api/jackpots/jackpot-789/evaluate-reward?betId=bet-123&userId=user-456" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response (Win):**
```json
{
  "betId": "bet-123",
  "won": true,
  "rewardAmount": 5000.00,
  "message": "Congratulations! You won the jackpot!"
}
```

**Response (Loss):**
```json
{
  "betId": "bet-123",
  "won": false,
  "rewardAmount": null,
  "message": "Better luck next time!"
}
```

#### Get Reward Details

```bash
# Get reward information for a specific bet
curl -X GET http://localhost:8080/api/jackpots/jackpot-789/rewards/bet-123 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## Complete Workflow Example

Here's a complete example showing the typical workflow:

```bash
# 1. Authenticate
export JWT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}' | jq -r '.token')

echo "Token: $JWT_TOKEN"

# 2. Submit a bet
curl -X POST http://localhost:8080/api/bets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "betId": "bet-001",
    "userId": "user-001",
    "jackpotId": "jackpot-001",
    "betAmount": 50.00
  }'

# 3. Check contribution
curl -X GET http://localhost:8080/api/bets/bet-001/contribution \
  -H "Authorization: Bearer $JWT_TOKEN"

# 4. Evaluate reward
curl -X POST "http://localhost:8080/api/jackpots/jackpot-001/evaluate-reward?betId=bet-001&userId=user-001" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## API Endpoints Summary

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| POST | `/api/auth/login` | Authenticate user | No |
| POST | `/api/bets` | Submit a new bet | Yes |
| GET | `/api/bets/{betId}/contribution` | Get bet contribution | Yes |
| GET | `/api/jackpots/{jackpotId}` | Get jackpot details | Yes |
| POST | `/api/jackpots/{jackpotId}/evaluate-reward` | Evaluate reward | Yes |
| GET | `/api/jackpots/{jackpotId}/rewards/{betId}` | Get reward details | Yes |

## Configuration

The application uses the following default configuration:

- **Port:** 8080
- **Database:** H2 in-memory (jdbc:h2:mem:jackpotdb)
- **Redis:** localhost:6379
- **Kafka:** localhost:9092

For production use, update the `application.yml` file with your specific configuration.

## Development

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (optional)

### Building

```bash
mvn clean package
```

### Testing

```bash
mvn test
```

## Docker Compose Setup

For a complete development environment with all dependencies, use the provided Docker Compose configuration:

### Quick Start

```bash
# Start all services
docker-compose up --build

# Or start in background
docker-compose up -d


### Key Features of the Docker Setup

- **Health Checks**: All services include health monitoring
- **Service Discovery**: Uses Docker internal DNS for communication
- **Data Persistence**: Redis data is persisted across restarts
- **Dependency Management**: Services start in correct order
- **Testing**: Automated connectivity testing script

### Configuration Overview

```yaml
version: '3.8'
services:
  jackpot-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
    volumes:
      - redis_data:/data

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    ports:
      - "9092:9092"
    depends_on:
      zookeeper:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "kafka-topics", "--list", "--bootstrap-server", "localhost:9092"]

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    healthcheck:
      test: ["CMD", "echo", "ruok", "|", "nc", "localhost", "2181"]

networks:
  jackpot-network:
    driver: bridge

volumes:
  redis_data:
```

## Troubleshooting

### Application Issues
- **Authentication failed:** Ensure you're using the correct username/password
- **Connection refused:** Check if the service is running on port 8080
- **Kafka errors:** Ensure Kafka is running if you're using the bet submission feature
- **Redis errors:** Ensure Redis is running for JWT secret management

### Docker-Specific Issues
- **Services not starting:** Check if ports are already in use (`lsof -i :8080`)
- **Connection timeouts:** Wait for health checks to pass or check service logs
- **Docker resource issues:** Ensure Docker has sufficient memory and disk space
- **Network problems:** Verify Docker network configuration

### Docker Debugging Commands
```bash
# Check service status
docker-compose ps

# View service logs
docker-compose logs [service-name]

# Test connectivity from inside containers
docker-compose exec jackpot-service curl http://localhost:8080/actuator/health

# Check Docker resource usage
docker system df
docker stats
```
