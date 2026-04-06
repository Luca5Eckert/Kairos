package com.kairos.application.use_case;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.EmbeddingProvider;
import com.kairos.domain.port.SourceRepository;
import org.springframework.stereotype.Component;

@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final EmbeddingProvider embeddingProvider;

    public UploadSourceUseCase(SourceRepository sourceRepository, EmbeddingProvider embeddingProvider) {
        this.sourceRepository = sourceRepository;
        this.embeddingProvider = embeddingProvider;
    }

    public void execute(UploadSourceCommand command){
        var embedding = embeddingProvider.embed(command.content());

        var source = new Source(
                command.title(),
                command.content(),
                embedding
        );

        sourceRepository.save(source);

    }

}
