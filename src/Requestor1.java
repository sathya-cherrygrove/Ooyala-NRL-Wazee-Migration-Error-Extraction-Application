import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Requestor1 {

	static int WorkFlowCounter = 1;
	static String endpoint = "https://flex.nrl.ooflex.net/api/";
	static WebTarget target;
	static Client client;
	static LinkedHashMap<String, ArrayList<String>> CompleteJobDetails = new LinkedHashMap<String, ArrayList<String>>();
	static String LOG_workflowname, LOG_workflowid, LOG_jobname, LOG_jobid, LOG_failmessage, LOG_faileddate;

	static JSONObject getJsonUsingFlexAPI(String APIResourceWithRequestParameters) throws ParseException {

		System.out.println("**Get Json Using Flex API Called**Using url : " + APIResourceWithRequestParameters);
		target = client.target(APIResourceWithRequestParameters);
		Response res = target.request().get();
		String value = res.readEntity(String.class);
		Object obj = new JSONParser().parse(value.toString());
		JSONObject jo = (JSONObject) obj;
		res.close();
		return jo;

	}

	static void processWorkflowJson(JSONObject jsonobject) throws ParseException, IOException {

		System.out.println("**Process Workflow Json Called**");
		JSONArray ja = (JSONArray) jsonobject.get("workflows");
		System.out.println("Size of workflows : " + ja.size());
		Iterator itr1 = ja.iterator();
		while (itr1.hasNext()) {

			System.out.println("Processing Workflow :" + WorkFlowCounter);
			Iterator<Map.Entry> itr2 = ((Map) itr1.next()).entrySet().iterator();
			while (itr2.hasNext()) {
				Map.Entry pair = itr2.next();
				if (pair.getKey().equals("name")) {
					LOG_workflowname = pair.getValue().toString();

				}

				if (pair.getKey().equals("id")) {
					LOG_workflowid = pair.getValue().toString();
				}

				if (pair.getKey().equals("href")) {
					System.out.println("Processing Workflow " + WorkFlowCounter + " : " + pair.getValue());
					ExtractFailedJobsWithinWorkflow(pair.getValue().toString());
					WorkFlowCounter++;

				}

			}

			System.out.println("Workflow Processing Complete");
			System.out.println("Failed Job Details : LOG_workflowname" + LOG_workflowname + "LOG_workflowid :"
					+ LOG_workflowid + "LOG_jobname: " + LOG_jobname + "LOG_jobid: " + LOG_jobid + "LOG_failmessage:"
					+ LOG_failmessage + "LOG_faileddate:" + LOG_faileddate);
			ArrayList<String> alist = new ArrayList<String>(
					Arrays.asList(LOG_jobname, LOG_failmessage, LOG_faileddate, LOG_workflowid, LOG_workflowname));
			CompleteJobDetails.put(LOG_jobid, alist);

		}

		System.out.println("*** All Workflows Processed *** No Processed" + CompleteJobDetails.size());
		createCSVFileFromJobDetails(CompleteJobDetails);
		System.out.println("Details Logged into CSV File");

	}

	static void ExtractFailedJobsWithinWorkflow(String workflowHrefURL) throws ParseException, IOException {
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

	static void processFailedJobAndLogErrorDetails(HashMap<String, String> JobDetailHashMap)
			throws ParseException, IOException {

		if (JobDetailHashMap.get("status").equals("Failed")) {
			System.out.println("## FAILED JOB IDENTIFIED. Id: " + JobDetailHashMap.get("id") + ". Failed Job Name : "
					+ JobDetailHashMap.get("name"));
			System.out.println(
					"** Job Name :" + JobDetailHashMap.get("name") + ". Status: " + JobDetailHashMap.get("status"));
			LOG_jobname = JobDetailHashMap.get("name").toString();
			LOG_jobid = JobDetailHashMap.get("id").toString();

			investigateErrorHistoryOfJobAndPopulateLogs(JobDetailHashMap.get("href").toString());
		}

	}

	static void investigateErrorHistoryOfJobAndPopulateLogs(String hrefUrlOfFailedJob)
			throws ParseException, IOException {

		HashMap<String, String> JobErrorDetails = new HashMap<String, String>();
		System.out.println("** Investigating Error History of Job href: " + hrefUrlOfFailedJob + "**");
		String historyOfFailedJob = hrefUrlOfFailedJob + "/history";
		JSONObject JobHistory = FlexAPICaller.extractSingleErrorJobInformation(historyOfFailedJob);
		JSONArray jobjson = (JSONArray) JobHistory.get("events");
		Iterator iterateJobErrors = jobjson.iterator();

		while (iterateJobErrors.hasNext()) {

			Iterator<Map.Entry> itr2 = ((Map) iterateJobErrors.next()).entrySet().iterator();
			while (itr2.hasNext()) {
				Map.Entry pair = itr2.next();
				if (pair.getKey().equals("exceptionMessage"))

				{
					System.out.println(pair.getKey() + "  " + pair.getValue());
					String correctedString = pair.getValue().toString();
					correctedString = correctedString.replaceAll(",", "*");

					JobErrorDetails.put(pair.getKey().toString(), correctedString);
				}
				if (pair.getKey().equals("time")) {
					// System.out.println(pair.getKey() + " " + pair.getValue());
					JobErrorDetails.put(pair.getKey().toString(), pair.getValue().toString());
				}
				if (pair.getKey().equals("message")) {
					// System.out.println(pair.getKey() + " " + pair.getValue());

					JobErrorDetails.put(pair.getKey().toString(), pair.getValue().toString());
				}
				if (pair.getKey().equals("eventType")) {
					// System.out.println(pair.getKey() + " " + pair.getValue());
					JobErrorDetails.put(pair.getKey().toString(), pair.getValue().toString());
				}
				if (pair.getKey().equals("stackTrace")) {
					String temp;
					if (pair.getValue().toString().contains("ClassCastException")) {temp = "ClassCastException";}
					else
					{
						temp = (pair.getValue().toString()).substring(0, 30).replaceAll(",", "*").replaceAll(" ", "");
					}
					JobErrorDetails.put(pair.getKey().toString(), temp);
					System.out.println("@@@" + pair.getKey().toString() + "    " + temp);

				}

			}

			if (JobErrorDetails.get("eventType").equals("Failed")) {
				System.out.println(JobErrorDetails.values());
				if (JobErrorDetails.get("exceptionMessage") != null) {
					String tempmessage = JobErrorDetails.get("exceptionMessage").toString();
					if (tempmessage.contains("BigDecimal")) {
						tempmessage = tempmessage.substring(0, 30);
					}
					LOG_failmessage = tempmessage;
				} else {
					LOG_failmessage = (JobErrorDetails.get("stackTrace").toString());
				}

				LOG_faileddate = JobErrorDetails.get("time").toString();

			}

			JobErrorDetails.clear();
		}

	}

	public static void createCSVFileFromJobDetails(LinkedHashMap<String, ArrayList<String>> JobDetailsList)
			throws IOException {
		int serialno = 1;
		System.out.println("About to write details into CSV file");
		BufferedWriter br = new BufferedWriter(
				new FileWriter(new File("/Users/sathya/Desktop/Azure_Error_Details.csv")));
		br.write("S.No, Job Id, Job Name, Error Reason, Date, Workflow Id, Workflow Name \n");
		for (Map.Entry<String, ArrayList<String>> entry : JobDetailsList.entrySet()) {
			br.write(Integer.toString(serialno));
			br.write(",");
			String key = entry.getKey();
			ArrayList<String> value = entry.getValue();
			br.write(key);
			br.write(",");
			for (String allvals : value) {
				br.write(allvals);
				br.write(",");
			}
			br.write("\n");
			serialno++;
		}
		br.close();

	}

	public static void main(String[] args) throws ParseException, IOException {

		int offset = 2110;

		/* Using Login Password for Flex */
		client = ClientBuilder.newClient().register(new Authenticator("sathya", "password@123"));
		/* API Params to extract failed workflows */
		// String workflowRequest =
		// "https://flex.nrl.ooflex.net/api/workflows;status=Failed;createdFrom=20 Aug
		// 2018;createdTo=now;owner=sathya;offset=101";
		for (int i = 0; i < 1; i++) {
			String workflowRequest = "https://flex.nrl.ooflex.net/api/workflows;status=Failed;createdFrom=20 Aug 2018;createdTo=now;owner=sathya;offset="
					+ offset + ";limit=50";
			/* Extracting Workflows */
			JSONObject jsonobject = getJsonUsingFlexAPI(workflowRequest);
			/* Processing the jsonobject received */
			processWorkflowJson(jsonobject);
			/* Processing at a time */
			offset = offset + 50;
		}

	}

}
