# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching for dependencies)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Run ───────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/satellite-api-0.0.1-SNAPSHOT.jar app.jar

# Railway sets PORT env var; default to 8080 locally
ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
