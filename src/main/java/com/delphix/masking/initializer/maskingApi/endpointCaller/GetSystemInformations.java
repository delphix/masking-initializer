package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.pojo.SystemInformation;

public class GetSystemInformations extends GetApiCall {

    private static final String GET_SYSTEM_INFORMATION_PATH = "system-information";
    private SystemInformation systemInformation;

    
    @Override
    protected void setResponse(String responseBody) {
        systemInformation = Utils.getClassFromJson(responseBody, SystemInformation.class);
        currentSize = 1;
        total = 1L;
    }

    @Override
    protected String getEndpoint(int pageNumber) {
        return GET_SYSTEM_INFORMATION_PATH;
    }

    public SystemInformation getSystemInformation() {
        return systemInformation;
    }
}