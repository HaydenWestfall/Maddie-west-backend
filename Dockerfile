# syntax=docker/dockerfile:1

# --- Stage 1: build the executable jar ---------------------------------------
# The repo has no Maven wrapper, so the toolchain comes from the builder image.
# Java 21 here matches <java.version> in pom.xml.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies against pom.xml alone, so editing src/ does not re-download
# the world. The cache mount keeps ~/.m2 warm across builds without landing in a layer.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
# Tests are skipped on purpose: the integration tests start MongoDB via Testcontainers,
# which needs a Docker daemon this build container does not have. Run `mvn verify` in CI.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

# --- Stage 2: explode the jar into Spring Boot layers -------------------------
# Splitting the fat jar lets the runtime image cache dependencies separately from
# application classes, so a code-only change ships a few hundred KB instead of ~80MB.
FROM eclipse-temurin:21-jre-jammy AS extract
WORKDIR /extract
COPY --from=build /build/target/*.jar app.jar
# The destination must be its own empty directory — extracting alongside app.jar fails,
# and the JVM still exits 0, which would silently produce an image with no application.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers \
    && mkdir -p layers/dependencies layers/spring-boot-loader \
                layers/snapshot-dependencies layers/application \
    && test -f layers/spring-boot-loader/org/springframework/boot/loader/launch/JarLauncher.class

# --- Stage 3: runtime ---------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# curl is only here to back the HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

# Ordered most-stable-first: a source change invalidates only the application layer.
COPY --from=extract --chown=app:app /extract/layers/dependencies/ ./
COPY --from=extract --chown=app:app /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=app:app /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=app:app /extract/layers/application/ ./

# logback-spring.xml rolls files into ./logs in addition to stdout, and the app runs
# as a non-root user, so the directory has to exist and be writable up front.
RUN mkdir -p /app/logs && chown app:app /app/logs

USER app

ENV PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseContainerSupport"

EXPOSE 8081

# Actuator reports DOWN when MongoDB is unreachable, so this tracks real readiness.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT}/actuator/health" || exit 1

# `exec` hands PID 1 to the JVM so SIGTERM reaches Spring's shutdown hooks; the sh
# wrapper is what allows JAVA_OPTS to be overridden at run time.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
