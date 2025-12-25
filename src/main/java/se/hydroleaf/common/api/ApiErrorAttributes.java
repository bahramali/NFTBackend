package se.hydroleaf.common.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorAttributes.class);

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
        Object statusValue = attributes.get("status");
        if (statusValue instanceof Integer status && status == HttpStatus.NOT_FOUND.value()) {
            log.debug("Generating JSON 404 error response");
            Map<String, Object> minimal = new LinkedHashMap<>();
            minimal.put("code", "NOT_FOUND");
            minimal.put("message", "Resource not found");
            return minimal;
        }
        return attributes;
    }
}
