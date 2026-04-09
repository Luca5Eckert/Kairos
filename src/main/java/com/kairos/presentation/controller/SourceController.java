package com.kairos.presentation.controller;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.application.query.SearchSourceQuery;
import com.kairos.application.use_case.GenerateSourceContextUseCase;
import com.kairos.application.use_case.SearchSourceUseCase;
import com.kairos.application.use_case.UploadSourceUseCase;
import com.kairos.domain.model.SearchResult;
import com.kairos.presentation.dto.request.GenerateSourceContextRequest;
import com.kairos.presentation.dto.request.UploadSourceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sources")
public class SourceController {

    private final UploadSourceUseCase uploadSourceUseCase;
    private final SearchSourceUseCase searchSourceUseCase;

    public SourceController(UploadSourceUseCase uploadSourceUseCase, SearchSourceUseCase searchSourceUseCase) {
        this.uploadSourceUseCase = uploadSourceUseCase;
        this.searchSourceUseCase = searchSourceUseCase;
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
    public ResponseEntity<SearchResult> generateSourceContext(
            @RequestBody @Valid GenerateSourceContextRequest request
    ) {
        var query = SearchSourceQuery.of(request.termQuery());
        var context = searchSourceUseCase.execute(query);

        return ResponseEntity.ok(context);
    }


}
