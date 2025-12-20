package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.repository.dto.TopicSensorsResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.TopicService;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final AuthorizationService authorizationService;

    public TopicController(TopicService topicService, AuthorizationService authorizationService) {
        this.topicService = topicService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/sensors")
    public TopicSensorsResponse getSensorTypesByTopic(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.requireAdminOrOperator(token);
        return topicService.getSensorTypesByTopic();
    }
}
