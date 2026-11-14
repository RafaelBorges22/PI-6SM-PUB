# Smart Parking - API (Spring Boot + Kotlin) em container.
# Build:  docker compose --profile app build backend

# ---- 1) Compilacao ---------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Wrapper e scripts do Gradle primeiro: a camada de dependencias fica em cache.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# No Windows o git pode trocar LF por CRLF; o /bin/sh do Linux nao aceita.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- 2) Execucao -----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/smart-parking-backend-1.0.0.jar app.jar

ENV TZ=America/Sao_Paulo
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
