package com.delphix.masking.endpointCodeGen.POJO;

import lombok.Data;

@Data
public class MemberVariable {

    private String name;
    private String type;
    private String listType;

    public String getUpperName() {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}
