package se.hydroleaf;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import se.hydroleaf.config.CorsProperties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({CorsProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Single-segment paths without dots (e.g. /reports)
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");

        // multi-segment paths without dots (e.g. /dashboard/layer/L01)
        registry.addViewController("/{path:[^\\.]*}/**")
                .setViewName("forward:/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
