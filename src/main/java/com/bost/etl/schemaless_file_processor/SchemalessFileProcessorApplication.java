package com.bost.etl.schemaless_file_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SchemalessFileProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchemalessFileProcessorApplication.class, args);
	}

}
