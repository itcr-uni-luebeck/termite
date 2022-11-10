# Build jar using Gradle
FROM gradle:6.9.2-jdk11-alpine AS gradle_build
COPY . /home/abide_validation/termite
WORKDIR /home/abide_validation/termite
# Create jar
RUN gradle bootJar --no-daemon

# Copy jar to new image for running terminology service
FROM openjdk:17
COPY --from=gradle_build /home/abide_validation/termite/build/libs/termite.jar /app/termite.jar
COPY /src/main/resources/config/log4j2.xml /app/log4j2.xml
COPY upload.sh /app/upload.sh
VOLUME ["/app/terminology_data"]
WORKDIR /app

ENTRYPOINT sh upload.sh
