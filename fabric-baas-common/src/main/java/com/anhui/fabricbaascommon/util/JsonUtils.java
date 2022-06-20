package com.anhui.fabricbaascommon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.json.JacksonJsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static final JacksonJsonParser JACKSON_JSON_PARSER = new JacksonJsonParser();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Map<String, Object> loadAsMap(String json) throws IOException {
        return JACKSON_JSON_PARSER.parseMap(json);
    }

    public static Map<String, Object> loadAsMap(File file) throws IOException {
        String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return loadAsMap(str);
    }

    public static List<Object> loadAsList(String json) throws IOException {
        return JACKSON_JSON_PARSER.parseList(json);
    }

    public static List<Object> loadAsList(File file) throws IOException {
        String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return loadAsList(str);
    }

    public static void save(File dstFile, Object obj) throws IOException {
        String str = OBJECT_MAPPER.writeValueAsString(obj);
        FileUtils.write(dstFile, str, StandardCharsets.UTF_8);
    }
}
