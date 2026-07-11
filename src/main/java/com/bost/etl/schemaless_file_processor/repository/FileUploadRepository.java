package com.bost.etl.schemaless_file_processor.repository;

import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {

    List<FileUpload> findByTemplateId(UUID templateId);

    List<FileUpload> findByUploadedBy(String uploadedBy);

    List<FileUpload> findByUploadStatus(String uploadStatus);

    Optional<FileUpload> findByIdAndTemplateId(UUID id, UUID templateId);

    @Query("SELECT f FROM FileUpload f WHERE f.uploadStatus = :status ORDER BY f.createdAt DESC")
    List<FileUpload> findByUploadStatusOrderByCreatedAtDesc(@Param("status") String status);
}
