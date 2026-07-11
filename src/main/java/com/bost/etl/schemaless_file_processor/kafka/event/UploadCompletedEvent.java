package com.bost.etl.schemaless_file_processor.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadCompletedEvent {

    private UUID uploadId;
    private UUID templateId;
    private String fileName;
    private String uploadStatus;
    private Integer totalRecords;
    private Integer successfulRecords;
    private Integer failedRecords;
    private String uploadedBy;
    private LocalDateTime completedAt;
}
