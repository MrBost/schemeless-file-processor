package com.bost.etl.schemaless_file_processor.reader;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvFileReader implements FileDataReader {
    @Override public Map<String, String> readHeaders(File file) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) throw new IllegalArgumentException("CSV file is empty or has no headers");
            Map<String, String> result = new HashMap<>();
            for (String header : headers) { String value = header.trim(); if (result.putIfAbsent(value, value) != null) throw new IllegalArgumentException("CSV contains duplicate header: " + value); }
            return result;
        }
    }
    @Override public List<Map<String, String>> readRows(File file) throws Exception { List<Map<String,String>> rows=new ArrayList<>(); forEachRow(file, rows::add); return rows; }
    @Override public void forEachRow(File file, RowConsumer consumer) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) throw new IllegalArgumentException("CSV file is empty or has no headers");
            for (String[] row; (row = reader.readNext()) != null;) {
                if (row.length == 0 || (row.length == 1 && row[0].isEmpty())) continue;
                Map<String,String> data = new HashMap<>();
                for (int index=0; index<headers.length; index++) data.put(headers[index].trim(), index < row.length && row[index] != null ? row[index].trim() : "");
                consumer.accept(data);
            }
        }
    }
}
