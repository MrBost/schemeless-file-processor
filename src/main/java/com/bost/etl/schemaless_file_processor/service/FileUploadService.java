package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.FileUploadResponse;
import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.exception.FileProcessingException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.repository.FileUploadRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;
    private final UploadTemplateRepository templateRepository;

    @Value("${app.file.storage.location:./uploads}")
    private String storageLocation;

    @Value("${app.file.allowed-types:csv,xlsx,xls}")
    private String allowedTypes;

    @Value("${app.file.storage.max-size-mb:50}")
    private long maxFileSizeMB;

    public FileUploadResponse uploadFile(MultipartFile file, UUID templateId, String uploadedBy) {
        validateFile(file);
        
        UploadTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + templateId));

        String fileName = storeFile(file);
        String fileType = getFileExtension(file.getOriginalFilename());

        FileUpload fileUpload = FileUpload.builder()
                .template(template)
                .fileName(fileName)
                .fileType(fileType)
                .uploadedBy(uploadedBy)
                .uploadStatus("PENDING")
                .totalRecords(0)
                .successfulRecords(0)
                .failedRecords(0)
                .build();

        fileUpload = fileUploadRepository.save(fileUpload);

        log.info("File uploaded successfully: {} by user: {}", fileName, uploadedBy);

        return mapToResponse(fileUpload);
    }

    public FileUploadResponse getUploadById(UUID uploadId) {
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("File upload not found with id: " + uploadId));
        return mapToResponse(fileUpload);
    }

    public List<FileUploadResponse> getUploadsByTemplate(UUID templateId) {
        return fileUploadRepository.findByTemplateId(templateId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<FileUploadResponse> getUploadsByUser(String uploadedBy) {
        return fileUploadRepository.findByUploadedBy(uploadedBy).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<FileUploadResponse> getUploadsByStatus(String status) {
        return fileUploadRepository.findByUploadStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String fileExtension = getFileExtension(originalFilename);
        List<String> allowedTypeList = Arrays.asList(allowedTypes.split(","));
        
        if (!allowedTypeList.contains(fileExtension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedTypes);
        }

        long fileSizeMB = file.getSize() / (1024 * 1024);
        if (fileSizeMB > maxFileSizeMB) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSizeMB + "MB");
        }
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(storageLocation);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFileName = UUID.randomUUID() + "." + fileExtension;

            Path targetLocation = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation);

            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file", ex);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private FileUploadResponse mapToResponse(FileUpload fileUpload) {
        return FileUploadResponse.builder()
                .uploadId(fileUpload.getId())
                .status(fileUpload.getUploadStatus())
                .fileName(fileUpload.getFileName())
                .fileType(fileUpload.getFileType())
                .totalRecords(fileUpload.getTotalRecords())
                .successfulRecords(fileUpload.getSuccessfulRecords())
                .failedRecords(fileUpload.getFailedRecords())
                .createdAt(fileUpload.getCreatedAt())
                .build();
    }
}
