FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY settings.xml ./settings.xml
COPY pom.xml ./pom.xml
RUN mvn -s settings.xml -f pom.xml dependency:go-offline

COPY src ./src
RUN mvn -s settings.xml -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/target/app-exec.jar /app/app.jar
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
