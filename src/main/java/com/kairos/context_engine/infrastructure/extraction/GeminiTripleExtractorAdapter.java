package com.kairos.context_engine.infrastructure.extraction;

import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.domain.port.extraction.TripleExtractor;
import com.kairos.context_engine.infrastructure.ai.gemini.dto.TripleExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that implements the {@link TripleExtractor} interface using the Gemini API.
 * This class is responsible for generating prompts, calling the Gemini API, and parsing the responses to extract semantic triples from input text.
 */
@Component
@Slf4j
public class GeminiTripleExtractorAdapter implements TripleExtractor {


    private static final String PROMPT = """
            You are a knowledge graph construction engine for Open Information Extraction (OpenIE).
            
            Your task is to read the input text and extract all meaningful semantic triples in the form:
            subject -> predicate -> object
            
            Extraction rules:
            - Extract relationships between concepts, entities, events, processes, or ideas present in the text.
            - Extract both explicit relationships and strongly supported implicit relationships.
            - Do not invent facts that are not grounded in the text.
            - Do not extract trivial, redundant, circular, or self-referential triples.
            - Prefer fewer high-quality triples over many weak or generic ones.
            - If no meaningful triples can be extracted, return:
              { "triples": [] }
            
            Normalization rules:
            - All subjects, predicates, and objects must be in English.
            - Subjects and objects must be lowercase, normalized noun phrases.
            - Predicates must be uppercase verb phrases.
            - Remove unnecessary determiners and possessives.
            - Resolve pronouns and coreferences whenever possible.
            - Keep phrases concise, precise, and semantically complete.
            
            Predicate rules:
            - Prefer specific predicates over generic ones.
            - Good examples: "USES", "EXTENDS", "COMPUTES", "CAUSES", "DEPENDS_ON", "IMPLEMENTS".
            - Bad examples: "IS_RELATED_TO", "ASSOCIATED_WITH", unless no better grounded predicate exists.
            
            Text:
            {text}
            """;

    private final ChatClient chatClient;

    public GeminiTripleExtractorAdapter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Extracts semantic triples from the input text using the Gemini API.
     *
     * @param text The input text from which to extract triples.
     * @return A list of {@link Triple} objects representing the extracted subject-predicate-object relationships.
     */
    @Override
    public List<Triple> extract(String text) {
        if (text == null || text.isBlank()) return List.of();

        TripleExtractionResult result = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(user -> user.text(PROMPT).param("text", text))
                .call()
                .entity(TripleExtractionResult.class);
    }


}
