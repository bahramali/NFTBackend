package se.hydroleaf.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.CustomersPageResponse;
import se.hydroleaf.store.service.AdminCustomerService;

@RestController
@RequestMapping(AdminCustomerController.BASE_MAPPING)
@RequiredArgsConstructor
public class AdminCustomerController {

    static final String BASE_MAPPING = "/api/admin/customers";

    private static final Logger log = LoggerFactory.getLogger(AdminCustomerController.class);

    private final AuthorizationService authorizationService;
    private final AdminCustomerService adminCustomerService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ObjectProvider<GitProperties> gitPropertiesProvider;

    @PostConstruct
    public void logControllerLoaded() {
        String buildInfo = resolveBuildInfo();
        if (buildInfo == null || buildInfo.isBlank()) {
            log.info("AdminCustomerController LOADED, baseMapping={}, class={}", BASE_MAPPING, getClass().getName());
            return;
        }
        log.info("AdminCustomerController LOADED, baseMapping={}, buildInfo={}", BASE_MAPPING, buildInfo);
    }

    @GetMapping
    public CustomersPageResponse list(
            HttpServletRequest request,
            @RequestHeader(name = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "last_order_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String query = request.getQueryString();
        String fullPath = request.getRequestURL().toString();
        if (query != null && !query.isBlank()) {
            fullPath = fullPath + "?" + query;
        }
        log.info("AdminCustomerController request start requestId={} method={} path={} sort={} page={} size={}",
                requestId,
                request.getMethod(),
                fullPath,
                sort,
                page,
                size);

        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireRoleOrPermission(
                user,
                Permission.CUSTOMERS_VIEW,
                UserRole.ADMIN
        );

        CustomersPageResponse response = adminCustomerService.list(sort, page, size);
        log.info("AdminCustomerController request complete requestId={} status=200 method={} path={} sort={} page={} size={} totalItems={}",
                requestId,
                request.getMethod(),
                fullPath,
                sort,
                page,
                size,
                response.getTotalItems());
        return response;
    }

    private String resolveBuildInfo() {
        GitProperties gitProperties = gitPropertiesProvider.getIfAvailable();
        if (gitProperties != null && gitProperties.getShortCommitId() != null) {
            return "git:" + gitProperties.getShortCommitId();
        }
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null && buildProperties.getVersion() != null) {
            return "version:" + buildProperties.getVersion();
        }
        return null;
    }
}
