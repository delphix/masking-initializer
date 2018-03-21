package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.pojo.ExportObject;
import com.delphix.masking.initializer.pojo.ExportObjectMetadata;

public class PostExportObject extends PostApiCall {

    private ExportObject exportObject;

    public PostExportObject(ExportObjectMetadata[] exportObjectMetadata) {
        body = Utils.getJSONFromClass(exportObjectMetadata);
    }

    @Override
    protected void setResponse(String responseBody) {
        exportObject = Utils.getClassFromJson(responseBody, ExportObject.class);
    }

    @Override
    protected String getEndpoint() {
        return "export";
    }

    @Override
    protected void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws ApiCallException {
        // this should never happen
    }

    public ExportObject getExportObject() {
        return exportObject;
    }
}
