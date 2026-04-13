package com.kairos.infrastructure.context.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SourceContextJobProperties.class)
public class SourceContextConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
