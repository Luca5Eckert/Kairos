
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline

COPY infra/scripts/prepare-model.sh ./infra/scripts/prepare-model.sh
RUN bash ./infra/scripts/prepare-model.sh

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q clean package -DskipTests


FROM eclipse-temurin:22-jre-jammy AS runtime

LABEL org.opencontainers.image.title="Spring Boot Application"
LABEL org.opencontainers.image.description="Robust production-ready Spring Boot container"
LABEL org.opencontainers.image.version="1.0"

ENV TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    APP_HOME=/app \
    JAVA_OPTS=""

WORKDIR ${APP_HOME}

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl tzdata && \
    rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    groupadd --system spring; \
    useradd --system \
    --gid spring \
    --home-dir ${APP_HOME} \
    --shell /usr/sbin/nologin \
    --no-create-home \
    spring

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R spring:spring ${APP_HOME}

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=45s \
            --retries=5 \
CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1


ENTRYPOINT ["sh", "-c", "java \
-XX:InitialRAMPercentage=25.0 \
-XX:MaxRAMPercentage=75.0 \
-XX:+UseStringDeduplication \
-Djava.security.egd=file:/dev/./urandom \
$JAVA_OPTS \
-jar app.jar"]
