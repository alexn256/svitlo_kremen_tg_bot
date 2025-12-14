# Build stage
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY bot/build.gradle.kts bot/settings.gradle.kts bot/gradle.properties* /app/bot/
COPY bot/gradle /app/bot/gradle
COPY bot/gradlew /app/bot/gradlew

# Copy source code
COPY bot/src /app/bot/src

# Copy addresses.json
COPY parser/addresses.json /app/parser/addresses.json

# Build the application
WORKDIR /app/bot
RUN ./gradlew clean build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from build stage (use wildcard to match any jar name)
COPY --from=build /app/bot/build/libs/*.jar /app/bot.jar

# Copy addresses.json
COPY --from=build /app/parser/addresses.json /app/parser/addresses.json

# Set environment variables
ENV ADDRESSES_FILE_PATH=/app/parser/addresses.json

# Run the application
CMD ["java", "-jar", "/app/bot.jar"]
