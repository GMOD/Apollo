package org.bbop.apollo.web.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

public class AnnotationEditorServiceWithPersistentStorageTest extends TestCase {

	private static String sessionId;
	
	public AnnotationEditorServiceWithPersistentStorageTest() {
		try {
			sessionId = login();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testSetOrganism() throws IOException, JSONException {
		System.out.println("== testSetOrganism() ==");
		JSONObject request = createRequest("set_organism");
		JSONObject organism = new JSONObject();
		request.put("organism", organism);
		organism.put("genus", "Foomus");
		organism.put("species", "barius");
		sendRequestAndPrintResponse(request);

		request = createRequest("get_organism");
		JSONObject returnedOrganism = sendRequestAndPrintResponse(request);
		assertEquals("Organism genus: ", organism.getString("genus"), returnedOrganism.getString("genus"));
		assertEquals("Organism species: ", organism.getString("species"), returnedOrganism.getString("species"));
	}
	
	public void testSetSourceFeature() throws IOException, JSONException {
		System.out.println("== testSetSourceFeature() ==");
		String residues = setSourceFeature();
		
		JSONObject request = createRequest("get_source_feature");
		JSONObject srcFeature = sendRequestAndPrintResponse(request);
		assertEquals("Source feature residues: ", residues, srcFeature.getJSONArray("features").getJSONObject(0).getString("residues"));
	}
	
	public void testAddFeature() throws JSONException, IOException {
		deleteSequenceAlterations();
		System.out.println("== testAddFeature() ==");
		JSONObject request = createRequest("add_feature");
		JSONObject gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		request.put("features", new JSONArray().put(gene));

		sendRequestAndPrintResponse(request);
		JSONArray features = getFeatures();
		assertEquals("Number of features: ", new Integer(1), new Integer(features.length()));
		assertEquals("Gene uniquenames: ", gene.getString("uniquename"), features.getJSONObject(0).getString("uniquename"));
	}
	
	private JSONArray getFeatures() throws JSONException, IOException {
		JSONObject request = createRequest("get_features");
		JSONObject features = sendRequestAndPrintResponse(request);
		return features.getJSONArray("features");
	}

	private String login() throws IOException, JSONException {
		String username = "foo";
		String password = "bar";
		URL url = new URL("http://localhost:8080/ApolloWeb/Login");
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		out.write("username=" + username + "&password=" + password);
		out.flush();
		out.close();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return new JSONObject(in.readLine()).getString("session-id");
	}
	
	private URL getAnnotationEditorURL() throws MalformedURLException {
		return new URL("http://localhost:8080/ApolloWeb/AnnotationEditorService" + ";jsessionid=" + sessionId);
	}
	
	private HttpURLConnection openURLConnection() throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection)getAnnotationEditorURL().openConnection();
		connection.setDoOutput(true);
		return connection;
	}
	
	private JSONObject sendRequestAndPrintResponse(JSONObject request) throws IOException, JSONException {
		int indent = 2;
		System.out.println("Request for operation: " + request.getString("operation"));
		System.out.println(request.toString(indent));
		HttpURLConnection connection = openURLConnection();
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		out.write(request.toString());
		out.flush();
		out.close();
		boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
		String line;
		BufferedReader in;
		StringBuilder buffer = new StringBuilder();
		if (ok) {
			System.out.println("Response for operation: " + request.getString("operation"));
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		}
		else {
			System.out.println("Failure for operation: " + request.getString("operation") + " [" + connection.getResponseCode() + "]" + " " + connection.getResponseMessage());
			in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		}
		while ((line = in.readLine()) != null) {
			buffer.append(line);
		}
		if (buffer.length() > 0) {
			JSONObject response = new JSONObject(buffer.toString());
			System.out.println(response.toString(indent));
			return response;
		}
		return null;
	}
	
	private JSONObject createJSONFeature(int fmin, int fmax, int strand, String cvterm, String name) throws JSONException {
		JSONObject feature = new JSONObject();
		JSONObject type = new JSONObject();
		feature.put("type", type);
		JSONObject cv = new JSONObject();
		type.put("cv", cv);
		cv.put("name", "SO");
		type.put("name", cvterm);
		feature.put("uniquename", name);
		JSONObject location = new JSONObject();
		feature.put("location", location);
		location.put("fmin", fmin);
		location.put("fmax", fmax);
		location.put("strand", strand);
		return feature;
	}
	
	private JSONObject createJSONFeature(int fmin, int fmax, int strand, String cvterm, String name, String residues) throws JSONException {
		JSONObject feature = createJSONFeature(fmin, fmax, strand, cvterm, name);
		feature.put("residues", residues);
		return feature;
	}
		
