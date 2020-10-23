package com.delphix.masking.initializer.maskingApi.endpointCaller;

import com.delphix.masking.initializer.Utils;
import com.delphix.masking.initializer.exception.ApiCallException;
import com.delphix.masking.initializer.pojo.apiBody.LoginApiBody;
import com.delphix.masking.initializer.pojo.apiResponse.ApiErrorMessage;
import com.delphix.masking.initializer.pojo.apiResponse.LoginApiResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class ApiCallDriver {

    private static final String URL = "%s://%s:%s/%s%s";
    private static final String URL_WITH_VERSION = "%s://%s:%s/%s%s%s";
    private static final String BASE_PATH = "%s/api/";
    private static final String VERSION = "v%s/";
    private static final String LOGIN_PATH = "login";
    Logger logger = LogManager.getLogger(ApiCallDriver.class);
    private String host;
    private String port;
    private String apiPath;
    private String apiVersion;
    private String username;
    private String password;
    private String Authorization;
    private boolean replace;
    private boolean sslEnabled;

    public ApiCallDriver(String host, String username, String password, String port, String apiPath, String apiVersion, boolean replace, boolean sslEnabled)
            throws ApiCallException {
        this.host = host;
        this.port = port;
        this.apiPath = apiPath;
        this.apiVersion = apiVersion;
        this.username = username;
        this.password = password;
        this.replace = replace;
        this.sslEnabled = sslEnabled;
        login();
    }

    public ApiCallDriver(String host, String authToken, String port, String apiPath, String apiVersion, boolean replace, boolean sslEnabled) {
        this.host = host;
        this.port = port;
        this.apiPath = apiPath;
        this.apiVersion = apiVersion;
        this.Authorization = authToken;
        this.replace = replace;
        this.sslEnabled = sslEnabled;
    }

    private void login() throws ApiCallException {
        LoginApiBody loginApiBody = new LoginApiBody(username, password);
        String responseJson = makePostCall(LOGIN_PATH, Utils.getJSONFromClass(loginApiBody));
        Authorization = Utils.getClassFromJson(responseJson, LoginApiResponse.class).getAuthorization();
    }

    public void makePostCall(PostApiCall postApiCall) throws ApiCallException {
        try {
            String response;
            if (postApiCall.isMultiPart) {
                response = makeMultiPartPostCall(postApiCall.getEndpoint(), postApiCall.getMultiPartEntity());
            } else {
                response = makePostCall(postApiCall.getEndpoint(), postApiCall.getBody());
            }
            postApiCall.setResponse(response);
        } catch (ApiCallException e) {
            if (new Integer(409).equals(e.getStatusCode())) {
                postApiCall.handle409(this, postApiCall.getBody(), replace);
                return;
            }
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void makeGetCall(GetApiCall getApiCall) throws ApiCallException {

        int pageNumber = 1;
        while (getApiCall.isNextPage()) {
            String response = makeGetApiCall(getApiCall.getEndpoint(pageNumber));
            getApiCall.setResponse(response);
            pageNumber++;
        }

    }

    public void makePutCall(PutApiCall putApiCall) throws ApiCallException {
        String response;
        if (putApiCall.isMultiPart) {
            response = makeMultiPartPutCall(putApiCall.getEndpoint(), putApiCall.getMultiPartEntity());
        } else {
            response = makePutCall(putApiCall.getEndpoint(), putApiCall.getBody());
        }
        putApiCall.setResponse(response);
    }

    private String makeGetApiCall(String endpointPath) throws ApiCallException {
        String url = constructUrl(endpointPath);

        logger.info(url + " GET");

        HttpGet httpGet = new HttpGet(url);

        return handleResponse(httpGet, url, false);

    }

    private String makeMultiPartPostCall(String endpointPath, HttpEntity httpEntity) throws ApiCallException,
            IOException {
        String url = constructUrl(endpointPath);

        logger.info(url + " POST");

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(httpEntity);

        return handleResponse(httpPost, url, true);

    }

    private String makePostCall(String endpointPath, String jsonBody) throws ApiCallException {
        String url = constructUrl(endpointPath);

        logger.info(url + " POST");
        logger.debug("Request Body: {}", jsonBody);

        StringEntity stringEntity;
        stringEntity = new StringEntity(jsonBody, StandardCharsets.UTF_8);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(stringEntity);

        return handleResponse(httpPost, url, false);

    }

    private String makeMultiPartPutCall(String endpointPath, HttpEntity httpEntity) throws ApiCallException {
        String url = String.format(URL, getProtocol(), host, port, String.format(BASE_PATH, apiPath), endpointPath);

        logger.info(url + " PUT");

        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(httpEntity);

        return handleResponse(httpPut, url, true);

    }

    private String makePutCall(String endpointPath, String jsonBody) throws ApiCallException {
        String url = constructUrl(endpointPath);

        logger.info(url + " PUT");
        logger.debug("Request Body: {}", jsonBody);

        HttpPut httpPut = new HttpPut(url);
        if(StringUtils.isNotEmpty(jsonBody)) {
            StringEntity stringEntity;
            stringEntity = new StringEntity(jsonBody, StandardCharsets.UTF_8);

            httpPut.setEntity(stringEntity);
        }

        return handleResponse(httpPut, url, false);

    }
    private void setHeaders(HttpRequestBase httpRequestBase, boolean isMultiPart) {

        if (!isMultiPart) {
            httpRequestBase.setHeader("Content-type", "application/json");
        }

        httpRequestBase.setHeader("Accept", "application/json");

        if (Authorization != null) {
            httpRequestBase.setHeader("Authorization", Authorization);
        }
    }

    private String handleResponse(HttpRequestBase httpRequestBase, String url, boolean isMultiPart) throws
            ApiCallException {

        try {

            CloseableHttpClient httpClient;
            if (sslEnabled) {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true).build();
                httpClient = HttpClients.custom()
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
                        .build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }

            setHeaders(httpRequestBase, isMultiPart);
            HttpResponse httpResponse = httpClient.execute(httpRequestBase);

            InputStreamReader inputStreamReader = new InputStreamReader(httpResponse.getEntity().getContent());
            String responseBody = IOUtils.toString(inputStreamReader);

            String prettyJsonString;
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(responseBody);
                prettyJsonString = gson.toJson(je);
            } catch (JsonParseException e) {
                throw new ApiCallException("Unable to parse the response into JSON: " + responseBody, e);
            }
            logger.debug("Response body: {}", prettyJsonString);

            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                ApiErrorMessage apiErrorMessage = Utils.getClassFromJson(responseBody, ApiErrorMessage.class);
                throw new ApiCallException(httpRequestBase.getMethod(), httpResponse.getStatusLine().getStatusCode(),
                        url, apiErrorMessage.getErrorMessage());
            }
            return responseBody;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new ApiCallException(url, e);
        }
    }

    private String getProtocol() {
        return sslEnabled ? "https": "http";
    }

    private String constructUrl(String endpointPath) {
        String url = "";
        if(apiVersion != null) {
            url = String.format(URL_WITH_VERSION, getProtocol(), host, port, String.format(BASE_PATH, apiPath), String.format(VERSION, apiVersion), endpointPath);
        } else {
            url = String.format(URL, getProtocol(), host, port, String.format(BASE_PATH, apiPath), endpointPath);
        }
        return url;
    }
}
