package se.hydroleaf.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import se.hydroleaf.service.InvalidPermissionException;

@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record FieldErrorResponse(String field, String message) {}

    public record ValidationErrorResponse(List<FieldErrorResponse> errors) {}

    public record InvalidPermissionsResponse(List<FieldErrorResponse> errors, List<String> invalidPermissions) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        if (log.isDebugEnabled()) {
            log.debug("Validation failed: {}", errors);
        }
        return new ValidationErrorResponse(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid format in request body", invalidFormat);
            }
            return invalidFormatResponse(invalidFormat);
        }
        if (cause instanceof UnrecognizedPropertyException unrecognizedProperty) {
            if (log.isDebugEnabled()) {
                log.debug("Unrecognized field in request body", unrecognizedProperty);
            }
            return unrecognizedFieldResponse(unrecognizedProperty);
        }
        if (log.isDebugEnabled()) {
            log.debug("Unreadable request body", ex);
        }
        return new ValidationErrorResponse(List.of(new FieldErrorResponse("", "Malformed JSON request")));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ValidationErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        FieldErrorResponse error = new FieldErrorResponse("", Objects.requireNonNullElse(ex.getReason(), ""));
        return ResponseEntity.status(ex.getStatusCode()).body(new ValidationErrorResponse(List.of(error)));
    }

    @ExceptionHandler(InvalidPermissionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InvalidPermissionsResponse handleInvalidPermissions(InvalidPermissionException ex) {
        FieldErrorResponse error = new FieldErrorResponse("permissions", ex.getMessage());
        return new InvalidPermissionsResponse(List.of(error), ex.invalidPermissions());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ValidationErrorResponse> handleSecurity(SecurityException ex) {
        FieldErrorResponse error = new FieldErrorResponse("", ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("Authorization failed: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(org.springframework.http.HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(new ValidationErrorResponse(List.of(error)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ValidationErrorResponse(List.of(new FieldErrorResponse("", ex.getMessage())));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        FieldErrorResponse error = new FieldErrorResponse("", "Request method '" + ex.getMethod() + "' not supported");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ValidationErrorResponse(List.of(error)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ValidationErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        FieldErrorResponse error = new FieldErrorResponse("", "Resource not found");
        if (log.isDebugEnabled()) {
            log.debug("Resource not found: {}", ex.getResourcePath());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ValidationErrorResponse(List.of(error)));
    }

    private ValidationErrorResponse invalidFormatResponse(InvalidFormatException ex) {
        String field = pathFrom(ex.getPath());
        if (ex.getTargetType().isEnum()) {
            Object[] constants = ex.getTargetType().getEnumConstants();
            String allowed = constants == null
                    ? ""
                    : Arrays.stream(constants)
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
            String message = "Invalid value '%s'. Allowed values: %s".formatted(ex.getValue(), allowed);
            return new ValidationErrorResponse(List.of(new FieldErrorResponse(field, message)));
        }
        return new ValidationErrorResponse(List.of(new FieldErrorResponse(field, "Invalid value")));
    }

    private ValidationErrorResponse unrecognizedFieldResponse(UnrecognizedPropertyException ex) {
        List<JsonMappingException.Reference> path = ex.getPath();
        String fieldName = ex.getPropertyName();
        String parentPath = (path == null || path.isEmpty())
                ? ""
                : pathFrom(path.subList(0, path.size() - 1));
        String field = parentPath.isBlank() ? fieldName : parentPath + "." + fieldName;
        return new ValidationErrorResponse(List.of(new FieldErrorResponse(field, "Unrecognized field '%s'".formatted(fieldName))));
    }

    private String pathFrom(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.stream()
                .map(ref -> {
                    if (ref.getFieldName() != null) {
                        return ref.getFieldName();
                    }
                    if (ref.getIndex() >= 0) {
                        return "[%d]".formatted(ref.getIndex());
                    }
                    return "";
                })
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }
}
