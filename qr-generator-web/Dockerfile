# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY qr-generator-core/pom.xml qr-generator-core/pom.xml
COPY qr-generator-web/pom.xml qr-generator-web/pom.xml
COPY url-shortener-core/pom.xml url-shortener-core/pom.xml
COPY url-shortener-web/pom.xml url-shortener-web/pom.xml
COPY web-common/pom.xml web-common/pom.xml
RUN mvn -q -B dependency:go-offline

COPY qr-generator-core qr-generator-core
COPY qr-generator-web qr-generator-web
COPY web-common web-common
RUN mvn -q -B -pl qr-generator-web -am clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/qr-generator-web/target/qr-generator-web.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]