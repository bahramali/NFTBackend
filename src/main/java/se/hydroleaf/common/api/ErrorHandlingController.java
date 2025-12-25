package se.hydroleaf.common.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorHandlingController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingController.class);

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiError> handleError(HttpServletRequest request) {
        Object statusValue = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = statusValue instanceof Integer
                ? (Integer) statusValue
                : statusValue == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : Integer.parseInt(statusValue.toString());
        HttpStatus status = HttpStatus.resolve(statusCode);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (requestUri == null) {
            requestUri = request.getRequestURI();
        }

        if (status == HttpStatus.NOT_FOUND) {
            log.debug("No handler or static resource found for {} {}", request.getMethod(), requestUri);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(new ApiError("NOT_FOUND", "Resource not found"));
        }

        log.error("Unexpected error on {} {} with status {}", request.getMethod(), requestUri, statusCode);
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new ApiError("INTERNAL_ERROR", "Unexpected server error"));
    }
}
