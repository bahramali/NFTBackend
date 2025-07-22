package se.hydroleaf.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.RecordService;

@Component
public class KafkaListeners {

    private static final Logger logger = LoggerFactory.getLogger(KafkaListeners.class);
    private final RecordService recordService;

    public KafkaListeners(RecordService recordService) {
        this.recordService = recordService;
    }

    @KafkaListener(topics = "growSensors", groupId = "nft-backend")
    public void listenGrowSensors(String message) {
        logger.info("Received growSensors message: {}", message);
        recordService.saveMessage(message);
    }

    @KafkaListener(topics = "rootImages", groupId = "nft-backend")
    public void listenRootImages(String message) {
        logger.info("Received rootImages message: {}", message);
        recordService.saveMessage(message);
    }

    @KafkaListener(topics = "waterOutput", groupId = "nft-backend")
    public void listenWaterOutput(String message) {
        logger.info("Received waterOutput message: {}", message);
        recordService.saveMessage(message);
    }

    @KafkaListener(topics = "waterTank", groupId = "nft-backend")
    public void listenWaterTank(String message) {
        logger.info("Received waterTank message: {}", message);
        recordService.saveMessage(message);
    }
}
