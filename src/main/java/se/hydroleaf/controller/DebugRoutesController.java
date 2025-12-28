package se.hydroleaf.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import se.hydroleaf.controller.AdminCustomerController;

@RestController
@RequestMapping("/api/_debug/routes")
@ConditionalOnProperty(name = "debug.routes.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DebugRoutesController {

    private final RequestMappingHandlerMapping handlerMapping;
    private final Environment environment;

    @GetMapping("/admin-customers")
    public ResponseEntity<DebugRouteInfo> adminCustomersRoutes() {
        if (!isDebugEnabled()) {
            return ResponseEntity.notFound().build();
        }

        List<RouteMapping> mappings = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> isAdminCustomerHandler(entry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .map(entry -> toRouteMapping(entry.getKey()))
                .collect(Collectors.toList());

        DebugRouteInfo payload = new DebugRouteInfo(
                AdminCustomerController.class.getName(),
                mappings,
                Arrays.asList(environment.getActiveProfiles())
        );
        return ResponseEntity.ok(payload);
    }

    private boolean isDebugEnabled() {
        boolean envEnabled = environment.getProperty("DEBUG_ROUTES", Boolean.class, false);
        boolean profileEnabled = environment.acceptsProfiles(Profiles.of("debug"));
        return envEnabled || profileEnabled;
    }

    private boolean isAdminCustomerHandler(HandlerMethod handlerMethod) {
        return handlerMethod != null && AdminCustomerController.class.equals(handlerMethod.getBeanType());
    }

    private RouteMapping toRouteMapping(RequestMappingInfo mappingInfo) {
        List<String> patterns = mappingInfo.getPatternValues().stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        List<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .map(name -> name.toUpperCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.toList());
        return new RouteMapping(patterns, methods);
    }

    public record DebugRouteInfo(String controller, List<RouteMapping> mappings, List<String> activeProfiles) {
    }

    public record RouteMapping(List<String> patterns, List<String> methods) {
    }
}
