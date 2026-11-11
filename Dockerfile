# ---------- Stage 1: build the JAR (has the full JDK + Maven) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the Maven files first and download dependencies.
# Docker caches this layer, so later builds skip the download unless pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B dependency:go-offline

# Now copy the source code and build the JAR.
# Tests are skipped here because they already run in GitHub Actions (and need a database).
COPY src ./src
RUN ./mvnw -B package -DskipTests

# ---------- Stage 2: run the JAR (small image, Java runtime only) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container
RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]