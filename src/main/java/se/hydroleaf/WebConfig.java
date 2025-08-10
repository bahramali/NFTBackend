package se.hydroleaf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
@Configuration
public class WebConfig implements WebMvcConfigurer {

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
                .allowedOrigins("https://hydroleaf.se", "https://www.hydroleaf.se")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
