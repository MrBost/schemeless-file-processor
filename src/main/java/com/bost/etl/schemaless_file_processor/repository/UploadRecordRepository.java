package com.bost.etl.schemaless_file_processor.repository;

import com.bost.etl.schemaless_file_processor.entity.UploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadRecordRepository extends JpaRepository<UploadRecord, UUID> {

    List<UploadRecord> findByUploadId(UUID uploadId);

    List<UploadRecord> findByValidationStatus(String validationStatus);

    List<UploadRecord> findByProcessingStatus(String processingStatus);

    @Query("SELECT r FROM UploadRecord r WHERE r.upload.id = :uploadId AND r.validationStatus = :status")
    List<UploadRecord> findByUploadIdAndValidationStatus(@Param("uploadId") UUID uploadId, @Param("status") String status);

    @Query("SELECT COUNT(r) FROM UploadRecord r WHERE r.upload.id = :uploadId")
    long countByUploadId(@Param("uploadId") UUID uploadId);

    @Query("SELECT COUNT(r) FROM UploadRecord r WHERE r.upload.id = :uploadId AND r.validationStatus = 'VALID'")
    long countValidByUploadId(@Param("uploadId") UUID uploadId);

    @Query("SELECT COUNT(r) FROM UploadRecord r WHERE r.upload.id = :uploadId AND r.validationStatus = 'INVALID'")
    long countInvalidByUploadId(@Param("uploadId") UUID uploadId);
}
