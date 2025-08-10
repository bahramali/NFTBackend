package se.hydroleaf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:^(?!api$)[^\\.]*}")
                .setViewName("forward:/index.html");
        registry.addViewController("/{path:^(?!api$)[^\\.]*}/**/{subpath:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
