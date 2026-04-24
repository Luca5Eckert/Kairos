package com.kairos.context_engine.infrastructure.gemini.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;

@Configuration
@EnableResilientMethods
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfiguration {

    @Bean
    public RestClient restClient(GeminiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeoutSeconds() * 1000);
        factory.setReadTimeout(properties.timeoutSeconds() * 1000);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}