# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source to speed up rebuilds
COPY pom.xml .
COPY url-shortener-core/pom.xml url-shortener-core/pom.xml
COPY url-shortener-web/pom.xml url-shortener-web/pom.xml
COPY web-common/pom.xml web-common/pom.xml
RUN mvn -q -B dependency:go-offline

COPY url-shortener-core url-shortener-core
COPY url-shortener-web url-shortener-web
COPY web-common web-common
RUN mvn -q -B -pl url-shortener-web -am clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/url-shortener-web/target/url-shortener-web.jar app.jar

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "app.jar"]
