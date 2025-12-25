package se.hydroleaf.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SuspiciousPathFilter extends OncePerRequestFilter {

    private static final String RESPONSE_BODY = "{\"error\":\"not_found\"}";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !isSuspiciousPath(path);
    }

    boolean isSuspiciousPath(String path) {
        String normalized = path.toLowerCase();
        return normalized.endsWith(".php")
                || normalized.contains("/wp-")
                || normalized.equals("/xmlrpc.php")
                || normalized.contains("/.env")
                || normalized.contains("/vendor/")
                || normalized.contains("/cgi-bin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(RESPONSE_BODY);
    }
}
