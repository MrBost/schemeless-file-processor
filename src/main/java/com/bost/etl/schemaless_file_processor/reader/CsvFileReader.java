package com.bost.etl.schemaless_file_processor.reader;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CsvFileReader implements FileDataReader {

    @Override
    public Map<String, String> readHeaders(File file) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                throw new IllegalArgumentException("CSV file is empty or has no headers");
            }

            Map<String, String> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), headers[i].trim());
            }

            log.debug("Read {} headers from CSV file: {}", headers.length, file.getName());
            return headerMap;
        }
    }

    @Override
    public List<Map<String, String>> readRows(File file) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                throw new IllegalArgumentException("CSV file is empty or has no headers");
            }

            String[] row;
            int rowNum = 0;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                
                // Skip empty rows
                if (row.length == 0 || (row.length == 1 && row[0].isEmpty())) {
                    continue;
                }

                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    String value = i < row.length ? row[i] : "";
                    rowData.put(header, value != null ? value.trim() : "");
                }

                rows.add(rowData);
            }

            log.debug("Read {} data rows from CSV file: {}", rows.size(), file.getName());
            return rows;
        }
    }
}
