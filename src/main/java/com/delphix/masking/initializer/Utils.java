package com.delphix.masking.initializer;

import com.delphix.masking.initializer.exception.NoRegexMatchException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {

    private static long count = 0;

    public static synchronized String getFileName(String objectName) {
        return objectName + count++;
    }

    public static <T> void writeClassToFile(Path file, T t) {
        try (FileWriter fileWriter = new FileWriter(file.toFile())) {
            fileWriter.write(getJSONFromClass(t));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T getClassFromFile(Path filePath, Class<T> classOfT) throws IOException {
        if (FilenameUtils.getExtension(filePath.toString()).equalsIgnoreCase("yaml") || FilenameUtils.getExtension(filePath.toString()).equalsIgnoreCase("yml") ) {
            return getClassFromYamlFile(filePath, classOfT);
        } else if (FilenameUtils.getExtension(filePath.toString()).equalsIgnoreCase("json")) {
            return getClassFromJsonFile(filePath, classOfT);
        }
        throw new RuntimeException("File: [" + filePath + "] must end in .yaml/.yml or .json");
    }

    public static <T> T getClassFromJsonFile(Path jsonFilePath, Class<T> classOfT) throws IOException {
        Gson gson = new Gson();
        Reader reader = new FileReader(jsonFilePath.toFile());
        return gson.fromJson(reader, classOfT);
    }

    public static <T> String getJSONFromClass(T t) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(t);
    }

    public static <T> T getClassFromJson(String json, Class<T> classOfT) {
        Gson gson = new Gson();
        return gson.fromJson(json, classOfT);
    }

    public static <T> T getClassFromYamlFile(Path yamlFilePath, Class<T> classOfT) throws IOException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(yamlFilePath.toFile());
        return yaml.loadAs(inputStream, classOfT);
    }
}
