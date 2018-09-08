import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FlexAPICaller {

	static WebTarget target;
	static Client client;

	static JSONObject extractSingleErrorJobInformation(String apiUrlForJobHistory) throws IOException, ParseException {
		
		System.out.println("** In Method : FlexAPICaller.extractSingleErrorJobInformation **");
		URL url = new URL(apiUrlForJobHistory);
		// URL url = new URL("https://flex.nrl.ooflex.net/api/jobs/16806275/history");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Cache-Control", "no-cache");
		conn.setRequestProperty("Content-Type", "application/vnd.nativ.mio.v1+json");
		conn.setRequestProperty("Authorization", "Basic c2F0aHlhOnBhc3N3b3JkQDEyMw==");
		conn.setRequestProperty("Postman-Token", "a1cdb1f8-0918-d3d5-25e4-7e0af98b090c");
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String output = br.readLine();
	    Object obj = new JSONParser().parse(output.toString());
		JSONObject jo = (JSONObject) obj;
		conn.disconnect();
		return jo;

	}

}
