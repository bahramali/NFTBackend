package se.hydroleaf.common.api;

import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API error on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getCode(), ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> b));
        log.warn("Validation failed on {} {}: {}", request.getMethod(), request.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> details.put(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Constraint violation on {} {}: {}", request.getMethod(), request.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Request failed with status {} on {} {}: {}", ex.getStatusCode(), request.getMethod(), request.getRequestURI(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiError("REQUEST_FAILED", ex.getReason()));
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiError> handleStripeException(StripeException ex, HttpServletRequest request) {
        log.warn("Stripe request failed on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        Integer statusCode = ex.getStatusCode();
        if (statusCode != null) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved != null) {
                status = resolved;
            }
        }
        StripeErrorMapper.StripeError error = StripeErrorMapper.fromException(ex);
        return ResponseEntity.status(status)
                .body(new ApiError(error.code(), error.message()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFound(NoResourceFoundException ex) {
        if (log.isDebugEnabled()) {
            log.debug("Static resource not found: {}", ex.getResourcePath());
        }
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("No handler found for {} {}", request.getMethod(), request.getRequestURI());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Unexpected server error"));
    }
}
