package com.bost.etl.schemaless_file_processor.repository;

import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateFieldRepository extends JpaRepository<TemplateField, UUID> {

    List<TemplateField> findByTemplateId(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
