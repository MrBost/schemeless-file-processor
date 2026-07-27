package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.FileUploadResponse;
import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.event.UploadAcceptedEvent;
import com.bost.etl.schemaless_file_processor.exception.AccessDeniedException;
import com.bost.etl.schemaless_file_processor.exception.FileProcessingException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.reader.FileReaderFactory;
import com.bost.etl.schemaless_file_processor.repository.FileUploadRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.bost.etl.schemaless_file_processor.security.UserContext.getCurrentUsername;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {
    private final FileUploadRepository fileUploadRepository;
    private final UploadTemplateRepository templateRepository;
    private final FileReaderFactory fileReaderFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.file.storage.location:./uploads}") private String storageLocation;
    @Value("${app.file.allowed-types:csv,xlsx,xls}") private String allowedTypes;
    @Value("${app.file.storage.max-size-mb:50}") private long maxFileSizeMB;

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, UUID templateId, String uploadedBy) {
        validateFile(file);
        UploadTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));
        if (!template.getCreatedBy().equals(getCurrentUsername())) {
            throw new AccessDeniedException("You can only upload files using your own templates");
        }

        String fileType = getFileExtension(file.getOriginalFilename());
        Path stagedFile = stageUpload(file, fileType);
        String storedName = UUID.randomUUID() + "." + fileType;
        Path target = uploadDirectory().resolve(storedName);
        try {
            validateHeaders(stagedFile.toFile(), fileType, template);
            moveAtomically(stagedFile, target);
        } catch (Exception exception) {
            deleteQuietly(stagedFile);
            throw exception instanceof FileProcessingException processingException
                    ? processingException : new FileProcessingException("Failed to prepare uploaded file", exception);
        }
        // The database cannot atomically roll back a filesystem move; clean up the file on DB rollback.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteQuietly(target);
            }
        });

        FileUpload upload = fileUploadRepository.save(FileUpload.builder().template(template).fileName(storedName)
                .fileType(fileType).uploadedBy(uploadedBy).uploadStatus("PENDING").totalRecords(0)
                .successfulRecords(0).failedRecords(0).build());
        eventPublisher.publishEvent(new UploadAcceptedEvent(upload.getId()));
        log.info("Upload {} accepted for asynchronous processing by {}", upload.getId(), uploadedBy);
        return mapToResponse(upload);
    }

    private Path stageUpload(MultipartFile file, String fileType) {
        try {
            Path directory = uploadDirectory();
            Path staged = Files.createTempFile(directory, "upload-", "." + fileType);
            try (InputStream source = file.getInputStream()) { Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING); }
            validateFileSignature(staged, fileType);
            return staged;
        } catch (IOException exception) { throw new FileProcessingException("Failed to stage uploaded file", exception); }
    }

    private Path uploadDirectory() { try { Path path = Paths.get(storageLocation).toAbsolutePath().normalize(); Files.createDirectories(path); return path; } catch (IOException exception) { throw new FileProcessingException("Failed to create upload directory", exception); } }
    private void moveAtomically(Path source, Path target) throws IOException { try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); } catch (IOException ignored) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); } }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException exception) { log.warn("Could not remove staged file {}", path, exception); } }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileProcessingException("File is empty");
        String name = file.getOriginalFilename(); if (name == null || name.isBlank()) throw new FileProcessingException("Invalid file name");
        String extension = getFileExtension(name).toLowerCase();
        if (!Arrays.asList(allowedTypes.split(",")).contains(extension)) throw new FileProcessingException("File type not allowed. Allowed types: " + allowedTypes);
        if (file.getSize() > maxFileSizeMB * 1024L * 1024L) throw new FileProcessingException("File size exceeds maximum allowed size of " + maxFileSizeMB + "MB");
    }

    private void validateFileSignature(Path file, String type) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] header = input.readNBytes(8);
            boolean zip = header.length >= 4 && header[0] == 'P' && header[1] == 'K' && header[2] == 3 && header[3] == 4;
            boolean ole = header.length == 8 && (header[0] & 0xff) == 0xD0 && (header[1] & 0xff) == 0xCF && (header[2] & 0xff) == 0x11 && (header[3] & 0xff) == 0xE0;
            if (("xlsx".equals(type) && !zip) || ("xls".equals(type) && !ole)) throw new FileProcessingException("File contents do not match the selected extension");
        }
    }

    private void validateHeaders(File file, String type, UploadTemplate template) {
        try {
            Map<String, String> headers = fileReaderFactory.readHeaders(file, type);
            List<String> actual = headers.values().stream().map(value -> value.trim()).toList();
            List<String> missing = template.getFields().stream().filter(TemplateField::getRequired).map(TemplateField::getFieldName)
                    .filter(expected -> !actual.contains(expected)).toList();
            if (!missing.isEmpty()) throw new FileProcessingException("File headers do not match template '" + template.getTemplateName() + "'. Missing required fields: " + String.join(", ", missing));
        } catch (FileProcessingException exception) { throw exception; }
        catch (Exception exception) { throw new FileProcessingException("Failed to validate file headers: " + exception.getMessage(), exception); }
    }

    private String getFileExtension(String filename) { int index = filename == null ? -1 : filename.lastIndexOf('.'); return index < 0 ? "" : filename.substring(index + 1).toLowerCase(); }
    private FileUploadResponse mapToResponse(FileUpload upload) { return FileUploadResponse.builder().uploadId(upload.getId()).status(upload.getUploadStatus()).fileName(upload.getFileName()).fileType(upload.getFileType()).totalRecords(upload.getTotalRecords()).successfulRecords(upload.getSuccessfulRecords()).failedRecords(upload.getFailedRecords()).createdAt(upload.getCreatedAt()).build(); }

    // Query methods retain their public contract and authorization behaviour.
    public FileUploadResponse getUploadById(UUID id) { FileUpload upload=fileUploadRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("FileUpload",id)); if(!upload.getUploadedBy().equals(getCurrentUsername())) throw new AccessDeniedException("You can only view your own uploads"); return mapToResponse(upload); }
    public List<FileUploadResponse> getUploadsByTemplate(UUID templateId) { UploadTemplate template=templateRepository.findById(templateId).orElseThrow(()->new ResourceNotFoundException("Template",templateId)); if(!template.getCreatedBy().equals(getCurrentUsername())) throw new AccessDeniedException("You can only view uploads for your own templates"); return fileUploadRepository.findByTemplateId(templateId).stream().map(this::mapToResponse).toList(); }
    public List<FileUploadResponse> getUploadsByUser(String user) { if(!getCurrentUsername().equals(user)) throw new AccessDeniedException("You can only view your own uploads"); return fileUploadRepository.findByUploadedBy(user).stream().map(this::mapToResponse).toList(); }
    public List<FileUploadResponse> getUploadsByStatus(String status) { String user=getCurrentUsername(); return fileUploadRepository.findByUploadStatus(status).stream().filter(upload->upload.getUploadedBy().equals(user)).map(this::mapToResponse).toList(); }
}
