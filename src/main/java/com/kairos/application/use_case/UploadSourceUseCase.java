package com.kairos.application.use_case;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.IngestionPipeline;
import com.kairos.domain.port.SourceRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final IngestionPipeline ingestionPipeline;
    private final Executor virtualThreadExecutor;

    public UploadSourceUseCase(
            SourceRepository sourceRepository,
            IngestionPipeline ingestionPipeline,
            Executor virtualThreadExecutor) {
        this.sourceRepository = sourceRepository;
        this.ingestionPipeline = ingestionPipeline;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public UUID execute(UploadSourceCommand command) {
        var source = new Source(command.title(), command.content());
        sourceRepository.save(source);

        CompletableFuture.runAsync(() -> process(source), virtualThreadExecutor);

        return source.getId();
    }

    private void process(Source source) {
        source.markProcessing();
        sourceRepository.save(source);

        var result = ingestionPipeline.run(source);

        source.markCompleted();

        switch (result) {
            case PARTIAL_FAILURE -> source.markPartialFailure();
            case SUCCESS -> source.markCompleted();
        }
        sourceRepository.save(source);
    }

}