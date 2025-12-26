package se.hydroleaf.common.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

@RestController
@RequestMapping("${server.error.path:/error}")
public class ApiErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorController.class);

    private final ErrorAttributes errorAttributes;

    public ApiErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        HttpStatus status = statusFrom(request);
        if (status == HttpStatus.NOT_FOUND) {
            String method = request.getMethod();
            String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            if (log.isDebugEnabled()) {
                log.debug("Request not found on {} {}", method, path);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "Resource not found"));
        }
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request),
                ErrorAttributeOptions.defaults());
        return ResponseEntity.status(status).body(attributes);
    }

    private HttpStatus statusFrom(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return HttpStatus.valueOf(code);
        }
        if (statusCode instanceof String code) {
            return HttpStatus.valueOf(Integer.parseInt(code));
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
