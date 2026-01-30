package se.hydroleaf.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.MonitoringPageCreateRequest;
import se.hydroleaf.controller.dto.MonitoringPageDetailResponse;
import se.hydroleaf.controller.dto.MonitoringPageResponse;
import se.hydroleaf.controller.dto.MonitoringPageUpdateRequest;
import se.hydroleaf.model.MonitoringPage;
import se.hydroleaf.repository.MonitoringPageRepository;

@Service
@RequiredArgsConstructor
public class MonitoringPageService {

    private final MonitoringPageRepository monitoringPageRepository;

    public List<MonitoringPageResponse> listEnabledPages() {
        return monitoringPageRepository.findAllByEnabledTrueOrderBySortOrderAscTitleAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MonitoringPageDetailResponse getEnabledPageBySlug(String slug) {
        MonitoringPage page = monitoringPageRepository.findBySlugAndEnabledTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoring page not found"));
        return toDetailResponse(page);
    }

    public List<MonitoringPageDetailResponse> listAllPages() {
        return monitoringPageRepository.findAllByOrderBySortOrderAscTitleAsc()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    public MonitoringPageDetailResponse createPage(MonitoringPageCreateRequest request) {
        if (monitoringPageRepository.existsByRackId(request.rackId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Monitoring page already exists for rack");
        }
        if (monitoringPageRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Monitoring page slug already exists");
        }
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;
        boolean enabled = request.enabled() == null || request.enabled();
        MonitoringPage page = MonitoringPage.builder()
                .rackId(request.rackId())
                .title(request.title())
                .slug(request.slug())
                .sortOrder(sortOrder)
                .enabled(enabled)
                .build();
        return toDetailResponse(monitoringPageRepository.save(page));
    }

    public MonitoringPageDetailResponse updatePage(Long id, MonitoringPageUpdateRequest request) {
        MonitoringPage page = monitoringPageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoring page not found"));
        if (monitoringPageRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Monitoring page slug already exists");
        }
        page.setTitle(request.title());
        page.setSlug(request.slug());
        page.setSortOrder(request.sortOrder() != null ? request.sortOrder() : page.getSortOrder());
        if (request.enabled() != null) {
            page.setEnabled(request.enabled());
        }
        return toDetailResponse(monitoringPageRepository.save(page));
    }

    public void deletePage(Long id) {
        MonitoringPage page = monitoringPageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoring page not found"));
        monitoringPageRepository.delete(page);
    }

    private MonitoringPageResponse toResponse(MonitoringPage page) {
        return new MonitoringPageResponse(
                page.getId(),
                page.getTitle(),
                page.getSlug(),
                page.getRackId(),
                page.getSortOrder()
        );
    }

    private MonitoringPageDetailResponse toDetailResponse(MonitoringPage page) {
        return new MonitoringPageDetailResponse(
                page.getId(),
                page.getTitle(),
                page.getSlug(),
                page.getRackId(),
                page.getSortOrder(),
                page.isEnabled()
        );
    }
}
