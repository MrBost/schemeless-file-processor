package com.bost.etl.schemaless_file_processor.kafka.producer;

import com.bost.etl.schemaless_file_processor.kafka.event.UploadCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.upload-completed:upload-completed}")
    private String uploadCompletedTopic;

    public void publishUploadCompletedEvent(UploadCompletedEvent event) {
        try {
            kafkaTemplate.send(uploadCompletedTopic, event.getUploadId().toString(), event);
            log.info("Published upload completed event for upload ID: {} to topic: {}", 
                    event.getUploadId(), uploadCompletedTopic);
        } catch (Exception e) {
            log.error("Failed to publish upload completed event for upload ID: {}", event.getUploadId(), e);
            throw new RuntimeException("Failed to publish Kafka event", e);
        }
    }
}
