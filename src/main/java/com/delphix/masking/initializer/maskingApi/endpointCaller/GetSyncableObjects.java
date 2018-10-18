package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Constants;
import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.pojo.ExportObjectMetadata;
import com.delphix.masking.initializer.pojo.ExportObjectMetadataList;
import lombok.Setter;

import java.util.ArrayList;

public class GetSyncableObjects extends GetApiCall {

    private static final String GET_PROFILE_EXPRESSION_PATH = "syncable-objects";
    private ExportObjectMetadataList exportObjectMetadataList;

    private ArrayList exportObjectMetadataArray;

    @Setter String objectType;


    @Override
    protected void setResponse(String responseBody) {
        if (exportObjectMetadataArray == null) {
            exportObjectMetadataArray = new ArrayList<>();
        }

        exportObjectMetadataList = Utils.getClassFromJson(responseBody, ExportObjectMetadataList.class);
        exportObjectMetadataArray.addAll(exportObjectMetadataList.getResponseList());
        currentSize = exportObjectMetadataArray.size();
        if (total == null) {
            total = exportObjectMetadataList.getPageInfo().getTotal();
        }

    }

    @Override
    protected String getEndpoint(int pageNumber) {
        String path = GET_PROFILE_EXPRESSION_PATH + "?page_size=" + Constants.PAGE_SIZE + "&&page_number=" + pageNumber;
        if (objectType != null) {
        path += "&&object_type=" + objectType;

        }

        return path;
    }

    public ArrayList<ExportObjectMetadata> getExportResponseMetadata() {
        return new ArrayList<>(exportObjectMetadataList.getResponseList());
    }
}