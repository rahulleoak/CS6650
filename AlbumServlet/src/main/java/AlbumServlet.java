import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.google.gson.Gson;

@MultipartConfig
@WebServlet(name = "albumServlet", value = "/albums")
public class AlbumServlet extends HttpServlet {
  private static Map<String,Album> albumData= new HashMap<>();
  private static Map<String,String> imageMetadata = new HashMap<>();
  private final Gson gson = new Gson();

  //FOR TESTING PURPOSES: start
  Album testAlbum = new Album("Sex Pistols","Never Mind The Bollocks","1977");
  //FOR TESTING PURPOSES: end
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    System.out.println(this.albumData);
    //FOR TESTING PURPOSES: start
    albumData.put("1",testAlbum);
    //FOR TESTING PURPOSES: end

    //Validate URL
    String urlPath = req.getPathInfo();
    if(urlPath==null||urlPath.isEmpty()){
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    //Check field
    String albumID = req.getPathInfo().substring(1);
    System.out.println(albumID);
    if(albumData.containsKey(albumID)){
      Album album = albumData.get(albumID);
      //Load JSON
      JsonObject albumJSON = new JsonObject();
      albumJSON.addProperty("albumID",albumID);
      albumJSON.addProperty("profile",gson.toJson(albumData.get(albumID)));

      //Print response out
      PrintWriter out = resp.getWriter();
      out.print(gson.toJson(albumJSON));
      out.flush();
    }
    else{
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.getWriter().write("Album not found");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");

    //Generate new albumID
    String albumID = UUID.randomUUID().toString();

    //Read profile part
    Part profilePart = req.getPart("profile");
    BufferedReader reader = new BufferedReader(new InputStreamReader(profilePart.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder profileContent = new StringBuilder();
    String line;
    while ((line = reader.readLine())!= null){
      profileContent.append(line);
    }
    String profileJSON = profileContent.toString();
    //Parse profileJSON to an album object
    Album newAlbum = gson.fromJson(profileJSON,Album.class);

    //Read image data as byte stream
    Part imagePart = req.getPart("image");
    float imageSize=0;
    if (imagePart!=null){
      imageSize = imagePart.getSize();
    }
    else{
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.getWriter().write("Image not found");
    }

    //Load the new id->album pair to albumData map
    this.albumData.put(albumID,newAlbum);
    //Add id->imgSize pair to imageMetadata map
    this.imageMetadata.put(albumID, String.valueOf(imageSize));
    System.out.println(albumData);
    //Create and Send JSON response
    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("ID",albumID);
    jsonResponse.addProperty("imageSize",imageSize);
    PrintWriter out = resp.getWriter();
    out.print(gson.toJson(jsonResponse));
    out.flush();
  }
}
