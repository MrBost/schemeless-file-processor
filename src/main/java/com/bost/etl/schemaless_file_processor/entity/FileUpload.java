package com.bost.etl.schemaless_file_processor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_upload")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private UploadTemplate template;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "upload_status")
    @Builder.Default
    private String uploadStatus = "PENDING";

    @Column(name = "total_records")
    @Builder.Default
    private Integer totalRecords = 0;

    @Column(name = "successful_records")
    @Builder.Default
    private Integer successfulRecords = 0;

    @Column(name = "failed_records")
    @Builder.Default
    private Integer failedRecords = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
