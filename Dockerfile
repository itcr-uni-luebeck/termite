# Build jar using Gradle
FROM gradle:6.9.2-jdk11-alpine AS gradle_build
ENV TERMINOLGY_SERVICE_PORT=8083
COPY . /home/abide_validation/termite
WORKDIR /home/abide_validation/termite
# Change terminology service settings
RUN printf 'logging.file.path=log\n  logging.file.name=logging.log\n  logging.config=log4j2.xml\n  server.port=%s' \
 "${TERMINOLOGY_SERVICE_PORT}" \
  > src/main/resources/config/application.properties
# Create jar
RUN gradle bootJar --no-daemon

# Copy jar to new image for running terminology service
FROM openjdk:17
COPY --from=gradle_build /home/abide_validation/termite/build/libs/termite.jar /app/termite.jar
COPY /src/main/resources/config/log4j2.xml /app/log4j2.xml
WORKDIR /app
ENTRYPOINT ["java", "-Dserver.port=8083", "-jar", "termite.jar", "-Dlog4j.configurationFile=app/log4j2.xml"]