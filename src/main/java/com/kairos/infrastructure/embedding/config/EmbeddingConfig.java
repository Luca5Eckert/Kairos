package com.kairos.infrastructure.embedding.config;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class EmbeddingConfig  {


    @Bean(destroyMethod = "close")
    public OrtEnvironment ortEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    @Bean(destroyMethod = "close")
    public OrtSession onnxSession(
            OrtEnvironment environment,
            @Value("classpath:model/model.onnx") Resource modelResource
    ) throws OrtException, IOException {
        Path tempFile = Files.createTempFile("kairos-model-", ".onnx");
        try (InputStream in = modelResource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        return environment.createSession(tempFile.toString(), options);
    }


    @Bean
    public HuggingFaceTokenizer huggingFaceTokenizer(
            @Value("classpath:model/tokenizer.json") Resource tokenizerResource
    ) throws IOException {
        Path tempDir  = Files.createTempDirectory("kairos-embedding");
        Path tempFile = tempDir.resolve("tokenizer.json");
        try (InputStream in = tokenizerResource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return HuggingFaceTokenizer.newInstance(tempFile);
    }

}
