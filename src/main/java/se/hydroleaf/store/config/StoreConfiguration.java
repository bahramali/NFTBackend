package se.hydroleaf.store.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StoreProperties.class, StripeProperties.class, NetsEasyProperties.class})
public class StoreConfiguration {
}
