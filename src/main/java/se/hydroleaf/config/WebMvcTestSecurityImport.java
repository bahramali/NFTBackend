package se.hydroleaf.config;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Profile("test")
@Import(SecurityConfig.class)
public class WebMvcTestSecurityImport {
}
