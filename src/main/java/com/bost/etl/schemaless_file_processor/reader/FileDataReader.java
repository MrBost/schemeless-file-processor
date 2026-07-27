package com.bost.etl.schemaless_file_processor.reader;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FileDataReader {
    @FunctionalInterface
    interface RowConsumer { void accept(Map<String, String> row) throws Exception; }
    Map<String, String> readHeaders(File file) throws Exception;
    List<Map<String, String>> readRows(File file) throws Exception;

    default void forEachRow(File file, RowConsumer consumer) throws Exception {
        for (Map<String, String> row : readRows(file)) consumer.accept(row);
    }
}
