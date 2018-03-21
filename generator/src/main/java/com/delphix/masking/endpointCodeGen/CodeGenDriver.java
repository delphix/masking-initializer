package com.delphix.masking.endpointCodeGen;

import com.delphix.masking.endpointCodeGen.POJO.Endpoint;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CodeGenDriver {

    private static final String BUILD_DIR = "build/";
    private static final String SRC_DIR = "generator/src/";

    private static final String OUTPUT_DIR = BUILD_DIR + "src/main/java/com/delphix/masking/initializer/";
    private static final String POJO_DIR = OUTPUT_DIR + "pojo/";
    private static final String API_DIR = OUTPUT_DIR + "maskingApi/endpointCaller/";

    private static final String RESROUCES_DIR = BUILD_DIR + "resources/main/";
    private static final String ENDPOINT_DEFINITION_DIR = RESROUCES_DIR + "endpointDefinitions/";
    private static final String TEMPLATES_DIR = RESROUCES_DIR + "templates/";
    private static final String CREATE_TEMPLATE = TEMPLATES_DIR + "CreateEndpoint.vm";
    private static final String POJO_TEMPLATE = TEMPLATES_DIR + "POJO.vm";
    private static final String GET_TEMPLATE = TEMPLATES_DIR + "GetEndpoint.vm";
    private static final String LIST_TEMPLATE = TEMPLATES_DIR + "ListPOJO.vm";
    private static final String PUT_TEMPLATE = TEMPLATES_DIR + "UpdateEndpoint.vm";

    public CodeGenDriver() throws IOException {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        Template createTemplate = ve.getTemplate(CREATE_TEMPLATE);
        Template getTemplate = ve.getTemplate(GET_TEMPLATE);
        Template pojoTemplate = ve.getTemplate(POJO_TEMPLATE);
        Template pojoListTemplate = ve.getTemplate(LIST_TEMPLATE);
        Template updateTemplate = ve.getTemplate(PUT_TEMPLATE);

        initOutputFolder();
        List<File> files = new ArrayList<>(Arrays.asList(new File(ENDPOINT_DEFINITION_DIR).listFiles()));
        List<Endpoint> endpoints = new ArrayList<>();

        for (File file : files) {
            endpoints.add(Utils.getClassFromFile(file.getPath(), Endpoint.class));
        }

        for (Endpoint endpoint : endpoints) {
            VelocityContext context = new VelocityContext();

            String className = endpoint.getClassName().stream().collect(Collectors.joining());
            String classNameCapFirst = endpoint.getClassName()
                    .stream()
                    .map(string -> string.substring(0, 1).toUpperCase() + string.substring(1))
                    .collect(Collectors.joining());
            String classNameAllCaps = endpoint.getClassName()
                    .stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.joining("_"));

            context.put("ClassName", classNameCapFirst);
            context.put("className", className);
            context.put("CLASS_NAME", classNameAllCaps);
            context.put("memberVariables", endpoint.getMemberVariables());
            context.put("endpointPath", endpoint.getEndpointPath());
            context.put("nameField", endpoint.getNameField());
            context.put("idField", endpoint.getIdField());
            context.put("putAllowed", endpoint.getGeneratePut() != null ? endpoint.getGeneratePut() : true);
            context.put("queryParams", endpoint.getQueryParams());
            context.put("parentIdField", endpoint.getParentId());

            if (endpoint.getIsMultiPart() != null) {
                context.put("isMultiPart", endpoint.getIsMultiPart());
            }

            if (endpoint.getGeneratePojo() == null || endpoint.getGeneratePojo()) {
                createAndWrite(context, POJO_DIR + classNameCapFirst, pojoTemplate);
                createAndWrite(context, POJO_DIR + classNameCapFirst + "List", pojoListTemplate);
            }

            if (endpoint.getGenerateGet() == null || endpoint.getGenerateGet()) {
                createAndWrite(context, API_DIR + "Get" + classNameCapFirst + "s", getTemplate);
            }

            if (endpoint.getGeneratePost() == null || endpoint.getGeneratePost()) {
                createAndWrite(context, API_DIR + "Post" + classNameCapFirst, createTemplate);
            }

            if (endpoint.getGeneratePut() == null || endpoint.getGeneratePut()) {
                createAndWrite(context, API_DIR + "Put" + classNameCapFirst, updateTemplate);
            }

        }
    }

    private void createAndWrite(VelocityContext velocityContext, String path, Template template) throws IOException {
        StringWriter stringWriter = new StringWriter();
        template.merge(velocityContext, stringWriter);
        try (FileWriter fileWriter = new FileWriter(path + ".java")) {
            fileWriter.write(stringWriter.toString());
        }
    }

    private void initOutputFolder() throws IOException {

        File file = new File(POJO_DIR);
        if (file.exists()) {
            FileUtils.cleanDirectory(file);
        }
        file.mkdirs();
        file = new File(API_DIR);
        if (file.exists()) {
            FileUtils.cleanDirectory(file);
        }
        file.mkdirs();
    }

}