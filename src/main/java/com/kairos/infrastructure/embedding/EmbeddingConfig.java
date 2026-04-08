package com.kairos.infrastructure.embedding;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class EmbeddingConfig  {


    @Bean
    public OrtEnvironment ortEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    @Bean
    public OrtSession onnxSession(
            OrtEnvironment environment,
            @Value("classpath:model/model.onnx") Resource modelResource
    ) throws Exception {
        byte[] modelBytes;
        try (var inputStream = modelResource.getInputStream()) {
            modelBytes = inputStream.readAllBytes();
        }
        return environment.createSession(modelBytes);
    }


    @Bean
    public HuggingFaceTokenizer huggingFaceTokenizer(
            @Value("classpath:model/tokenizer.json") Resource tokenizerResource
    ) throws Exception {
        Path tempFile = Files.createTempFile("kairos-tokenizer", ".json");
        try (InputStream in = tokenizerResource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        tempFile.toFile().deleteOnExit();
        return HuggingFaceTokenizer.newInstance(tempFile);
    }

}
