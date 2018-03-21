package com.delphix.masking.initializer.maskingApi.endpointCaller;


import com.delphix.masking.initializer.exception.ApiCallException;
import org.apache.http.HttpEntity;

public abstract class PostApiCall extends ApiCall {

    String id;
    String name;
    String body;
    Integer parentId;
    boolean isMultiPart = false;

    protected abstract String getEndpoint();

    protected abstract void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws
            ApiCallException;

    public HttpEntity getMultiPartEntity() {
        return null;
    }

    String getBody() {
        return body;
    }

    public String getId() {
        if (id == null) {
            throw new RuntimeException("Response not set. Make sure the call was made and that it did not come back " +
                    "with an error");
        }
        return id;
    }
}
