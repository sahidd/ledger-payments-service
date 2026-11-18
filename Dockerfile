FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 ledger
COPY --from=build /src/target/ledger-payments-service.jar app.jar
USER ledger
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app.jar"]
