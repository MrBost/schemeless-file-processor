package com.bost.etl.schemaless_file_processor.reader;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FileDataReader {
    Map<String, String> readHeaders(File file) throws Exception;
    List<Map<String, String>> readRows(File file) throws Exception;
}
