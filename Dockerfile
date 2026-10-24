# --- Build stage: compile and package with Maven on JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Run stage: slim JRE image with just the executable jar
# 2026-10-23: per Render's eMudhra advisory, this service was rebuilt so the run
# image pulls the patched eclipse-temurin JRE (compromised eMudhra intermediates
# removed from the JRE cacerts) and the OS cert store is refreshed too.
FROM eclipse-temurin:17-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && update-ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/docusage-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
