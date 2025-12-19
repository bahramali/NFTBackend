package se.hydroleaf.store.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import se.hydroleaf.common.api.RateLimitException;
import se.hydroleaf.store.config.StoreProperties;

@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class StoreRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StoreRateLimitFilter.class);

    private final StoreProperties storeProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/store");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        StoreProperties.RateLimitProperties rate = storeProperties.getRateLimit();
        if (rate == null) {
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
        throw new RateLimitException("RATE_LIMITED", "Too many requests, please slow down");
    }

    private String resolveKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
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

}
