package com.kairos.module.context_engine.presentation.controller;

import com.kairos.module.context_engine.application.use_case.GetHistoryAnswerUseCase;
import com.kairos.module.context_engine.application.use_case.GetHistoryQuestionUseCase;
import com.kairos.module.context_engine.application.use_case.ListHistoryAnswersUseCase;
import com.kairos.module.context_engine.application.use_case.ListHistoryQuestionsUseCase;
import com.kairos.module.context_engine.presentation.dto.response.AnswerHistoryResponse;
import com.kairos.module.context_engine.presentation.dto.response.AnswerHistorySummaryResponse;
import com.kairos.module.context_engine.presentation.dto.response.HistoryPageResponse;
import com.kairos.module.context_engine.presentation.dto.response.QuestionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {
    private final ListHistoryQuestionsUseCase listHistoryQuestionsUseCase;
    private final GetHistoryQuestionUseCase getHistoryQuestionUseCase;
    private final ListHistoryAnswersUseCase listHistoryAnswersUseCase;
    private final GetHistoryAnswerUseCase getHistoryAnswerUseCase;

    @GetMapping("/questions")
    public ResponseEntity<HistoryPageResponse<QuestionHistoryResponse>> listQuestions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        var result = listHistoryQuestionsUseCase.execute(page, size);
        return ResponseEntity.ok(HistoryPageResponse.of(result, QuestionHistoryResponse::of));
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionHistoryResponse> getQuestion(@PathVariable UUID questionId) {
        return ResponseEntity.ok(QuestionHistoryResponse.of(getHistoryQuestionUseCase.execute(questionId)));
    }

    @GetMapping("/questions/{questionId}/answers")
    public ResponseEntity<HistoryPageResponse<AnswerHistorySummaryResponse>> listAnswers(
            @PathVariable UUID questionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        var result = listHistoryAnswersUseCase.execute(questionId, page, size);
        return ResponseEntity.ok(HistoryPageResponse.of(result, AnswerHistorySummaryResponse::of));
    }

    @GetMapping("/answers/{answerId}")
    public ResponseEntity<AnswerHistoryResponse> getAnswer(@PathVariable UUID answerId) {
        return ResponseEntity.ok(AnswerHistoryResponse.of(getHistoryAnswerUseCase.execute(answerId)));
    }
}
