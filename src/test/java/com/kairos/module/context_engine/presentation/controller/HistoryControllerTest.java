package com.kairos.module.context_engine.presentation.controller;

import com.kairos.module.context_engine.application.use_case.GetHistoryAnswerUseCase;
import com.kairos.module.context_engine.application.use_case.GetHistoryQuestionUseCase;
import com.kairos.module.context_engine.application.use_case.ListHistoryAnswersUseCase;
import com.kairos.module.context_engine.application.use_case.ListHistoryQuestionsUseCase;
import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import com.kairos.module.context_engine.domain.model.history.HistoryPage;
import com.kairos.module.context_engine.domain.model.history.QuestionHistory;
import com.kairos.share.exception.GlobalHandlerException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {
    @Mock private ListHistoryQuestionsUseCase listQuestions;
    @Mock private GetHistoryQuestionUseCase getQuestion;
    @Mock private ListHistoryAnswersUseCase listAnswers;
    @Mock private GetHistoryAnswerUseCase getAnswer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new HistoryController(listQuestions, getQuestion, listAnswers, getAnswer))
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void listsQuestionsWithPaginationMetadata() throws Exception {
        UUID questionId = UUID.randomUUID();
        var question = new QuestionHistory(questionId, UUID.randomUUID(), "How?", Instant.parse("2026-01-01T00:00:00Z"), 2, Instant.parse("2026-01-02T00:00:00Z"));
        when(listQuestions.execute(0, 20)).thenReturn(new HistoryPage<>(List.of(question), 0, 20, 21));

        mockMvc.perform(get("/history/questions").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.content[0].answerCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void returnsCompleteAnswerSnapshotFromDedicatedEndpoint() throws Exception {
        UUID answerId = UUID.randomUUID();
        var snapshot = new AnswerSnapshot("hipporag-2",
                new AnswerSnapshot.RetrievalParameters(10, 30, 10, 20, 0.45, 0.85), List.of(), List.of(), List.of());
        when(getAnswer.execute(answerId)).thenReturn(new Answer(answerId, UUID.randomUUID(), 1, snapshot,
                Instant.parse("2026-01-02T00:00:00Z")));

        mockMvc.perform(get("/history/answers/{answerId}", answerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId.toString()))
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.snapshot.retrievalVersion").value("hipporag-2"))
                .andExpect(jsonPath("$.snapshot.parameters.graphPassageLimit").value(20));
    }

    @Test
    void mapsMissingHistoryToNotFound() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(getQuestion.execute(questionId)).thenThrow(new EntityNotFoundException("History resource not found"));

        mockMvc.perform(get("/history/questions/{questionId}", questionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.path").value("/history/questions/" + questionId));
    }
}
