package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.MonitoringPageCreateRequest;
import se.hydroleaf.controller.dto.MonitoringPageDetailResponse;
import se.hydroleaf.controller.dto.MonitoringPageUpdateRequest;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.MonitoringPageService;

@RestController
@RequestMapping("/api/admin/monitoring-pages")
@RequiredArgsConstructor
public class AdminMonitoringPageController {

    private final AuthorizationService authorizationService;
    private final MonitoringPageService monitoringPageService;

    @GetMapping
    public List<MonitoringPageDetailResponse> listPages(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.requireMonitoringConfig(token);
        return monitoringPageService.listAllPages();
    }

    @PostMapping
    public MonitoringPageDetailResponse createPage(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody MonitoringPageCreateRequest request) {
        authorizationService.requireMonitoringConfig(token);
        return monitoringPageService.createPage(request);
    }

    @PutMapping("/{id}")
    public MonitoringPageDetailResponse updatePage(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody MonitoringPageUpdateRequest request) {
        authorizationService.requireMonitoringConfig(token);
        return monitoringPageService.updatePage(id, request);
    }

    @DeleteMapping("/{id}")
    public void deletePage(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id) {
        authorizationService.requireMonitoringConfig(token);
        monitoringPageService.deletePage(id);
    }
}
