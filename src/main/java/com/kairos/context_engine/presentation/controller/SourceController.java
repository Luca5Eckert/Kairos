package com.kairos.context_engine.presentation.controller;

import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.context_engine.presentation.dto.request.GenerateSourceContextRequest;
import com.kairos.context_engine.presentation.dto.request.UploadSourceRequest;
import com.kairos.context_engine.presentation.dto.response.ContextResponse;
import com.kairos.context_engine.presentation.mapper.SourceMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sources")
public class SourceController {

    private final UploadSourceUseCase uploadSourceUseCase;
    private final SearchSourceUseCase searchSourceUseCase;

    private final SourceMapper mapper;

    public SourceController(UploadSourceUseCase uploadSourceUseCase, SearchSourceUseCase searchSourceUseCase, SourceMapper mapper) {
        this.uploadSourceUseCase = uploadSourceUseCase;
        this.searchSourceUseCase = searchSourceUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<Void> uploadSource(
            @RequestBody @Valid UploadSourceRequest request
    ) {
        var command = UploadSourceCommand.of(
                request.title(),
                request.content(),
                request.authorId()
        );
        uploadSourceUseCase.execute(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping
    public ResponseEntity<ContextResponse> generateSourceContext(
            @RequestBody @Valid GenerateSourceContextRequest request
    ) {
        var query = SearchSourceQuery.of(request.termQuery());
        var result = searchSourceUseCase.execute(query);

        return ResponseEntity.ok(mapper.toContextResponse(result));
    }


}
