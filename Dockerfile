# syntax=docker/dockerfile:1.7

# Stage 1 — download do modelo
FROM alpine:3.20 AS model-download

RUN apk add --no-cache curl

WORKDIR /tmp/model

RUN set -eux; \
    curl -L --fail --retry 5 --retry-all-errors --connect-timeout 20 \
      "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx" \
      -o model.onnx; \
    curl -L --fail --retry 5 --retry-all-errors --connect-timeout 20 \
      "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json" \
      -o tokenizer.json; \
    test -s model.onnx; \
    test -s tokenizer.json

# Stage 2 — build da aplicação
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY --from=model-download /tmp/model/ ./src/main/resources/model/

RUN mvn -B -q package -DskipTests

# Stage 3 — imagem final
FROM eclipse-temurin:21-jre-jammy

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

RUN set -eux; \
    groupadd --system app; \
    useradd --system --gid app --create-home --home-dir ${APP_HOME} app

COPY --from=build /app/target/*.jar ${APP_HOME}/app.jar

RUN chown -R app:app ${APP_HOME}

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]