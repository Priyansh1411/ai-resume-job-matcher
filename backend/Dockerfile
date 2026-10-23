# Stage 1: build the Spring Boot fat jar. Tests are not run here (mvn test
# already runs separately, and this project's integration tests need a Docker
# socket for Testcontainers, which an image build stage doesn't reliably have)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Stage 2: run the jar on a JRE-only image - no JDK, no Maven, no source.
# All configuration (JWT_SECRET, DB credentials, provider keys) comes from
# environment variables at container start, never baked into the image.
FROM eclipse-temurin:25-jre-jammy
RUN groupadd --gid 1000 spring \
    && useradd --uid 1000 --gid spring --system --no-create-home --shell /usr/sbin/nologin spring
WORKDIR /app
COPY --from=build --chown=spring:spring /build/target/backend-*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
