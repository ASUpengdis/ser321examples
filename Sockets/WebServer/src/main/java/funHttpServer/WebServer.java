/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(new String(readFileInBytes(file)));
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          try
          {
	          query_pairs = splitQuery(request.replace("multiply?", ""));
	
	          // extract required fields from parameters
	          try
	          {
	        	  Integer num1 = Integer.parseInt(query_pairs.get("num1"));
	        	  Integer num2 = Integer.parseInt(query_pairs.get("num2"));
	        	  // do math
	        	  Integer result = num1 * num2;
	          
	        	  // Generate response
	        	  builder.append("HTTP/1.1 200 OK\n");
	        	  builder.append("Content-Type: text/html; charset=utf-8\n");
	        	  builder.append("\n");
	        	  builder.append("Result is: " + result);
	
	          }
	          catch(NumberFormatException e)
	          {
	        	  builder.append("HTTP/1.1 400 Incorrect Syntax\n");
	        	  builder.append("Content-Type: text/html; charset=utf-8\n");
	        	  builder.append("\n");
	        	  builder.append("Error 400: Please use integers or correct variable names");
	          }
          }
          catch(Exception e)
          {
        	  builder.append("HTTP/1.1 400 Incorrect Syntax\n");
        	  builder.append("Content-Type: text/html; charset=utf-8\n");
        	  builder.append("\n");
        	  builder.append("Error 400: Please include all parameters");
          }
          // TODO: Include error handling here with a correct error code and
          // a response that makes sense

        } else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          //     then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
          //     "/repos/OWNERNAME/REPONAME/contributors"

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          try
          {
	          query_pairs = splitQuery(request.replace("github?", ""));
	          try
	          {
	        	  String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
	        	  //System.out.println(json);
	        	  
	        	  JSONArray repoArray = new JSONArray(json);
	        	  JSONArray newArray = new JSONArray();
	        	  for(int i = 0; i < repoArray.length(); i++)
	        	  {
	        		  JSONObject temp = new JSONObject();
	        		  temp.put("ownerName", repoArray.getJSONObject(i).getJSONObject("owner").getString("login"));
	        		  temp.put("ownerID",repoArray.getJSONObject(i).getJSONObject("owner").getInt("id"));
	        		  temp.put("repoName", repoArray.getJSONObject(i).getString("name"));
	        		  
	        		  newArray.put(temp);
	        	  }
	        	  
	        	  builder.append("HTTP/1.1 200 OK\n");
	        	  builder.append("Content-Type: text/html; charset=utf-8\n");
	        	  builder.append("\n");
	        	  for(int i = 0; i < newArray.length();i++)
	        	  {
	        		  builder.append("<div>").append(newArray.getJSONObject(i).getString("ownerName") + ", "
	        				  + newArray.getJSONObject(i).getInt("ownerID") + " -> "
	        				  + newArray.getJSONObject(i).getString("repoName")+'\n');
	        		  builder.append('\n').append("</div>");
	        	  }
	          }
	          catch(Exception e)
	          {
	        	  builder.append("HTTP/1.1 400 Syntax Error\n");
	        	  builder.append("Content-Type: text/html; charset=utf-8\n");
	        	  builder.append("\n");
	        	  builder.append("Error 400: The directory provided does not exist or your query is misspelled");
	        	  e.printStackTrace();
	          }
          }
          catch(Exception e)
          {
        	  	builder.append("HTTP/1.1 400 Syntax Error\n");
      	  		builder.append("Content-Type: text/html; charset=utf-8\n");
      	  		builder.append("\n");
      	  		builder.append("Please include the parameters. i.e: /github?query=user/amehlhase316/repos");
          }
          //builder.append("Check the todos mentioned in the Java source file");
          // TODO: Parse the JSON returned by your fetch and create an appropriate
          // response
          // and list the owner name, owner id and name of the public repo on your webpage, e.g.
          // amehlhase, 46384989 -> memoranda
          // amehlhase, 46384989 -> ser316examples
          // amehlhase, 46384989 -> test316
          

        }
        else if(request.contains("projectile?")) {
        	Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            // extract path parameters
        	try
        	{
            query_pairs = splitQuery(request.replace("projectile?", ""));
            
            Double angle = 0.0;
            Double velocity = 0.0;
            boolean pass = false;
            boolean caught = false;
            try
            {
            	angle = Double.parseDouble(query_pairs.get("angle"));
            	velocity = Double.parseDouble(query_pairs.get("velocity"));
            	pass = true;
            	
            } catch (NumberFormatException e)
            {
            	builder.append("HTTP/1.1 400 Syntax Error\n");
          	  	builder.append("Content-Type: text/html; charset=utf-8\n");
          	  	builder.append("\n");
          	  	builder.append("Error 400: Please make sure you are using numbers for the values and that the parameter names are correctly spelled");
          	  	caught = true;
            }
            
            boolean goodAngle = true;
            boolean goodVelocity = true;
            if(angle < 0 || angle > 90)
            {
            	goodAngle = false;
            }
            if(velocity < 0)
            {
            	goodVelocity = false;
            }
            if(goodAngle == false || goodVelocity == false)
            {
            	pass = false;
            }
            
            if(pass == true)
            {
            	double radians = Math.toRadians(angle);
            	double maxHeight = 0;
            	double maxDistance = 0;
            	double timeOfFlight = 0;
            	
            	double yVelocity =  velocity * Math.sin(radians);
            	double xVelocity = velocity * Math.cos(radians);
            	
            	double timeToMaxHeight = yVelocity / 9.81;
            	maxHeight = yVelocity * timeToMaxHeight + .5 * (-9.81) * Math.pow(timeToMaxHeight, 2);
            	
            	timeOfFlight = timeToMaxHeight * 2;
            	
            	maxDistance = xVelocity * timeOfFlight;
            	
            	builder.append("HTTP/1.1 200 OK\n");
          	  	builder.append("Content-Type: text/html; charset=utf-8\n");
          	  	builder.append("\n");
          	  	builder.append(String.format("<div>The max height of the projectile is: %.2f meters </div>",maxHeight));
          	  	builder.append(String.format("<div>The max distance of the projectile is: %.2f meters </div>",maxDistance));
          	  	builder.append(String.format("<div>The total time of flight of the projectile is: %.2f seconds </div>",timeOfFlight));
            }
            else
            {
            	if(caught == false)
            	{
            		builder.append("HTTP/1.1 400 Syntax Error\n");
            		builder.append("Content-Type: text/html; charset=utf-8\n");
          	  		builder.append("\n");
            	}
          	  	if(goodAngle == false)
          	  		builder.append("<div>Please make sure the angle is between 0 and 90.</div>");
          	  	if(goodVelocity == false)
          	  		builder.append("<div>Please make sure that the velocity is greater than 0.</div>");
            }
        	}
        	catch(Exception e)
        	{
        		builder.append("HTTP/1.1 400 Syntax Error\n");
        	  	builder.append("Content-Type: text/html; charset=utf-8\n");
        	  	builder.append("\n");
        	  	builder.append("Please include the parameters. i.e: /projectile?angle=45&velocity=100");
        	}
        }
        else if(request.contains("dice?"))
        {
        	Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            // extract path parameters
        	try
        	{
	            query_pairs = splitQuery(request.replace("dice?", ""));
	            if(query_pairs.size() < 3)
	            {
	            	builder.append("HTTP/1.1 400 Syntax Error\n");
	          	  	builder.append("Content-Type: text/html; charset=utf-8\n");
	          	  	builder.append("\n");
	          	  	builder.append("Please include the parameters. i.e: /dice?dice1=6&dice2=20&roll=10");
	            }
	            else
	            {
			        int numSides1 = 0;
			        int numSides2 = 0;
			        int numRolls = 0;
			        boolean valid = false;
			        try
			        {
			        	numSides1 = Integer.parseInt(query_pairs.get("dice1"));
			        	numSides2 = Integer.parseInt(query_pairs.get("dice2"));
			        	numRolls = Integer.parseInt(query_pairs.get("rolls"));
			        	valid = true;
			        }
			        catch (NumberFormatException e)
			        {
			        	builder.append("HTTP/1.1 400 Syntax Error\n");
			      	  	builder.append("Content-Type: text/html; charset=utf-8\n");
			      	  	builder.append("\n");
			      	  	builder.append("Error 400: Please make sure you are using numbers for the values and that the parameter names are correctly spelled");
			        }
			        
			        if(valid == true && (numSides1 < 1 || numSides2 < 1 || numRolls < 1))
			        {
			        	builder.append("HTTP/1.1 400 Syntax Error\n");
			      	  	builder.append("Content-Type: text/html; charset=utf-8\n");
			      	  	builder.append("\n");
			      	  	builder.append("Error 400: Please use a number greater than 0 for all variables");
			        }
			        else if(valid == true)
			        {
			        	int[] rollValues = new int[numRolls];
			        	int roll1 = 0;
			        	int roll2 = 0;
			        	int maxValue = 0;
			        	int totalValue = 0;
			        	String rolls = "";
			        	
			        	for(int i = 0; i < numRolls; i++)
			        	{
			        		roll1 = (int)(Math.random()*numSides1)+1;
			        		roll2 = (int)(Math.random()*numSides2)+1;
			        		rollValues[i] = roll1+roll2;
			        		
			        		if(rollValues[i] > maxValue)
			        			maxValue = rollValues[i];
			        		
			        		totalValue += rollValues[i];
			        		if(i + 1 != numRolls)
			        		{
			        			rolls += rollValues[i] + ", ";
			        		}
			        		else
			        		{
			        			rolls += rollValues[i];
			        		}
			        	}
			        	double averageValue = totalValue * 1.0 / numRolls;
			        	
			        	builder.append("HTTP/1.1 200 OK\n");
			      	  	builder.append("Content-Type: text/html; charset=utf-8\n");
			      	  	builder.append("\n");
			      	  	builder.append("<div>Rolls: ["+rolls+"]</div>");
			      	  	builder.append(String.format("<div>Largest roll was: %1d</div>", maxValue));
			      	  	builder.append(String.format("<div>The average roll was: %.2f</div>", averageValue));
			        }
	            }
        	}
        	catch(Exception e)
        	{
        		builder.append("HTTP/1.1 400 Syntax Error\n");
        	  	builder.append("Content-Type: text/html; charset=utf-8\n");
        	  	builder.append("\n");
        	  	builder.append("Please include the parameters. i.e: /dice?dice1=6&dice2=20&roll=10");
        	}
        }
        else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn =  url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
