package com.delphix.masking.endpointCodeGen.POJO;

import lombok.Data;

import java.util.List;

@Data
public class Endpoint {

    private List<String> className;
    private String endpointPath;
    private String nameField;
    private String idField;
    private String parentId;
    private List<MemberVariable> memberVariables;
    private Boolean generatePojo;
    private Boolean generatePost;
    private Boolean generateGet;
    private Boolean generatePut;
    private Boolean isMultiPart;
    private List<QueryParam> queryParams;

}

