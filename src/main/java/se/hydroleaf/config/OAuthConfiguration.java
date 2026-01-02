package se.hydroleaf.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfiguration {

    @Bean
    public WebClient oauthWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
