package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.pojo.FileFormat;
import com.delphix.masking.initializer.pojo.FileUpload;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.util.List;

public class PostFileUpload extends PostApiCall {

    private static final String POST_FILE_UPLOAD_PATH = "file-uploads";
    private HttpEntity httpEntity;

    @Override
    public void setResponse(String responseBody) {
        id = Utils.getClassFromJson(responseBody, FileUpload.class).getFileReferenceId();
    }

    @Override
    public String getEndpoint() {
        return POST_FILE_UPLOAD_PATH;
    }

    public PostFileUpload(File file) {
        isMultiPart = true;
        httpEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
    }

    @Override
    protected void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws ApiCallException {
        // No need to handle error code 409, as it will never be duplicate and always generate new file reference id.
    }

    @Override
    public HttpEntity getMultiPartEntity() {
        return httpEntity;
    }
}
