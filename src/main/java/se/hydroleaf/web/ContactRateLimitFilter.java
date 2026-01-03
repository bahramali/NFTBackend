package se.hydroleaf.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import se.hydroleaf.config.ContactProperties;
import se.hydroleaf.config.CorsProperties;

@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class ContactRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContactRateLimitFilter.class);
    private static final List<String> ALLOWED_METHODS = List.of("POST", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of(
            "Content-Type",
            "X-Requested-With"
    );

    private final ContactProperties contactProperties;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!"/api/contact".equals(uri)) {
            return true;
        }

        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContactProperties.RateLimitProperties rate = contactProperties.getRateLimit();
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
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        boolean suspectedBot = isSuspectedBot(request);
        log.warn(
                "contact_message_dropped requestId={} reason=rate_limit ip={} userAgent={} suspectedBot={}",
                requestId,
                key,
                request.getHeader(HttpHeaders.USER_AGENT),
                suspectedBot
        );
        handleRateLimitExceeded(request, response, rate, suspectedBot);
    }

    private String resolveKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote : "unknown";
    }

    private Bucket newBucket(ContactProperties.RateLimitProperties rate, String key) {
        ContactProperties.LimitProperties perMinute = rate.getPerMinute() == null
                ? new ContactProperties.LimitProperties(5, 5, 60)
                : rate.getPerMinute();
        ContactProperties.LimitProperties perDay = rate.getPerDay() == null
                ? new ContactProperties.LimitProperties(20, 20, 86_400)
                : rate.getPerDay();

        long minuteCapacity = Math.max(1, perMinute.getCapacity());
        long minuteRefillTokens = Math.max(1, perMinute.getRefillTokens());
        long minuteRefillSeconds = Math.max(1, perMinute.getRefillSeconds());

        long dayCapacity = Math.max(1, perDay.getCapacity());
        long dayRefillTokens = Math.max(1, perDay.getRefillTokens());
        long dayRefillSeconds = Math.max(1, perDay.getRefillSeconds());

        Bandwidth minuteLimit = Bandwidth.classic(
                minuteCapacity,
                Refill.intervally(minuteRefillTokens, Duration.ofSeconds(minuteRefillSeconds))
        );
        Bandwidth dayLimit = Bandwidth.classic(
                dayCapacity,
                Refill.intervally(dayRefillTokens, Duration.ofSeconds(dayRefillSeconds))
        );

        log.debug(
                "Creating contact rate limit bucket for {} with minute cap {} refill {} per {}s and day cap {} refill {} per {}s",
                key,
                minuteCapacity,
                minuteRefillTokens,
                minuteRefillSeconds,
                dayCapacity,
                dayRefillTokens,
                dayRefillSeconds
        );

        return Bucket.builder()
                .addLimit(minuteLimit)
                .addLimit(dayLimit)
                .build();
    }

    private void handleRateLimitExceeded(
            HttpServletRequest request,
            HttpServletResponse response,
            ContactProperties.RateLimitProperties rate,
            boolean suspectedBot
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        if (suspectedBot) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            applyCorsHeaders(request, response);
            return;
        }

        ContactProperties.LimitProperties perMinute = rate.getPerMinute() == null
                ? new ContactProperties.LimitProperties(5, 5, 60)
                : rate.getPerMinute();
        long retryAfterSeconds = Math.max(1, perMinute.getRefillSeconds());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        applyCorsHeaders(request, response);

        ApiError apiError = new ApiError("RATE_LIMITED", "Too many requests, please slow down");
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }

    private boolean isSuspectedBot(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (!StringUtils.hasText(userAgent)) {
            return true;
        }
        String lowered = userAgent.toLowerCase();
        return lowered.contains("bot")
                || lowered.contains("crawler")
                || lowered.contains("spider")
                || lowered.contains("scraper");
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
