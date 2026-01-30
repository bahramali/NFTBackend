package se.hydroleaf.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.MonitoringPageDetailResponse;
import se.hydroleaf.controller.dto.MonitoringPageResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.MonitoringPageService;

@RestController
@RequestMapping("/api/monitoring-pages")
@RequiredArgsConstructor
public class MonitoringPageController {

    private final MonitoringPageService monitoringPageService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public List<MonitoringPageResponse> listEnabledPages(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.requireMonitoringView(token);
        return monitoringPageService.listEnabledPages();
    }

    @GetMapping("/{slug}")
    public MonitoringPageDetailResponse getEnabledPage(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String slug) {
        authorizationService.requireMonitoringView(token);
        return monitoringPageService.getEnabledPageBySlug(slug);
    }
}
