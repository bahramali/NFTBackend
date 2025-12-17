package se.hydroleaf.store.web;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Refill;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import se.hydroleaf.common.api.RateLimitException;
import se.hydroleaf.store.config.StoreProperties;

@Component
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
        Bucket bucket = buckets.computeIfAbsent(resolveKey(request), this::newBucket);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Rate limit exceeded for {}", request.getRemoteAddr());
        throw new RateLimitException("RATE_LIMITED", "Too many requests, please slow down");
    }

    private String resolveKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket newBucket(String key) {
        StoreProperties.RateLimitProperties rate = storeProperties.getRateLimit();
        log.debug("Creating rate limit bucket for {}", key);
        Bandwidth limit = Bandwidth.classic(rate.getCapacity(), Refill.intervally(rate.getRefillTokens(), Duration.ofSeconds(rate.getRefillSeconds())));
        return Bucket.builder().addLimit(limit).build();
    }
}
