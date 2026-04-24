package com.kairos.context_engine.infrastructure.semantic;

import com.kairos.context_engine.domain.semantic.ChunkerExtractor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChunkerExtractorAdapter implements ChunkerExtractor {

    @Override
    public List<String> extract(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be greater than or equal to 0");
        }

        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be less than chunkSize");
        }

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));

            if (end == content.length()) {
                break;
            }

            start += step;
        }

        return chunks;
    }
}