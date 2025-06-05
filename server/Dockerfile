# server/Dockerfile
FROM gradle:8.5.0-jdk17 AS build

# Update CA certificates as root first
USER root
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && apt-get clean && update-ca-certificates

# Switch to gradle user for all subsequent operations in this stage
USER gradle
WORKDIR /app

# Copy files and ensure they are owned by the gradle user
COPY --chown=gradle:gradle . .

# Produce a distribution under build/install/<projectName>
# This will run as 'gradle' user and have permission to create .gradle in /app
RUN gradle installDist --no-daemon -x test -x check -PskipFormatting

# Debug: List the contents of the installDist output directory
RUN echo "---- Contents of /app/build/install/server ----" && ls -R /app/build/install/server

FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy only the "server" distribution that Gradle produced; replace "server" if your project
# is named differently (run "./gradlew installDist" locally to confirm the folder name).
COPY --from=build /app/build/install/server /app

# Debug: List the contents of the final /app directory
RUN echo "---- Contents of /app in final stage ----" && ls -R /app

EXPOSE 8080
# ENTRYPOINT ["./bin/server"]
