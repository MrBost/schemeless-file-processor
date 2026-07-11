package com.bost.etl.schemaless_file_processor.dto;

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
public class FileUploadResponse {

    private UUID uploadId;
    private String status;
    private String fileName;
    private String fileType;
    private Integer totalRecords;
    private Integer successfulRecords;
    private Integer failedRecords;
    private LocalDateTime createdAt;
}
