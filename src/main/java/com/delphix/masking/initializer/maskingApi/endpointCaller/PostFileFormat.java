package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.pojo.FileFormat;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.nio.file.Path;

public class PostFileFormat extends PostApiCall {

    private static final String Post_FILE_FORMAT_PATH = "file-formats";
    private HttpEntity httpEntity;

    public PostFileFormat(FileFormat fileFormat, Path path) {
        body = Utils.getJSONFromClass(fileFormat);
        name = fileFormat.getFileFormatName();
        isMultiPart = true;
        httpEntity = MultipartEntityBuilder
                .create()
                .addBinaryBody("fileFormat", path.resolve(fileFormat.getFileFormatName()).toFile(), ContentType
                        .MULTIPART_FORM_DATA, fileFormat.getFileFormatName())
                .addTextBody("fileFormatType", fileFormat.getFileFormatType())
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
    }

    @Override
    public void setResponse(String responseBody) {
        id = Utils.getClassFromJson(responseBody, FileFormat.class).getFileFormatId().toString();
    }

    @Override
    public String getEndpoint() {
        return Post_FILE_FORMAT_PATH;
    }

    @Override
    protected void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws ApiCallException {
        GetFileFormats getFileFormats = new GetFileFormats();
        apiCallDriver.makeGetCall(getFileFormats);
        for (FileFormat fileFormat : getFileFormats.getFileFormats()) {
            if (!fileFormat.getFileFormatName().equalsIgnoreCase(name)) {
                continue;
            }
            id = fileFormat.getFileFormatId().toString();

        }
    }

    @Override
    public HttpEntity getMultiPartEntity() {
        return httpEntity;
    }
}
