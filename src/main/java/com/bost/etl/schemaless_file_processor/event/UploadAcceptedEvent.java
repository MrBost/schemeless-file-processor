package com.bost.etl.schemaless_file_processor.event;

import java.util.UUID;

/** Published only after an upload has been committed and is safe to process. */
public record UploadAcceptedEvent(UUID uploadId) { }
