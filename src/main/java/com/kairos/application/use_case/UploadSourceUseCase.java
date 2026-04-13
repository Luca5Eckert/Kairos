package com.kairos.application.use_case;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.port.SourceRepository;
import com.kairos.domain.model.Source;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for uploading a source. It creates a new source, saves it to the repository,
 * and publishes a CreatedSourceEvent.
 */
@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final GenerateSourceContextUseCase generateSourceContextUseCase;

    public UploadSourceUseCase(SourceRepository sourceRepository, GenerateSourceContextUseCase generateSourceContextUseCase) {
        this.sourceRepository = sourceRepository;
        this.generateSourceContextUseCase = generateSourceContextUseCase;
    }

    /**
     * Executes the upload source use case. It creates a new source based on the provided command,
     * @param command The command containing information to upload source.
     * @return The unique identifier of the newly created source.
     */
    @Transactional
    public UUID execute(UploadSourceCommand command) {
        var source = Source.create(command.title(), command.content());

        sourceRepository.save(source);

        generateSourceContextUseCase.execute(GenerateSourceContextCommand.of(source.getId(), source.getContent()));

        return source.getId();
    }

}
