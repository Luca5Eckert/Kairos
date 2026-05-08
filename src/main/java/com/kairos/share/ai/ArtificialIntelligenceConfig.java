package com.kairos.share.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArtificialIntelligenceConfig {

    @Bean
    ChatClient tripleExtractionChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are an information extraction engine.
                        Extract factual subject-predicate-object triples from the user's text.
                        Do not invent facts.
                        Return only information supported by the input.
                        """
                )
                .build();
    }

}
