/**
 * BwlTaggedArtifacts
 * 
 * By using Blueworks Live's REST API this application will get a list of
 * artifacts in the given account that are tagged with the given term.
 * 
 * @author Martin Westphal, westphal@de.ibm.com
 * @version 1.0
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.bind.DatatypeConverter;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * 
 * Compile:
 *    javac -cp .;commons-io-2.4.jar;wink-json4j-1.3.0.jar BwlTaggedArtifacts.java
 * 
 * Run it:
 *    java -cp .;commons-io-2.4.jar;wink-json4j-1.3.0.jar BwlTaggedArtifacts <user> <password> <account> <tag>
 * 
 */
public class BwlTaggedArtifacts {

    // --- The Blueworks Live server info and login
    private final static String REST_API_SERVER = "https://www.blueworkslive.com";
    private static String REST_API_USERNAME = "";
    private static String REST_API_PASSWORD = "";
    private static String REST_API_ACCOUNT_NAME = "";

    // --- API call parameters
    private static String SEARCH_VALUE = "";

    // --- Configuration
    private static String DELIMITER = ",";
    private static String REPLACEMENT = "";
    
    // --- Usage
    private static String USAGE = "Usage: BwlFileDownloader <user> <password> <account> <tag> [optional_arguments]\n"
    		+ "Optional arguments:\n"
    		+ "  -h               This help message\n"
    		+ "  -d <delimiter>   The delimiter for the csv outputs, default="+DELIMITER+"\n"
    		+ "  -r <replacement> The replacement for the delimiter in a name, default="+REPLACEMENT+"\n"
    		;
    

    public static void main(String[] args) {
    	int i = 4;
    	String arg;
    	if (args.length < i) printErrorAndExit("missing command line arguments, "+i+" arguments required");
    	REST_API_USERNAME = args[0];
    	REST_API_PASSWORD = args[1];
    	REST_API_ACCOUNT_NAME = args[2];
    	SEARCH_VALUE = args[3];
        
    	while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
    		if (arg.equals("-h")) printErrorAndExit("");
    		else if (arg.equals("-d")) {
                if (i < args.length) DELIMITER = args[i++];
                else printErrorAndExit("option -d requires a string argument"); 
            }
    		else if (arg.equals("-r")) {
                if (i < args.length) REPLACEMENT = args[i++];
                else printErrorAndExit("option -r requires a string argument"); 
            }
    		else  {
    			printErrorAndExit("unknown command line option "+arg);
            }
    	}
    	
        try {
            InputStream restApiStream = getSearchResponse();
            try {
                JSONObject appListResult = new JSONObject(restApiStream);
            	//System.out.println(appListResult.toString(2));
                
                JSONArray spaces = (JSONArray) appListResult.get("spaces");
                for (Object obj : spaces) {
                	JSONObject space = (JSONObject)obj; // contains: id, name, [ parentSpaceId, tags, processes, decisions,  ]
                	String spaceid = space.getString("id");
                	String spacename = space.getString("name");
                	
                	if (hasTag(space, SEARCH_VALUE)) 
                		printResult("space",spacename,spaceid);
                	
                	if (space.has("processes")) {
                		JSONArray processes = (JSONArray) space.get("processes");
                        for (Object obj2 : processes) {
                        	JSONObject process = (JSONObject)obj2; // contains: id, name, milestones, [ tags ]
                        	String processname = process.getString("name");
                        	String processid = process.getString("id");
                        	if (hasTag(process, SEARCH_VALUE)) 
                        		printResult("process",processname,processid);
                        }
                	}

                	if (space.has("decisions")) {
                		JSONArray decisions = (JSONArray) space.get("decisions");
                        for (Object obj2 : decisions) {
                        	JSONObject decision = (JSONObject)obj2; // contains: id, name, milestones, [ tags ]
                        	String decisionname = decision.getString("name");
                        	String decisionid = decision.getString("id");
                        	if (hasTag(decision, SEARCH_VALUE)) 
                        		printResult("decision",decisionname,decisionid);
                        }
                	}
                }
            } finally {
                restApiStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print search result.
     * 
     * @param type
     * @param name
     * @param id
     */
    private static void printResult(String type, String name, String id) {
    	name = name.replace(DELIMITER, REPLACEMENT);
    	System.out.println(type+DELIMITER+name+DELIMITER+id);
    }

    /**
     * Check if the JSON object contains the given tag.
     * 
     * @param obj the JSON object, e.g. space, process, decision
     * @param tagname the tag to check
     * @throws JSONException 
     */
    private static boolean hasTag (JSONObject obj, String tagname) throws JSONException {
    	if (obj.has("tags")) {
    		JSONArray tags = (JSONArray) obj.get("tags");
            for (Object obj2 : tags) {
            	JSONObject tag = (JSONObject)obj2; // contains: id, name
            	String name = tag.getString("name");
            	if (tagname.compareToIgnoreCase(name) == 0) return true;
            }
    	}
    	return false;
    }

    /**
     * Call this method to print out an error message during command line parsing,
     * together with the USAGE information and exit.
     * Use an empty message to get USAGE only.
     * 
     * @param message the error message to print
     */
    private static void printErrorAndExit (String message) {
        if (message.length() > 0) System.err.println("ERROR: "+message);
        System.err.println(USAGE);
        System.exit(1);
    }
    
    
    /**
     * Generic call of the API resource "Search".
     */
    private static InputStream getSearchResponse () throws IOException {
        StringBuilder appListUrlBuilder = new StringBuilder(REST_API_SERVER + "/scr/api/Search");
        appListUrlBuilder.append("?account=").append(REST_API_ACCOUNT_NAME);
        appListUrlBuilder.append("&version=").append("20120130");
        appListUrlBuilder.append("&searchFieldName=").append("tag");
        appListUrlBuilder.append("&searchValue=").append(URLEncoder.encode(SEARCH_VALUE, "UTF-8"));
        appListUrlBuilder.append("&returnFields=").append("tag");

        HttpURLConnection restApiURLConnection = getRestApiConnection(appListUrlBuilder.toString());
        if (restApiURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            System.err.println("Error calling the Blueworks Live REST API: " + restApiURLConnection.getResponseMessage());
            System.exit(1);
        }

        return restApiURLConnection.getInputStream();
    }
    
    
    /**
     * Set up the connection to a REST API including handling the Basic Authentication request headers that must be
     * present on every API call.
     * 
     * @param apiCall The URL string indicating the api call and parameters.
     * @return the open connection
     */
    public static HttpURLConnection getRestApiConnection(String apiCall) throws IOException {

        // Call the provided api on the Blueworks Live server
        URL restApiUrl = new URL(apiCall);
        HttpURLConnection restApiURLConnection = (HttpURLConnection) restApiUrl.openConnection();

        // Add the HTTP Basic authentication header which should be present on every API call.
        addAuthenticationHeader(restApiURLConnection);

        return restApiURLConnection;
    }

    /**
     * Add the HTTP Basic authentication header which should be present on every API call.
     * 
     * @param restApiURLConnection The open connection to the REST API.
     */
    private static void addAuthenticationHeader(HttpURLConnection restApiURLConnection) {
        String userPwd = REST_API_USERNAME + ":" + REST_API_PASSWORD;
        String encoded = DatatypeConverter.printBase64Binary(userPwd.getBytes());
        restApiURLConnection.setRequestProperty("Authorization", "Basic " + encoded);
    }
}
