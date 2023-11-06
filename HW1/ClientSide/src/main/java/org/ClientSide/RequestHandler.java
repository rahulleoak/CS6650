package org.ClientSide;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import java.io.File;
import java.io.IOException;

public class RequestHandler {

  private final CloseableHttpClient httpClient;
  private final String baseUrl;

  public RequestHandler(CloseableHttpClient httpClient, String baseUrl) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
  }

  public int sendGetRequest(String albumID) {
    try {
      String endPoint = baseUrl + "/albums/" + albumID;
      HttpGet httpGet = new HttpGet(endPoint);

      HttpResponse response = httpClient.execute(httpGet);
      return handleResponse(response);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }

  public int sendPostRequest(String artist, String title, String year, File imageFile) {
    try {
      HttpClient client = this.httpClient;
      String endPoint = baseUrl + "/albums";
      HttpPost httpPost = new HttpPost(endPoint);

      String jsonProfileText = String.format("{\"artist\": \"%s\", \"title\": \"%s\", \"year\": \"%s\"}", artist, title, year);
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addBinaryBody("image", imageFile, ContentType.DEFAULT_BINARY, "nmtb.png");
      builder.addTextBody("profile", jsonProfileText, ContentType.APPLICATION_JSON);

      httpPost.setEntity(builder.build());
      HttpResponse response = client.execute(httpPost);
      return handleResponse(response);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }

  public int handleResponse(HttpResponse response) throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      String responseBody = EntityUtils.toString(entity);
    }
    return statusCode;
  }

}
