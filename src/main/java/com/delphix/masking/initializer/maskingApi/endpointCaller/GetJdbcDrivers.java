package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Constants;
import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.pojo.JdbcDriver;
import com.delphix.masking.initializer.pojo.JdbcDriverList;

import java.util.ArrayList;

public class GetJdbcDrivers extends GetApiCall {

    private static final String GET_JDBC_DRIVER_PATH = "jdbc-drivers";
    private JdbcDriverList jdbcDriverList;
    private ArrayList jdbcDriverArray;

    
    @Override
    protected void setResponse(String responseBody) {
        if (jdbcDriverArray == null) {
           jdbcDriverArray = new ArrayList<>();
        }

        jdbcDriverList = Utils.getClassFromJson(responseBody, JdbcDriverList.class);
        jdbcDriverArray.addAll(jdbcDriverList.getResponseList());
        currentSize = jdbcDriverArray.size();
        if (total == null) {
            total = jdbcDriverList.getPageInfo().getTotal();
        }

    }

    @Override
    protected String getEndpoint(int pageNumber) {
        String path = GET_JDBC_DRIVER_PATH + "?is_built_in=false";
                return path;
    }

    public ArrayList<JdbcDriver> getJdbcDrivers() {
        return jdbcDriverArray;
    }
}