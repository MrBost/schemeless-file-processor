-- Create upload_template table
CREATE TABLE upload_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create template_field table
CREATE TABLE template_field (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT FALSE,
    validation_rule TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_field_template FOREIGN KEY(template_id) 
        REFERENCES upload_template(id) ON DELETE CASCADE
);

-- Create file_upload table
CREATE TABLE file_upload (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    uploaded_by VARCHAR(100),
    upload_status VARCHAR(50) DEFAULT 'PENDING',
    total_records INTEGER DEFAULT 0,
    successful_records INTEGER DEFAULT 0,
    failed_records INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_upload_template FOREIGN KEY(template_id) 
        REFERENCES upload_template(id)
);

-- Create upload_record table with JSONB
CREATE TABLE upload_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id UUID NOT NULL,
    record_data JSONB NOT NULL,
    validation_status VARCHAR(50) DEFAULT 'PENDING',
    validation_errors JSONB,
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_upload_record_upload FOREIGN KEY(upload_id) 
        REFERENCES file_upload(id) ON DELETE CASCADE
);

-- Create upload_error table
CREATE TABLE upload_error (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id UUID NOT NULL,
    row_number INTEGER,
    error_message TEXT NOT NULL,
    row_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_upload_error_upload FOREIGN KEY(upload_id) 
        REFERENCES file_upload(id) ON DELETE CASCADE
);

-- Create GIN index for JSONB queries on upload_record
CREATE INDEX idx_upload_record_data ON upload_record USING GIN(record_data);

-- Create indexes for common query patterns
CREATE INDEX idx_file_upload_template ON file_upload(template_id);
CREATE INDEX idx_file_upload_status ON file_upload(upload_status);
CREATE INDEX idx_file_upload_created_at ON file_upload(created_at);
CREATE INDEX idx_upload_record_upload ON upload_record(upload_id);
CREATE INDEX idx_upload_record_validation_status ON upload_record(validation_status);
CREATE INDEX idx_upload_record_processing_status ON upload_record(processing_status);
CREATE INDEX idx_template_field_template ON template_field(template_id);
CREATE INDEX idx_upload_error_upload ON upload_error(upload_id);

-- Add unique constraint for field names within a template
CREATE UNIQUE INDEX idx_template_field_unique_name ON template_field(template_id, field_name);
