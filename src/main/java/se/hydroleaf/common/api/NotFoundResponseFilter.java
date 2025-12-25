package se.hydroleaf.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class NotFoundResponseFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NotFoundResponseFilter.class);

    private final ObjectMapper objectMapper;

    public NotFoundResponseFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            if (shouldRewriteNotFound(responseWrapper)) {
                log.debug("Responding with JSON 404 for {} {}", request.getMethod(), request.getRequestURI());
                responseWrapper.resetBuffer();
                responseWrapper.setStatus(HttpStatus.NOT_FOUND.value());
                responseWrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(responseWrapper.getOutputStream(), new ApiError("NOT_FOUND", "Resource not found"));
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean shouldRewriteNotFound(ContentCachingResponseWrapper responseWrapper) {
        if (responseWrapper.getStatus() != HttpStatus.NOT_FOUND.value() || responseWrapper.isCommitted()) {
            return false;
        }
        String contentType = responseWrapper.getContentType();
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            byte[] body = responseWrapper.getContentAsByteArray();
            if (body.length > 0) {
                String payload = new String(body, java.nio.charset.StandardCharsets.UTF_8);
                return !payload.contains("\"code\"");
            }
            return true;
        }
        return true;
    }
}
