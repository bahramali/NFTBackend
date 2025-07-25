package se.hydroleaf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://hydroleaf.se", "https://www.hydroleaf.se")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
