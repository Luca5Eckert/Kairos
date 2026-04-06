package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import org.springframework.stereotype.Component;

@Component
public class GenerateSourceContextUseCase {

    /**
     * TODO: Implement the logic to generate source context based on the provided command.
     * This method divides the content into manageable chunks and generates context for each chunk.
     * The generated context is then stored in the database for later retrieval.
     * After this, get triples for each chunk and store them in the graph database.
     * @param command the command containing the source ID and content for which the context needs to be generated.
     */
    public void execute(GenerateSourceContextCommand command) {


    }

}
