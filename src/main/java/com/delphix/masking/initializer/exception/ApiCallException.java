package com.delphix.masking.initializer.exception;

public class ApiCallException extends RuntimeException {
    private static final String ERROR_POST_STATUS_EXCEPTION = "Got status code: [%d] with message [%s] trying to hit " +
            "URL: [%s] with body [%s]";
    private static final String ERROR_POST_FORMAT_EXCEPTION = "Got [%s] trying to hit URL: [%s] with body [%s]";
    private static final String ERROR_GET_FORMAT_EXCEPTION = "Got [%s] trying to hit URL: [%s]";
    private static final String ERROR_GET_STATUS_EXCEPTION = "Got status code: [%d] with message [%s] trying to hit " +
            "URL: [%s]";
    private static final String ERROR_GET_STATUS_TYPE_EXCEPTION = "Got status code: [%d] with message [%s] trying to " +
            "hit URL: [%s] [%s]";


    private int statusCode = -1;
    private String responseError = "";

    public ApiCallException(String url, String body, Exception e) {
        super(String.format(ERROR_POST_FORMAT_EXCEPTION, e.getClass(), url, body), e);
    }

    public ApiCallException(int statusCode, String url, String requestBody, String responseError) {
        super(String.format(ERROR_POST_STATUS_EXCEPTION, statusCode, responseError, url, requestBody));
        this.statusCode = statusCode;
        this.responseError = responseError;
    }

    public ApiCallException(String url, Exception e) {
        super(String.format(ERROR_GET_FORMAT_EXCEPTION, e.getClass(), url));
    }

    public ApiCallException(int statusCode, String url, String responseError) {
        super(String.format(ERROR_GET_STATUS_EXCEPTION, statusCode, responseError, url));
        this.statusCode = statusCode;
        this.responseError = responseError;
    }

    public ApiCallException(String operation, int statusCode, String url, String responseError) {
        super(String.format(ERROR_GET_STATUS_TYPE_EXCEPTION, statusCode, responseError, url, operation));
        this.statusCode = statusCode;
        this.responseError = responseError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseError() {
        return responseError;
    }
}
