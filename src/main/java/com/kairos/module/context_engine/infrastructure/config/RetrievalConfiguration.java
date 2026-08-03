package com.kairos.module.context_engine.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RetrievalProperties.class, HistoryProperties.class})
public class RetrievalConfiguration {
}
