package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.pojo.JdbcDriver;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.nio.file.Path;

public class PostJdbcDriver extends PostApiCall {

    private static final String Post_JDBC_DRIVER_PATH = "jdbc-drivers";
    private HttpEntity httpEntity;
    private File driverFile;

    @Override
    public void setResponse(String responseBody) {
        id = Utils.getClassFromJson(responseBody, JdbcDriver.class).getJdbcDriverId().toString();
    }

    @Override
    public String getEndpoint() {
        return Post_JDBC_DRIVER_PATH;
    }

    @Override
    protected void handle409(ApiCallDriver apiCallDriver, String body, boolean replace) throws ApiCallException {
        GetJdbcDrivers getJdbcDrivers = new GetJdbcDrivers();
        apiCallDriver.makeGetCall(getJdbcDrivers);
        for (JdbcDriver existingJdbcDriver: getJdbcDrivers.getJdbcDrivers()) {
            if (!existingJdbcDriver.getDriverName().equalsIgnoreCase(name)) {
                continue;
            }
            id = existingJdbcDriver.getJdbcDriverId().toString();
            if (replace) {
                JdbcDriver jdbcDriver = Utils.getClassFromJson(body, JdbcDriver.class);
                jdbcDriver.setJdbcDriverId(existingJdbcDriver.getJdbcDriverId());
                apiCallDriver.makePutCall(new PutJdbcDriver(jdbcDriver, this.driverFile));
            }
        }
    }
    public PostJdbcDriver(JdbcDriver jdbcDriver, File driverFile) {
        this.driverFile = driverFile;
        body = Utils.getJSONFromClass(jdbcDriver);
        name = jdbcDriver.getDriverName();
        isMultiPart = true;
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        if(jdbcDriver.getDescription() != null) {
            multipartEntityBuilder.addTextBody("description", jdbcDriver.getDescription());
        }
        httpEntity = multipartEntityBuilder
                .addBinaryBody("driverFile", driverFile, ContentType.MULTIPART_FORM_DATA, driverFile.getName())
                .addTextBody("driverName", jdbcDriver.getDriverName())
                .addTextBody("driverClassName", jdbcDriver.getDriverClassName())
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
    }

    @Override
    public HttpEntity getMultiPartEntity() {
        return httpEntity;
    }
}
