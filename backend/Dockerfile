# Build stage: compile the app with Maven and JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Run stage: only a Java runtime and the built jar
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --create-home --uid 1001 app && mkdir -p /app/data && chown -R app:app /app
COPY --from=build /build/target/*.jar app.jar
USER app
ENV APP_DATA_DIR=/app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]