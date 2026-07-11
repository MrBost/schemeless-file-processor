package com.bost.etl.schemaless_file_processor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private FileUpload upload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "record_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode recordData;

    @Column(name = "validation_status")
    private String validationStatus = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private JsonNode validationErrors;

    @Column(name = "processing_status")
    private String processingStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
