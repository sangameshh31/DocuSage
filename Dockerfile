# --- Build stage: compile and package with Maven on JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Run stage: slim JRE image with just the executable jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/docusage-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
