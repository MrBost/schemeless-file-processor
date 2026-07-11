package com.bost.etl.schemaless_file_processor.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ExcelFileReader implements FileDataReader {

    @Override
    public Map<String, String> readHeaders(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = getWorkbook(file, fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is empty or has no headers");
            }

            Map<String, String> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && !headerValue.isEmpty()) {
                    headerMap.put(headerValue.trim(), headerValue.trim());
                }
            }

            log.debug("Read {} headers from Excel file: {}", headerMap.size(), file.getName());
            return headerMap;
        }
    }

    @Override
    public List<Map<String, String>> readRows(File file) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = getWorkbook(file, fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is empty or has no headers");
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                String headerValue = getCellValueAsString(cell);
                headers.add(headerValue != null ? headerValue.trim() : "");
            }

            int totalRows = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    Cell cell = row.getCell(j);
                    String value = getCellValueAsString(cell);
                    rowData.put(header, value != null ? value.trim() : "");
                }

                rows.add(rowData);
            }

            log.debug("Read {} data rows from Excel file: {}", rows.size(), file.getName());
            return rows;
        }
    }

    private Workbook getWorkbook(File file, FileInputStream fis) throws Exception {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("Unsupported Excel file format: " + fileName);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }
}
