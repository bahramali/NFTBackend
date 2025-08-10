package se.hydroleaf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

      @Override
      public void addViewControllers(ViewControllerRegistry registry) {
        // Any single-segment track without a dot
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    
        // Any multi-segment path whose last segment has no dots
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
      }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://hydroleaf.se", "https://www.hydroleaf.se")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
