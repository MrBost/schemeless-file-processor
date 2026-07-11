package com.bost.etl.schemaless_file_processor.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FileReaderFactory {

    private final CsvFileReader csvFileReader;
    private final ExcelFileReader excelFileReader;

    public FileReaderFactory(CsvFileReader csvFileReader, ExcelFileReader excelFileReader) {
        this.csvFileReader = csvFileReader;
        this.excelFileReader = excelFileReader;
    }

    public FileDataReader getReader(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "csv" -> csvFileReader;
            case "xlsx", "xls" -> excelFileReader;
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
        };
    }

    public Map<String, String> readHeaders(File file, String fileType) throws Exception {
        FileDataReader reader = getReader(fileType);
        return reader.readHeaders(file);
    }

    public List<Map<String, String>> readRows(File file, String fileType) throws Exception {
        FileDataReader reader = getReader(fileType);
        return reader.readRows(file);
    }
}
