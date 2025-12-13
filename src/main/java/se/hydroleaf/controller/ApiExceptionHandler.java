package se.hydroleaf.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record FieldErrorResponse(String field, String message) {}

    public record ValidationErrorResponse(List<FieldErrorResponse> errors) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ValidationErrorResponse(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            return invalidFormatResponse(invalidFormat);
        }
        if (cause instanceof UnrecognizedPropertyException unrecognizedProperty) {
            return unrecognizedFieldResponse(unrecognizedProperty);
        }
        return new ValidationErrorResponse(List.of(new FieldErrorResponse("", "Malformed JSON request")));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ValidationErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        FieldErrorResponse error = new FieldErrorResponse("", Objects.requireNonNullElse(ex.getReason(), ""));
        return ResponseEntity.status(ex.getStatusCode()).body(new ValidationErrorResponse(List.of(error)));
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
        String fieldPath = pathFrom(ex.getPath());
        String fieldName = ex.getPropertyName();
        String field = fieldPath.isBlank() ? fieldName : fieldPath + "." + fieldName;
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
