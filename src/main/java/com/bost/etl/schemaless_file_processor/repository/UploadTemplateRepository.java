package com.bost.etl.schemaless_file_processor.repository;

import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadTemplateRepository extends JpaRepository<UploadTemplate, UUID> {

    Optional<UploadTemplate> findByTemplateName(String templateName);

    List<UploadTemplate> findByCreatedBy(String createdBy);

    @Query("SELECT t FROM UploadTemplate t LEFT JOIN FETCH t.fields WHERE t.id = :id")
    Optional<UploadTemplate> findByIdWithFields(@Param("id") UUID id);

    @Query("SELECT t FROM UploadTemplate t LEFT JOIN FETCH t.fields WHERE t.id = :id AND t.createdBy = :createdBy")
    Optional<UploadTemplate> findByIdWithFieldsAndCreator(@Param("id") UUID id, @Param("createdBy") String createdBy);

    boolean existsByTemplateName(String templateName);
}
