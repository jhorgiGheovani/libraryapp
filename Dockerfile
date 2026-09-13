FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests \
        -Dmaven.wagon.http.retryHandler.count=3

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app


RUN groupadd --system libraryapp \
 && useradd --system --gid libraryapp --home /app libraryapp

COPY --from=build --chown=libraryapp:libraryapp /build/target/*.jar app.jar

USER libraryapp
EXPOSE 9210

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
