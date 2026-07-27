package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.event.UploadAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
class UploadProcessingDispatcher {
    private final ProcessingService processingService;

    @Async("fileProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(UploadAcceptedEvent event) {
        try {
            processingService.processFileUpload(event.uploadId());
        } catch (Exception exception) {
            // ProcessingService records FAILED itself; this protects the executor thread.
            log.error("Asynchronous processing failed for upload {}", event.uploadId(), exception);
        }
    }
}
