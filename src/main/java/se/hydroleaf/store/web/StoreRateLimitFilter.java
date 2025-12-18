package se.hydroleaf.store.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Map<String, CounterWindow> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/store");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        CounterWindow window = buckets.computeIfAbsent(key, k -> new CounterWindow());
        if (consume(window)) {
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
        return request.getRemoteAddr();
    }

    private boolean consume(CounterWindow window) {
        StoreProperties.RateLimitProperties rate = storeProperties.getRateLimit();
        long limit = Math.max(1, Math.min(rate.getCapacity(), rate.getRefillTokens()));
        long now = System.currentTimeMillis();
        long windowMs = Duration.ofSeconds(rate.getRefillSeconds()).toMillis();
        synchronized (window) {
            if (now - window.windowStart >= windowMs) {
                window.windowStart = now;
                window.count.set(0);
            }
            if (window.count.get() >= limit) {
                return false;
            }
            window.count.incrementAndGet();
            return true;
        }
    }

    private static class CounterWindow {
        private long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger();
    }
}
