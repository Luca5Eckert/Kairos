package com.kairos.presentation.controller;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.application.use_case.GenerateSourceContextUseCase;
import com.kairos.application.use_case.UploadSourceUseCase;
import com.kairos.presentation.dto.request.UploadSourceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sources")
public class SourceController {

    private final UploadSourceUseCase uploadSourceUseCase;
    private final GenerateSourceContextUseCase generateSourceContextUseCase;

    public SourceController(UploadSourceUseCase uploadSourceUseCase, GenerateSourceContextUseCase generateSourceContextUseCase) {
        this.uploadSourceUseCase = uploadSourceUseCase;
        this.generateSourceContextUseCase = generateSourceContextUseCase;
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


}
