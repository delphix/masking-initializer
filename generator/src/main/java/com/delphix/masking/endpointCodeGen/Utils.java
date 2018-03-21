package com.delphix.masking.endpointCodeGen;

import com.google.gson.Gson;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;

public class Utils {

    public static <T> T getClassFromFile(String filePath, Class<T> classOfT) throws IOException {
        if (FilenameUtils.getExtension(filePath).equalsIgnoreCase("yaml") || FilenameUtils.getExtension(filePath)
                .equalsIgnoreCase("yml")) {
            return getClassFromYamlFile(filePath, classOfT);
        } else if (FilenameUtils.getExtension(filePath).equalsIgnoreCase("json")) {
            return getClassFromJsonFile(filePath, classOfT);
        }
        throw new RuntimeException("File: [" + filePath + "] must end in .yaml/.yml or .json");
    }

    public static <T> T getClassFromJsonFile(String jsonFilePath, Class<T> classOfT) throws IOException {
        Gson gson = new Gson();
        String jsonString = readFile(jsonFilePath);
        return gson.fromJson(jsonString, classOfT);
    }

    public static <T> String getJSONFromClass(T t) {
        Gson gson = new Gson();
        return gson.toJson(t);
    }

    public static <T> String getYamlFromClass(T t) {
        Yaml yaml = new Yaml();
        return yaml.dump(t);
    }

    public static <T> T getClassFromJson(String json, Class<T> classOfT) {
        Gson gson = new Gson();
        return gson.fromJson(json, classOfT);
    }

    public static <T> T getClassFromYamlFile(String yamlFilePath, Class<T> classOfT) throws IOException {
        Yaml yaml = new Yaml();
        String yamlFile = readFile(yamlFilePath);
        return yaml.loadAs(yamlFile, classOfT);
    }

    public static <T> T getClassFromYaml(String yamlString, Class<T> classOfT) throws IOException {
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlString, classOfT);
    }

    public static String readFile(String filePath) throws IOException {
        return FileUtils.readFileToString(new File(filePath), Charsets.UTF_8);
    }

    public static String capFirstLetter(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

}

