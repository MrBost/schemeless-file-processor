package com.bost.etl.schemaless_file_processor.repository;

import com.bost.etl.schemaless_file_processor.entity.UploadError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadErrorRepository extends JpaRepository<UploadError, UUID> {

    List<UploadError> findByUploadId(UUID uploadId);

    void deleteByUploadId(UUID uploadId);
}
