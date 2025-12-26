package se.hydroleaf.config;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@ConditionalOnBean(RequestMappingHandlerMapping.class)
@RequiredArgsConstructor
@Slf4j
public class RequestMappingStartupLogger implements ApplicationRunner {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();
        log.info("Registered request mappings ({} total):", mappings.size());
        mappings.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> logMapping(entry.getKey(), entry.getValue()));
    }

    private void logMapping(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        Set<String> patterns = mappingInfo.getPatternValues();
        Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        String methodsLabel = methods.isEmpty() ? "ANY" : String.join(",", methods);
        String patternLabel = patterns.isEmpty() ? "(no patterns)" : String.join(", ", patterns);
        String handlerLabel = handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        log.info("{} {} -> {}", methodsLabel, patternLabel, handlerLabel);
        if (patternLabel.contains("/api/me")) {
            log.info("Detected /api/me mapping: {} {} -> {}", methodsLabel, patternLabel, handlerLabel);
        }
    }
}
