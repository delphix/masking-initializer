package com.delphix.masking.initializer.maskingApi.endpointCaller;

public abstract class GetApiCall extends ApiCall {

    protected Long total;
    protected Integer currentSize;

    protected abstract String getEndpoint(int pageNumber);

    protected boolean isNextPage() {
        if (total == null || currentSize < total) {
            return true;
        }
        return false;
    }

}
