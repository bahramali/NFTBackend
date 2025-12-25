package se.hydroleaf.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class BotProbeFilter extends OncePerRequestFilter {

    private static final List<String> CONTAINS_BLOCK = List.of("/wp-", "/cgi-bin/", "/.env");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !isSuspiciousPath(path);
    }

    boolean isSuspiciousPath(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".php") || CONTAINS_BLOCK.stream().anyMatch(normalized::contains);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
