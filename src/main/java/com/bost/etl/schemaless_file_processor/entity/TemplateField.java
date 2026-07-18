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
@Table(name = "template_field")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private UploadTemplate template;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_type", nullable = false)
    private String fieldType;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = false;

    @Column(name = "validation_rule", columnDefinition = "TEXT")
    private String validationRule;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
