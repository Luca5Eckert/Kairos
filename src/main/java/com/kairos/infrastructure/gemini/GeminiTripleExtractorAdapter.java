package com.kairos.infrastructure.gemini;

import com.kairos.domain.graph.TripleExtractor;
import com.kairos.domain.model.Triple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that implements the {@link TripleExtractor} interface using the Gemini API.
 * This class is responsible for generating prompts, calling the Gemini API, and parsing the responses to extract semantic triples from input text.
 */
@Component
@Slf4j
public class GeminiTripleExtractorAdapter implements TripleExtractor {

    private static final int MAX_ATTEMPTS = 4;
    private static final long INITIAL_BACKOFF_MILLIS = 1_000L;

    private final GeminiRestClient geminiRestClient;
    private final GeminiResponseParser parser;

    public GeminiTripleExtractorAdapter(GeminiRestClient geminiRestClient, GeminiResponseParser parser) {
        this.geminiRestClient = geminiRestClient;
        this.parser = parser;
    }


    /**
     * Extracts semantic triples from the input text using the Gemini API.
     * @param text The input text from which to extract triples.
     * @return A list of {@link Triple} objects representing the extracted subject-predicate-object relationships.
     */
    @Override
    public List<Triple> extract(String text) {
        String prompt = generatePrompt(text);

        var response = geminiRestClient.call(prompt);

        return parser.parseResponse(response);
    }

    /**
     * Generates a detailed prompt for Gemini to extract high-quality triples from the input text.
     * @param text The input text from which to extract triples. Can be null or empty, but will be normalized to an empty string.
     * @return A carefully crafted prompt string that instructs Gemini to perform Open Information Extraction (OpenIE) and return valid JSON triples according to the specified schema and rules.
     */
    private String generatePrompt(String text) {
        String safeText = text == null ? "" : text.trim();

        return """
        You are a knowledge graph construction engine for Open Information Extraction (OpenIE).

        Your task is to read the input text and extract all meaningful semantic triples in the form:
        subject -> predicate -> object

        Return ONLY valid JSON.
        Do not return markdown.
        Do not wrap the JSON in code fences.
        Do not add explanations, comments, notes, or preamble text.

        The JSON response MUST follow this exact schema:
        {
          "triples": [
            {
              "subject": "string",
              "predicate": "STRING",
              "object": "string"
            }
          ]
        }

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

        Output constraints:
        - Output must be valid JSON only.
        - Output must contain a top-level "triples" array.
        - Each triple object must contain exactly: "subject", "predicate", "object".
        - Do not include confidence, explanation, metadata, or extra fields.

        Input text:
        ---
        %s
        ---
        """.formatted(safeText);
    }

}