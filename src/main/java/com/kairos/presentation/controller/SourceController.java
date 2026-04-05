package com.kairos.presentation.controller;

import com.kairos.presentation.dto.request.UploadSourceRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sources")
public class SourceController {

    @PostMapping
    public ResponseEntity<Void> uploadSource(
            @RequestBody @Valid UploadSourceRequest request
    ) {
        return ResponseEntity.ok().build();
    }


}
