package com.bost.etl.schemaless_file_processor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FileProcessingMetrics {

    private final MeterRegistry meterRegistry;

    public void incrementFileUploads(String fileType) {
        Counter.builder("file.uploads.total")
                .description("Total number of file uploads")
                .tag("file_type", fileType)
                .register(meterRegistry)
                .increment();
    }

    public void incrementFileProcessingSuccess(String fileType) {
        Counter.builder("file.processing.success")
                .description("Total number of successfully processed files")
                .tag("file_type", fileType)
                .register(meterRegistry)
                .increment();
    }

    public void incrementFileProcessingFailure(String fileType) {
        Counter.builder("file.processing.failure")
                .description("Total number of failed file processing")
                .tag("file_type", fileType)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRecordsProcessed(String templateId) {
        Counter.builder("records.processed.total")
                .description("Total number of records processed")
                .tag("template_id", templateId)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRecordsValid(String templateId) {
        Counter.builder("records.valid.total")
                .description("Total number of valid records")
                .tag("template_id", templateId)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRecordsInvalid(String templateId) {
        Counter.builder("records.invalid.total")
                .description("Total number of invalid records")
                .tag("template_id", templateId)
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessingTime(String fileType, long durationMs) {
        Timer.builder("file.processing.duration")
                .description("File processing duration in milliseconds")
                .tag("file_type", fileType)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordValidationTime(String templateId, long durationMs) {
        Timer.builder("record.validation.duration")
                .description("Record validation duration in milliseconds")
                .tag("template_id", templateId)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordMappingTime(String templateId, long durationMs) {
        Timer.builder("record.mapping.duration")
                .description("Record mapping duration in milliseconds")
                .tag("template_id", templateId)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordKafkaPublishTime(String topic, long durationMs) {
        Timer.builder("kafka.publish.duration")
                .description("Kafka event publish duration in milliseconds")
                .tag("topic", topic)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
