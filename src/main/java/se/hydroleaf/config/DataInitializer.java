package se.hydroleaf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner demoDataInitializer() {
        return args -> log.info("Demo data seed is enabled; no demo users are created by default.");
    }
}
