package se.hydroleaf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("test")
@Import(SecurityConfig.class)
public class WebMvcTestSecurityConfig implements WebMvcConfigurer {
}
