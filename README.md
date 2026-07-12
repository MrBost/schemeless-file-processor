# ETL Schemaless File Processor

A production-ready, schema-driven file ingestion service built with Spring Boot, PostgreSQL JSONB, and Kafka for processing CSV and Excel files with user-defined field templates.

## Features

- **Schema Management**: Define custom data templates with field types and validation rules
- **File Processing**: Support for CSV and Excel (XLSX, XLS) file formats
- **Dynamic Mapping**: Automatic type conversion and field mapping based on templates
- **Validation Engine**: Comprehensive validation with custom rules (regex, min/max, length, email, URL)
- **JSONB Storage**: PostgreSQL JSONB for flexible schemaless data storage
- **Event-Driven**: Kafka integration for async event publishing
- **Security**: JWT-based authentication and authorization
- **Observability**: Micrometer metrics for monitoring and Prometheus integration
- **API Documentation**: OpenAPI/Swagger documentation

## Technology Stack

- **Backend**: Spring Boot 4.1.0, Java 21
- **Database**: PostgreSQL 16 with JSONB
- **Messaging**: Apache Kafka 7.7.0
- **Caching**: Redis 7
- **File Processing**: Apache POI 5.5.1, OpenCSV 5.12.0
- **Validation**: Jakarta Validation
- **Security**: Spring Security, JWT (jjwt 0.12.3)
- **Documentation**: SpringDoc OpenAPI 2.7.0
- **Testing**: Testcontainers 1.20.4, JUnit 5, Mockito
- **Build**: Maven

## Prerequisites

- Java 21
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 16
- Kafka 7.7.0
- Redis 7

## Quick Start

### 1. Clone the repository

```bash
git clone <repository-url>
cd schemaless-file-processor
```

### 2. Start infrastructure services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Kafka on port 9092
- Zookeeper on port 2181
- Redis on port 6379

### 3. Configure application properties

Edit `src/main/resources/application.properties` to configure:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/schemaless_processor
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT
app.jwt.secret=your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
app.jwt.expiration=86400000
```

### 4. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api/v1`

## API Documentation

Access the Swagger UI at: `http://localhost:8080/api/v1/swagger-ui.html`

## API Endpoints

### Template Management

- `POST /api/v1/templates` - Create a new template
- `GET /api/v1/templates/{id}` - Get template by ID
- `GET /api/v1/templates` - Get all templates
- `GET /api/v1/templates/creator/{userId}` - Get templates by creator
- `PUT /api/v1/templates/{id}` - Update template
- `DELETE /api/v1/templates/{id}` - Delete template

### File Upload

- `POST /api/v1/uploads` - Upload a file
- `GET /api/v1/uploads/{uploadId}` - Get upload by ID
- `GET /api/v1/uploads/template/{templateId}` - Get uploads by template
- `GET /api/v1/uploads/user/{userId}` - Get uploads by user
- `GET /api/v1/uploads/status/{status}` - Get uploads by status

### Health & Metrics

- `GET /api/v1/actuator/health` - Health check
- `GET /api/v1/actuator/metrics` - Application metrics
- `GET /api/v1/actuator/prometheus` - Prometheus metrics

## Usage Example

### 1. Create a Template

```bash
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "templateName": "customer_template",
    "description": "Customer data template",
    "fields": [
      {
        "fieldName": "name",
        "fieldType": "string",
        "required": true,
        "validationRule": "minLength:2"
      },
      {
        "fieldName": "email",
        "fieldType": "string",
        "required": true,
        "validationRule": "email"
      },
      {
        "fieldName": "age",
        "fieldType": "integer",
        "required": true,
        "validationRule": "min:0,max:120"
      }
    ]
  }'
```

### 2. Upload a File

```bash
curl -X POST http://localhost:8080/api/v1/uploads \
  -H "Authorization: Bearer <jwt-token>" \
  -F "file=@customers.csv" \
  -F "templateId=<template-uuid>"
```

### 3. Process File

The file will be automatically processed with status updates:
- PENDING → PROCESSING → COMPLETED/PARTIALLY_COMPLETED/FAILED

## Validation Rules

Supported validation rules:
- `regex:pattern` - Regular expression matching
- `min:value` - Minimum numeric value
- `max:value` - Maximum numeric value
- `minLength:length` - Minimum string length
- `maxLength:length` - Maximum string length
- `email` - Email format validation
- `url` - URL format validation

## Supported Field Types

- `string` - Text values
- `integer` - Integer numbers
- `long` - Long integers
- `double` - Decimal numbers
- `boolean` - True/false values
- `date` - Date values (YYYY-MM-DD)
- `datetime` - DateTime values (ISO format)
- `json` - JSON objects

## Monitoring

The application exposes metrics via Micrometer:

- `file.uploads.total` - Total file uploads by type
- `file.processing.success` - Successful file processing
- `file.processing.failure` - Failed file processing
- `records.processed.total` - Total records processed
- `records.valid.total` - Valid records
- `records.invalid.total` - Invalid records
- `file.processing.duration` - Processing duration
- `record.validation.duration` - Validation duration
- `record.mapping.duration` - Mapping duration
- `kafka.publish.duration` - Kafka publish duration

Access metrics at: `http://localhost:8080/api/v1/actuator/metrics`

## Testing

Run unit tests:
```bash
mvn test
```

Run integration tests with Testcontainers:
```bash
mvn verify
```

## Project Structure

```
src/main/java/com/bost/etl/schemaless_file_processor/
├── config/              # Configuration classes
├── controller/          # REST controllers
├── dto/                 # Data transfer objects
├── entity/              # JPA entities
├── exception/           # Custom exceptions
├── kafka/               # Kafka integration
│   ├── event/          # Event classes
│   └── producer/       # Kafka producers
├── mapper/             # Field mapping logic
├── metrics/            # Metrics collection
├── reader/             # File readers
├── repository/          # JPA repositories
├── security/           # Security configuration
├── service/            # Business logic
└── validator/          # Validation logic
```

## Configuration

### File Upload Settings

```properties
app.file.storage.location=./uploads
app.file.storage.max-size-mb=50
app.file.allowed-types=csv,xlsx,xls
```

### Kafka Settings

```properties
spring.kafka.bootstrap-servers=localhost:9092
app.kafka.topic.upload-completed=upload-completed
```

## License

[Specify your license here]

## Contributing

[Specify contribution guidelines here]
