FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY bot/build.gradle.kts bot/settings.gradle.kts bot/gradle.properties* /app/bot/
COPY bot/gradle /app/bot/gradle
COPY bot/gradlew /app/bot/gradlew

COPY bot/src /app/bot/src

COPY parser/addresses.json /app/parser/addresses.json

WORKDIR /app/bot
RUN ./gradlew clean build --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/bot/build/libs/*.jar /app/bot.jar

COPY --from=build /app/parser/addresses.json /app/parser/addresses.json

ENV ADDRESSES_FILE_PATH=/app/parser/addresses.json

CMD ["java", "-jar", "/app/bot.jar"]
