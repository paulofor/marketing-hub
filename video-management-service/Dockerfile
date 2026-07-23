# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY video-management-service/pom.xml video-management-service/pom.xml
RUN mvn -B -f video-management-service/pom.xml dependency:go-offline

COPY video-management-service/src video-management-service/src
RUN mvn -B -f video-management-service/pom.xml package -DskipTests && \
    mv video-management-service/target/video-management-service-0.0.1-SNAPSHOT.jar video-management-service/target/app.jar

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg curl espeak-ng fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/video-management-service/target/app.jar app.jar
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8095/actuator/health || exit 1
ENTRYPOINT ["/bin/sh", "-c", "load_secret() { target=\"$1\"; file_var=\"$2\"; current=\"$(eval echo \\$$target)\"; file_path=\"$(eval echo \\$$file_var)\"; if [ -z \"$current\" ] && [ -n \"$file_path\" ] && [ -r \"$file_path\" ]; then export \"$target=$(tr -d '\\r\\n' < \"$file_path\")\"; fi; }; load_secret GEMINI_API_KEY GEMINI_API_KEY_FILE; load_secret LUMA_API_KEY LUMA_API_KEY_FILE; load_secret KLING_API_KEY KLING_API_KEY_FILE; if [ -z \"$VIDEO_PROVIDERS_VEO_API_KEY\" ] && [ -n \"$GEMINI_API_KEY\" ]; then export VIDEO_PROVIDERS_VEO_API_KEY=\"$GEMINI_API_KEY\"; fi; if [ -z \"$LUMA_AGENTS_API_KEY\" ] && [ -n \"$LUMA_API_KEY\" ]; then export LUMA_AGENTS_API_KEY=\"$LUMA_API_KEY\"; fi; if [ -z \"$VIDEO_PROVIDERS_LUMA_API_KEY\" ] && [ -n \"$LUMA_API_KEY\" ]; then export VIDEO_PROVIDERS_LUMA_API_KEY=\"$LUMA_API_KEY\"; fi; if [ -z \"$VIDEO_PROVIDERS_KLING_API_KEY\" ] && [ -n \"$KLING_API_KEY\" ]; then export VIDEO_PROVIDERS_KLING_API_KEY=\"$KLING_API_KEY\"; fi; exec java $JAVA_OPTS -jar /app/app.jar"]
