# --- Build stage ---
FROM gradle:8.11-jdk25 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
