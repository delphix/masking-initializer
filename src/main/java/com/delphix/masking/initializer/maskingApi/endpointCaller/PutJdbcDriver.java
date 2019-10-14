package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.pojo.JdbcDriver;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;

public class PutJdbcDriver extends PutApiCall {

    private static final String Post_JDBC_DRIVER_PATH = "jdbc-drivers/";
    private HttpEntity httpEntity;

    @Override
    public void setResponse(String responseBody) {
        id = Utils.getClassFromJson(responseBody, JdbcDriver.class).getJdbcDriverId().toString();
    }

    @Override
    public String getEndpoint() {
        try {
            return Post_JDBC_DRIVER_PATH + URLEncoder.encode(id, "ISO-8859-1").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public PutJdbcDriver(JdbcDriver jdbcDriver, File driverFile) {
        body = Utils.getJSONFromClass(jdbcDriver);
        name = jdbcDriver.getDriverName();
        id = jdbcDriver.getJdbcDriverId().toString();
        isMultiPart = true;
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        if(driverFile != null && driverFile.exists() && !driverFile.isDirectory()) {
            multipartEntityBuilder.addBinaryBody(
                    "driverFile",
                    driverFile,
                    ContentType.MULTIPART_FORM_DATA,
                    driverFile.getName());
        }
        httpEntity = multipartEntityBuilder
                .addTextBody("jdbcDriverId", jdbcDriver.getJdbcDriverId().toString())
                .addTextBody("driverClassName", jdbcDriver.getDriverClassName())
                .addTextBody("description", jdbcDriver.getDescription())
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
    }

    @Override
    public HttpEntity getMultiPartEntity() {
        return httpEntity;
    }
}
