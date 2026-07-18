package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.FileUploadResponse;
import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.exception.AccessDeniedException;
import com.bost.etl.schemaless_file_processor.exception.FileProcessingException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.repository.FileUploadRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import static com.bost.etl.schemaless_file_processor.security.UserContext.getCurrentUsername;

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
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));

        // Verify user owns the template
        String currentUser = getCurrentUsername();
        if (!template.getCreatedBy().equals(currentUser)) {
            throw new AccessDeniedException("You can only upload files using your own templates");
        }

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
        String currentUser = getCurrentUsername();
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", uploadId));
        
        // Verify user owns the upload
        if (!fileUpload.getUploadedBy().equals(currentUser)) {
            throw new AccessDeniedException("You can only view your own uploads");
        }
        return mapToResponse(fileUpload);
    }

    public List<FileUploadResponse> getUploadsByTemplate(UUID templateId) {
        String currentUser = getCurrentUsername();
        
        // Verify user owns the template
        UploadTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));
        
        if (!template.getCreatedBy().equals(currentUser)) {
            throw new AccessDeniedException("You can only view uploads for your own templates");
        }
        
        return fileUploadRepository.findByTemplateId(templateId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<FileUploadResponse> getUploadsByUser(String uploadedBy) {
        String currentUser = getCurrentUsername();
        
        // Users can only view their own uploads
        if (!currentUser.equals(uploadedBy)) {
            log.warn("User {} attempted to access uploads of user {}", currentUser, uploadedBy);
            throw new AccessDeniedException("You can only view your own uploads");
        }
        
        return fileUploadRepository.findByUploadedBy(uploadedBy).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<FileUploadResponse> getUploadsByStatus(String status) {
        String currentUser = getCurrentUsername();
        
        // Users can only view their own uploads by status
        return fileUploadRepository.findByUploadStatus(status).stream()
                .filter(upload -> upload.getUploadedBy().equals(currentUser))
                .map(this::mapToResponse)
                .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileProcessingException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileProcessingException("Invalid file name");
        }

        String fileExtension = getFileExtension(originalFilename);
        List<String> allowedTypeList = Arrays.asList(allowedTypes.split(","));
        
        if (!allowedTypeList.contains(fileExtension.toLowerCase())) {
            throw new FileProcessingException("File type not allowed. Allowed types: " + allowedTypes);
        }

        long fileSizeMB = file.getSize() / (1024 * 1024);
        if (fileSizeMB > maxFileSizeMB) {
            throw new FileProcessingException("File size exceeds maximum allowed size of " + maxFileSizeMB + "MB");
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
            throw new FileProcessingException("Failed to store file", ex);
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