# Етап 1: Build
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Копиране на Gradle wrapper и скриптове
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

RUN chmod +x gradlew

# Build
RUN ./gradlew bootJar --no-daemon

# Етап 2: Финален образ
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

EXPOSE 8080

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]