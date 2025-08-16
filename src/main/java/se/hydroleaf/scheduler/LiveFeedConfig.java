package se.hydroleaf.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveFeedConfig {

    @Bean
    public LastSeenRegistry lastSeenRegistry() {
        return new LastSeenRegistry();
    }
}

