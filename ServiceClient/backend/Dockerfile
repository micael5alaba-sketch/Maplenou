# syntax=docker/dockerfile:1.7
# Maplenou Customer Service API (Spring Boot 4.1, Java 21).
#   docker build -t maplenou-backend backend

# =============================================================================
# Stage 1 - Builder: compile with the Maven wrapper, then split the jar into layers
# =============================================================================

# Pin the base distribution (Ubuntu 24.04 "noble") so rebuilds use the same system packages.
FROM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /app

# Wrapper and pom first: the dependency download is cached until they change.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# The Maven repository is a BuildKit cache mount: reused across builds, never stored in an image layer.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/maplenou-support-*.jar extract --layers --launcher --destination /extracted

# =============================================================================
# Stage 2 - Runtime: JRE only (no compiler, no Maven), non-root
# =============================================================================

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S -g 10001 maplenou \
    && adduser -S -D -H -u 10001 -G maplenou maplenou \
    && mkdir -p /data/attachments \
    && chown maplenou:maplenou /data/attachments

# One layer per Spring Boot layer, from the least to the most frequently changed:
# a code change only rebuilds and re-downloads the small "application" layer.
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

ENV ATTACHMENTS_DIR=/data/attachments \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER 10001

# fixed port inside the container (the host port is BACKEND_HOST_PORT in .env)
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=5 \
    CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/actuator/health"]

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
