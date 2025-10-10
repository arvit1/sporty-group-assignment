# Multi-stage build for Jackpot Service
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent POM and module POMs
COPY pom.xml .
COPY jackpot-service-impl/pom.xml jackpot-service-impl/
COPY jackpot-service-integration-tests/pom.xml jackpot-service-integration-tests/

# Download dependencies (this layer will be cached if POMs don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY jackpot-service-impl/src jackpot-service-impl/src
COPY jackpot-service-integration-tests/src jackpot-service-integration-tests/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Create runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Copy the built JAR from builder stage (Spring Boot creates -exec.jar)
COPY --from=builder /app/jackpot-service-impl/target/*-exec.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
