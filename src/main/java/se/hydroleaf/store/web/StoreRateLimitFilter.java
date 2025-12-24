package se.hydroleaf.store.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import se.hydroleaf.common.api.ApiError;
import se.hydroleaf.config.CorsProperties;
import se.hydroleaf.store.config.StoreProperties;

@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class StoreRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StoreRateLimitFilter.class);
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "X-Cart-Id"
    );

    private final StoreProperties storeProperties;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/store/") && !"/api/store".equals(uri)) {
            return true;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        return "/api/health".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        StoreProperties.RateLimitProperties rate = storeProperties.getRateLimit();
        if (rate == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(rate, k));
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Rate limit exceeded for {}", key);
        handleRateLimitExceeded(request, response, rate);
    }

    private String resolveKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote : "unknown";
    }

    private Bucket newBucket(StoreProperties.RateLimitProperties rate, String key) {
        long capacity = Math.max(1, rate.getCapacity());
        long refillTokens = Math.max(1, rate.getRefillTokens());
        long refillSeconds = Math.max(1, rate.getRefillSeconds());

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillSeconds))
        );

        log.debug(
                "Creating rate limit bucket for {} with cap {} refill {} per {}s",
                key, capacity, refillTokens, refillSeconds
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, StoreProperties.RateLimitProperties rate) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        long retryAfterSeconds = Math.max(1, rate.getRefillSeconds());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        applyCorsHeaders(request, response);

        ApiError apiError = new ApiError("RATE_LIMITED", "Too many requests, please slow down");
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (!StringUtils.hasText(origin)) {
            return;
        }

        if (corsProperties.getAllowedOrigins().contains(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(",", ALLOWED_METHODS));
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", ALLOWED_HEADERS));
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.RETRY_AFTER);
        }
    }
}
