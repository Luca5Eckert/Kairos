# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src

RUN mvn -B -q package -DskipTests

# --- Estágio 2: Runtime  ---
FROM eclipse-temurin:21-jre-jammy

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

RUN set -eux; \
    groupadd --system app; \
    useradd --system --gid app --of-home --home-dir ${APP_HOME} app

COPY --from=build /app/target/*.jar ${APP_HOME}/app.jar

RUN chown -R app:app ${APP_HOME}

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

# Comando de inicialização com flags básicas de otimização de memória
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]