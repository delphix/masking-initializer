package com.delphix.masking.initializer.pojo;

import lombok.Data;

import java.util.List;

@Data
public class MaskingSetup {

    private String host;
    private String port;
    private String apiPath;
    private String username;
    private String password;
    private String authToken;

    private Boolean scaled;

    private String version;

    private List<Application> applications;
    private List<FileFormat> fileFormats;
    private List<ProfileSet> profileSets;
    private List<ProfileExpression> profileExpressions;
    private List<Domain> domains;
    private List<ExportObject> exportObjects;
    private List<String> exportObjectFiles;
    private List<User> users;

}