	private JSONObject createJSONFeature(String cvterm, String name, String residues) throws JSONException {
		JSONObject feature = createJSONFeature(0, 0, 0, cvterm, name);
		feature.remove("location");
		feature.put("residues", residues);
		return feature;
	}

	private JSONObject createJSONFeatureProperty(String cvterm, String value) throws JSONException {
		JSONObject property = new JSONObject();
		JSONObject type = new JSONObject();
		property.put("type", type);
		JSONObject cv = new JSONObject();
		type.put("cv", cv);
		cv.put("name", "SO");
		type.put("name", cvterm);
		property.put("value", value);
		return property;
	}
	
	private JSONObject createRequest(String operation) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("operation", operation);
		request.put("track", "testTrackWithStore");
		return request;
	}
	
	private JSONObject deleteFeature(String uniqueName) throws JSONException, IOException {
		JSONObject request = createRequest("delete_feature");
		JSONObject feature = new JSONObject();
		request.put("features", new JSONArray().put(feature));
		feature.put("uniquename", uniqueName);
		return sendRequestAndPrintResponse(request);
	}
	
	private JSONObject deleteSequenceAlterations() throws JSONException, IOException {
		JSONObject request = createRequest("get_sequence_alterations");
		JSONObject response = sendRequestAndPrintResponse(request);
		request = createRequest("delete_sequence_alteration");
		request.put("features", response.getJSONArray("features"));
		return sendRequestAndPrintResponse(request);
	}
	
	private JSONObject getCDS(JSONObject transcript) throws JSONException {
		JSONArray transcriptChildren = transcript.getJSONArray("children");
		for (int i = 0; i < transcriptChildren.length(); ++i) {
			JSONObject child = transcriptChildren.getJSONObject(i);
			if (child.getJSONObject("type").getString("name").equals("CDS")) {
				return child;
			}
		}
		return null;
	}
	
	private String setSourceFeature() throws IOException, JSONException {
		String residues = "ATATCTTTTCTCACAATCGTTGCAGAGGACTTGTATGCACTTTAGACGTGGAAGAAGAATCGCGAGACTTTTCGGTGTGTCGGAGAGAGTGTTCATCGATCGTGCCTTTTGGCGTTAGGCGCGATTATGTCGCATGCTGTGTGAGTGTGTCTCTCTCGGCCACTGTAGGATGTTCGTGTCGATGCGAGTTTGTGAAATGCTAGGTCAATGTTGCTTGGTTCACAGTTTCGTACCTAGTTTGCGATCTGTCTGATTCGTTGTGTGGATGTAGAGAGCCTCTGGTGCGTAATGTAGGCTGGTTGGGACACATTTGACTTGTTCTGTAGGGTCGATCTGTCGATGGGGCTCTGGGTTTCCGATGTTTCTCCGTAGAGGGAGTTCTTCTGTATGTTTGTTTGTGGTTAGATCTGCTTTTGGAATCAATGAGCATTGCGAAGACTATTTCTCCTGCTAAAGAACTTTTCGATCGTCAAATAATTGTGATGATCTTTCTACCATTAAAGATTCCATTAGAGTCTTTGTTTGCCATGCATGTTTGGGATCGAGGATATTTGAAGCATACAGTGGTCTAATGTAGCGATTTCTTAACAACAATGCTTCCATTTCTTTTGCAGAGTATATAGAAATCATCGACAATCATGAATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGTAATGAAATGTTTTTTTTGCGATTTTAAACACTGTTTTCCGTGGATTGATGATGAACGATCCTCAAAGGTCTTTTTTATTTTCGGCACGGAACCCCTCGCAATCTTTGGTGTAGCTCATTTGTTCTCGCTAACGATCCATCTTCTGATGGTGTAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCGTGAGTCTACCATGCTATATCTTCTTGCAAATTATTTCGAAGTTTAGGCAGCTTGTTGAANTCTGCTAAATTGGTGGTGCTTCATGGTTTTGGAAAATCGGTGATTCCGCTGTTGTGGTGCTCATTCCTTCGTGTTAACTTGATTGATGGTAATGTGGTGTCGATGCAGTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAGAGAAGGAAGCGTAGTAATCTATCATGTAAATGGAGATTTGGTTTCTTGCAGAAACACCTGGTGTAATTTTTGTTTCATGAAATGTGAATATTTGAGTTTTTGTTAAT";
		JSONObject request = createRequest("set_source_feature");
		request.put("features", new JSONArray().put(createJSONFeature("chromosome", "chromosome", residues)));
		sendRequestAndPrintResponse(request);
		return residues;
	}
	
}
