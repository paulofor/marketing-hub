FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/app.jar /app/app.jar

EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
