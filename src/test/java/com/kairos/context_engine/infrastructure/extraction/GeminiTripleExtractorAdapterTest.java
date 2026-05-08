package com.kairos.context_engine.infrastructure.extraction;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiTripleExtractorAdapterTest {

    @Test
    void prompt_shouldRenderWithTextParameter() throws Exception {
        Field promptField = GeminiTripleExtractorAdapter.class.getDeclaredField("PROMPT");
        promptField.setAccessible(true);

        String prompt = (String) promptField.get(null);
        String renderedPrompt = new PromptTemplate(prompt).render(Map.of("text", "Spring AI renders templates."));

        assertThat(renderedPrompt).contains("Spring AI renders templates.");
    }
}
