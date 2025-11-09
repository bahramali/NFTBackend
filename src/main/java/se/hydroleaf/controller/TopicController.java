package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.repository.dto.TopicSensorsResponse;
import se.hydroleaf.service.TopicService;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/sensors")
    public TopicSensorsResponse getSensorTypesByTopic() {
        return topicService.getSensorTypesByTopic();
    }
}
