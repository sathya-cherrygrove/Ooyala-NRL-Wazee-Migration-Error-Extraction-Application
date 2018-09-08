import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class Requestor {

	static String endpoint = "https://flex.nrl.ooflex.net/api/";
	static WebTarget target;
	static Client client;
	
	static JSONObject getJsonUsingFlexAPI(String APIResourceWithRequestParameters) throws ParseException {

		System.out.println("**Get Json Using Flex API Called**Using url : "+ APIResourceWithRequestParameters);
		target = client.target(APIResourceWithRequestParameters);
		Response res = target.request().get();
		String value = res.readEntity(String.class);
		Object obj = new JSONParser().parse(value.toString());
		JSONObject jo = (JSONObject) obj;
		System.out.println(jo.toString());
		res.close();
		return jo;

	}

	static void processWorkflowJson(JSONObject jsonobject) throws ParseException {

		int WorkflowCounter = 1;
		System.out.println("**Process Workflow Json Called**");
		JSONArray ja = (JSONArray) jsonobject.get("workflows");
		System.out.println("Size of workflows : " + ja.size());
		Iterator itr1 = ja.iterator();
		while (itr1.hasNext()) {

			Iterator<Map.Entry> itr2 = ((Map) itr1.next()).entrySet().iterator();
			while (itr2.hasNext()) {
				Map.Entry pair = itr2.next();
				if (pair.getKey().equals("href")) {
					System.out.println("Processing Workflow " + WorkflowCounter + " : " + pair.getValue());
					ExtractFailedJobsWithinWorkflow(pair.getValue().toString());
					WorkflowCounter++;

				}
			}

		}

	}

	static void ExtractFailedJobsWithinWorkflow(String workflowHrefURL) throws ParseException {
		System.out.println("**Extract Failed Jobs Within Workflow Called**");
		HashMap<String, String> jobDetailsMap = new HashMap<String, String>();
		String jobId, jobName, jobStatus;
		String apiCallToExtractFailedJObsWithinWorkflow = workflowHrefURL + "/jobs";
		JSONObject jobDetails = getJsonUsingFlexAPI(apiCallToExtractFailedJObsWithinWorkflow);
		JSONArray jsonarray = (JSONArray) jobDetails.get("jobs");
		System.out.println("No of Jobs : " + jsonarray.size());
		Iterator jobIterator = jsonarray.iterator();

		while (jobIterator.hasNext()) {

			Iterator<Map.Entry> itr2 = ((Map) jobIterator.next()).entrySet().iterator();
			while (itr2.hasNext()) {
				Map.Entry pair = itr2.next();
				jobDetailsMap.put(pair.getKey().toString(), pair.getValue().toString());

			}
			processFailedJobAndLogErrorDetails(jobDetailsMap);
			jobDetailsMap.clear();

		}
	}

	static void processFailedJobAndLogErrorDetails(HashMap<String, String> JobDetailHashMap) throws ParseException {

		System.out.println("** Job Name :"+ JobDetailHashMap.get("name") + ". Status: "+ JobDetailHashMap.get("status"));
		if (JobDetailHashMap.get("status").equals("Failed")) {
			System.out.println("## FAILED JOB IDENTIFIED. Id: " + JobDetailHashMap.get("id") + ". Failed Job Name : "
					+ JobDetailHashMap.get("name"));
			
			investigateErrorHistoryOfJobAndPopulateLogs(JobDetailHashMap.get("href").toString());
			}

	  }
	
	static void investigateErrorHistoryOfJobAndPopulateLogs(String hrefUrlOfFailedJob) throws ParseException
	{
//		System.out.println("** Investigating Error History of Job href: "+ hrefUrlOfFailedJob + "**");
//		String historyOfFailedJob = hrefUrlOfFailedJob + "/history";
//		JSONObject JobHistory = getJsonUsingFlexAPI(historyOfFailedJob);
//		JSONArray jobjson = (JSONArray) JobHistory.get("events");
//		System.out.println("Size of job error array : " +jobjson );
//		Iterator iterateJobErrors = ja.iterator();
//		System.out.println(iterateJobErrors.next().toString());
		
		
		
		
	}

	public static void main(String[] args) throws ParseException {

		/* Using Login Password for Flex */
		client = ClientBuilder.newClient().register(new Authenticator("sathya", "password@123"));

		/* API Params to extract failed workflows */
		String workflowRequest = "https://flex.nrl.ooflex.net/api/workflows;status=Failed;createdFrom=20 Aug 2018;createdTo=now;owner=sathya;offset=101";

		/* Extracting Workflows */
		JSONObject jsonobject = getJsonUsingFlexAPI(workflowRequest);

		/* Processing the jsonobject received */
		processWorkflowJson(jsonobject);
		
		

	}

}
