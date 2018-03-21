package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.pojo.ExportObject;
import com.delphix.masking.initializer.pojo.ImportObjectMetadata;

public class PostImportObject extends PostApiCall {

    private ImportObjectMetadata[] importObjectMetadata;

    public PostImportObject(ExportObject exportObject) {
        body = Utils.getJSONFromClass(exportObject);
    }

    @Override
    protected void setResponse(String responseBody) {
        importObjectMetadata = Utils.getClassFromJson(responseBody, ImportObjectMetadata[].class);
    }

    @Override
    protected String getEndpoint() {
        return "import?force_overwrite=true";
    }

    @Override
    protected void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws ApiCallException {
        // this should never happen
    }

    public ImportObjectMetadata[] getImportObjectMetadata() {
        return importObjectMetadata;
    }
}
