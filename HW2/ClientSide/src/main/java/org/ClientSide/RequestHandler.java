package org.ClientSide;

import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;

public class RequestHandler {
  private final CloseableHttpClient httpClient;
  private final String baseUrl;
  private final Gson gson = new Gson();
  private String lastAlbumID;

  public RequestHandler(CloseableHttpClient httpClient, String baseUrl) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
  }

  public int sendGetRequest(String albumID) {
    int statusCode = -1;
    String endPoint = baseUrl + "/albums/" + albumID;
    HttpGet httpGet = new HttpGet(endPoint);

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      statusCode = response.getStatusLine().getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return statusCode;

  }

  public int sendPostRequest(String artist, String title, String year, File imageFile) {
    int statusCode = -1;
    String endPoint = baseUrl + "/albums";
    HttpPost httpPost = new HttpPost(endPoint);

    String jsonProfileText = String.format("{\"artist\": \"%s\", \"title\": \"%s\", \"year\": \"%s\"}", artist, title, year);
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addBinaryBody("image", imageFile, ContentType.DEFAULT_BINARY, "nmtb.png");
    builder.addTextBody("profile", jsonProfileText, ContentType.APPLICATION_JSON);

    httpPost.setEntity(builder.build());

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      statusCode = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      String responseBody = EntityUtils.toString(entity);
      JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
      if (responseJson.has("ID")) {
        lastAlbumID = responseJson.get("ID").getAsString();
      }
      EntityUtils.consume(entity);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return statusCode;
  }

  public String getLastAlbumID() {
    return lastAlbumID;
  }

}
