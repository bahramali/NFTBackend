package se.hydroleaf.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class LiveFeedConfig {

    @Bean
    public ConcurrentHashMap<String, Instant> lastSeen() {
        return new ConcurrentHashMap<>();
    }
}
