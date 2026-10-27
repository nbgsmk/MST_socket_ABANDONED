FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN useradd --system --create-home --home-dir /home/app appuser
COPY --from=builder /build/build/install/socket/ ./
RUN mkdir /config && chown appuser:appuser /config

USER appuser
EXPOSE 8080
ENTRYPOINT ["/app/bin/socket", "--config", "/config/application.yml"]