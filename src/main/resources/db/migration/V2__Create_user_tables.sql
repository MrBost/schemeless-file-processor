-- Create app_user table
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_role table for many-to-many relationship
CREATE TABLE user_role (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY(user_id) 
        REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role)
);

-- Create indexes for common query patterns
CREATE INDEX idx_app_user_username ON app_user(username);
CREATE INDEX idx_app_user_email ON app_user(email);
CREATE INDEX idx_app_user_enabled ON app_user(enabled);
