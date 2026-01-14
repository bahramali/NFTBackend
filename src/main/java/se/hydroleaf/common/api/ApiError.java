package se.hydroleaf.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String code;
    private final String message;
    private final Instant timestamp = Instant.now();
    private final String path;
    private final Map<String, Object> details;

    public ApiError(String code, String message) {
        this(code, message, null, null);
    }

    public ApiError(String code, String message, Map<String, Object> details) {
        this(code, message, null, details);
    }

    public ApiError(String code, String message, String path, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
