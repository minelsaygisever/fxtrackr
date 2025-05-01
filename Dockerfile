FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app

COPY --from=build /app/target/FXTrackr-1.0.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="\
  -Dspring.profiles.active=default \
  -Dspring.h2.console.enabled=true \
"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
EXPOSE 8080