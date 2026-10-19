FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build/backend
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip && rm -rf /var/lib/apt/lists/*
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod 755 mvnw
COPY backend/src src
# Image builds do not start Oracle. Run scripts/test.sh before releasing an image.
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 nexus \
    && useradd --uid 10001 --gid nexus --no-create-home --shell /usr/sbin/nologin nexus \
    && mkdir -p /app/var/attachments && chown -R nexus:nexus /app
WORKDIR /app
COPY --from=build --chown=nexus:nexus /build/backend/target/nexus-crm-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
ENV SERVER_ADDRESS=0.0.0.0 SERVER_PORT=8080 NEXUS_STORAGE_DIR=/app/var/attachments
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness > /dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/app.jar"]
