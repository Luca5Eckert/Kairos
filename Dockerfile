# Stage 1 — baixa o modelo
FROM curlimages/curl:latest AS model-download

RUN mkdir -p /model && \
    curl -L --fail \
      "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx" \
      -o /model/model.onnx && \
    curl -L --fail \
      "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json" \
      -o /model/tokenizer.json

# Stage 2 — build da aplicação
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
# injeta o modelo antes de compilar/empacotar
COPY --from=model-download /model src/main/resources/model/
RUN mvn package -DskipTests -q

# Stage 3 — imagem final
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
USER nobody