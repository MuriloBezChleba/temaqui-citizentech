FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre
RUN useradd --system temaqui
WORKDIR /app
COPY --from=build /build/target/temaqui-1.0.0.jar app.jar
USER temaqui
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

