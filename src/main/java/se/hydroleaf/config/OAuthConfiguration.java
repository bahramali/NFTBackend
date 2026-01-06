package se.hydroleaf.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(15);

    @Bean
    public WebClient oauthWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler((int) READ_TIMEOUT.toSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) WRITE_TIMEOUT.toSeconds())));
        return builder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
