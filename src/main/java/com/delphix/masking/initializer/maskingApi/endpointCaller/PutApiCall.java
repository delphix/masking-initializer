package com.delphix.masking.initializer.maskingApi.endpointCaller;

import org.apache.http.HttpEntity;

public abstract class PutApiCall extends ApiCall {

    String id;
    String name;
    String body;
    boolean isMultiPart = false;

    protected abstract String getEndpoint();

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

    public void setId(String id) {
        this.id = id;
    }

}
