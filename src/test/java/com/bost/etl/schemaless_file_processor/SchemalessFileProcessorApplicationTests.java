package com.bost.etl.schemaless_file_processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * Smoke test: verifies the Spring application context loads without requiring
 * any live infrastructure (PostgreSQL, Redis, or an external Kafka broker).
 *
 * Infrastructure is handled as follows:
 *   - Kafka  → @EmbeddedKafka spins up an in-process broker.
 *   - JPA / PostgreSQL → DataSource, JPA, and Flyway auto-configs are excluded
 *                        in src/test/resources/application.properties; the five
 *                        JPA repositories are replaced with Mockito mocks so the
 *                        service layer still wires up cleanly.
 *   - Redis / Cache    → auto-configs excluded; no CacheManager bean is created.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"upload-completed"},
        brokerPropertiesLocation = ""   // use defaults
)
class SchemalessFileProcessorApplicationTests {

	@Test
	void contextLoads() {
	}

}
