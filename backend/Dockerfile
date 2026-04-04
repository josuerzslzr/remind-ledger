# Build stage
FROM amazoncorretto:21-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q package -DskipTests

# Runtime: JDK 21 + curl for ECS/Compose health checks
FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl \
  && addgroup -g 65532 -S app \
  && adduser -u 65532 -S -G app -h /app -D app
WORKDIR /app
COPY --from=build --chown=65532:65532 /app/target/remind-ledger-*.jar /app/app.jar
EXPOSE 8080
USER app
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
