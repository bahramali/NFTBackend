package se.hydroleaf.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LiveFeedConfig {

    @Bean
    public LastSeenRegistry lastSeenRegistry(
            @Value("${lastseen.max-age-seconds:300}") long maxAgeSeconds,
            @Value("${lastseen.max-size:0}") int maxSize) {
        Duration age = maxAgeSeconds > 0 ? Duration.ofSeconds(maxAgeSeconds) : null;
        return new LastSeenRegistry(age, maxSize);
    }
}

