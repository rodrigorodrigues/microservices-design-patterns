FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY scala-address-service/pom.xml scala-address-service/
# Copy other modules to allow -am to work if needed, or just install them
COPY . .
RUN mvn install -DskipTests -pl scala-address-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/scala-address-service/target/scala-address-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
