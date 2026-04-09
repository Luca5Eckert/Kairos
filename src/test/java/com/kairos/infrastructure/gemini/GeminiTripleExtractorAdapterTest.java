package com.kairos.infrastructure.gemini;

import com.kairos.domain.model.Triple;
import com.kairos.infrastructure.gemini.dto.GeminiResponse;
import com.kairos.infrastructure.gemini.exception.GeminiIntegrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiTripleExtractorAdapterTest {

    @Mock
    private GeminiRestClient geminiRestClient;

    @Mock
    private GeminiResponseParser parser;

    @InjectMocks
    private GeminiTripleExtractorAdapter adapter;

    // --- Fixtures ---

    private static final String SAMPLE_TEXT = "Spring Boot uses embedded Tomcat.";

    private static GeminiResponse geminiResponseWith(String text) {
        return new GeminiResponse(
                List.of(new GeminiResponse.Candidate(
                        new GeminiResponse.Content(
                                List.of(new GeminiResponse.Part(text)))))
        );
    }

    private static final GeminiResponse STUB_RESPONSE = geminiResponseWith("{}");

    // --- Blank / null guard ---

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void extract_returnsEmptyList_whenInputIsBlankOrNull(String input) {
        List<Triple> result = adapter.extract(input);

        assertThat(result).isEmpty();
        verifyNoInteractions(geminiRestClient, parser);
    }

    // --- Happy path ---

    @Test
    void extract_returnsTriplesFromParser_whenInputIsValid() {
        Triple triple = new Triple("spring boot", "USES", "embedded tomcat");
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(STUB_RESPONSE)).thenReturn(List.of(triple));

        List<Triple> result = adapter.extract(SAMPLE_TEXT);

        assertThat(result).containsExactly(triple);
    }

    @Test
    void extract_returnsMultipleTriples_whenParserExtractsMany() {
        List<Triple> expected = List.of(
                new Triple("spring boot", "USES", "embedded tomcat"),
                new Triple("tomcat", "IMPLEMENTS", "servlet container"),
                new Triple("spring boot", "PROVIDES", "auto-configuration")
        );
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(STUB_RESPONSE)).thenReturn(expected);

        List<Triple> result = adapter.extract(SAMPLE_TEXT);

        assertThat(result).hasSize(3).containsExactlyElementsOf(expected);
    }

    @Test
    void extract_returnsEmptyList_whenParserReturnsEmpty() {
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(STUB_RESPONSE)).thenReturn(List.of());

        List<Triple> result = adapter.extract(SAMPLE_TEXT);

        assertThat(result).isEmpty();
    }

    // --- Collaborator wiring ---

    @Test
    void extract_callsClientExactlyOnce_perInvocation() {
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(any())).thenReturn(List.of());

        adapter.extract(SAMPLE_TEXT);

        verify(geminiRestClient, times(1)).call(anyString());
    }

    @Test
    void extract_forwardsClientResponseToParser_unmodified() {
        GeminiResponse specificResponse = geminiResponseWith("{\"triples\":[]}");
        when(geminiRestClient.call(anyString())).thenReturn(specificResponse);
        when(parser.parseResponse(specificResponse)).thenReturn(List.of());

        adapter.extract(SAMPLE_TEXT);

        verify(parser).parseResponse(specificResponse);
    }

    // --- Prompt construction ---

    @Test
    void extract_promptContainsInputText() {
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(any())).thenReturn(List.of());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        adapter.extract(SAMPLE_TEXT);

        verify(geminiRestClient).call(captor.capture());
        assertThat(captor.getValue()).contains(SAMPLE_TEXT);
    }

    @Test
    void extract_promptContainsTrimmedInput_whenInputHasSurroundingWhitespace() {
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(any())).thenReturn(List.of());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        adapter.extract("   " + SAMPLE_TEXT + "   ");

        verify(geminiRestClient).call(captor.capture());
        assertThat(captor.getValue()).contains(SAMPLE_TEXT.trim());
    }

    @Test
    void extract_promptContainsRequiredInstructions() {
        when(geminiRestClient.call(anyString())).thenReturn(STUB_RESPONSE);
        when(parser.parseResponse(any())).thenReturn(List.of());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        adapter.extract(SAMPLE_TEXT);

        verify(geminiRestClient).call(captor.capture());
        String prompt = captor.getValue();
        assertThat(prompt)
                .contains("Return ONLY valid JSON")
                .contains("\"triples\"")
                .contains("subject")
                .contains("predicate")
                .contains("object")
                .contains("OpenIE");
    }


    @Test
    void extract_propagatesException_whenClientFails() {
        when(geminiRestClient.call(anyString()))
                .thenThrow(new GeminiIntegrationException("boom"));

        assertThatThrownBy(() -> adapter.extract(SAMPLE_TEXT))
                .isInstanceOf(GeminiIntegrationException.class);

        verify(parser, never()).parseResponse(any());
    }
}