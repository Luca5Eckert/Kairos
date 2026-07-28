package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.share.security.context.RequestContextProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetAllSourceProgressUploadUseCase {

    private final SourceRepository sourceRepository;

    private final RequestContextProvider contextProvider;

    public GetAllSourceProgressUploadUseCase(SourceRepository sourceRepository, RequestContextProvider contextProvider) {
        this.sourceRepository = sourceRepository;
        this.contextProvider = contextProvider;
    }

    public List<SourceProgressUpload> execute() {
        var request = contextProvider.getRequestContext();

        return sourceRepository.findAllSourcesProgressByAuthorId(request.userId());
    }

}
