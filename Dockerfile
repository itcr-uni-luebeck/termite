# Build jar using Gradle
FROM gradle:7.2.0-jdk17-alpine AS gradle_build
COPY . /termite
WORKDIR /termite
# Create jar
RUN gradle clean build bootJar

# Copy jar to new image for running terminology service
FROM openjdk:17
COPY --from=gradle_build /termite/build/libs/termite.jar /app/termite.jar
COPY /config/application.properties /app/config/application.properties
COPY /config/log4j2.xml /app/config/log4j2.xml
COPY upload.sh /app/upload.sh

# Default certificates
RUN mkdir -p /app/security/keystore
COPY /security/keystore /app/security/keystore

RUN mkdir /app/database

WORKDIR /app

#ENTRYPOINT sh upload.sh
ENTRYPOINT java -Dserver.port=9083 -jar termite.jar -Dlog4j.configurationFile=/app/config/log4j2.xml