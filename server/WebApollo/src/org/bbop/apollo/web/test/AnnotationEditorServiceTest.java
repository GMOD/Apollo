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

public class AnnotationEditorServiceTest extends TestCase {

	private String sessionId;
	
	public AnnotationEditorServiceTest() {
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
	
	public void testAddTranscript() throws JSONException, IOException {
		System.out.println("== testAddTranscript() ==");
		JSONObject request = createRequest("add_transcript");
		JSONObject gene = new JSONObject().put("uniquename", "gene");
		JSONObject transcript = createJSONFeature(638, 2628, 1, "transcript", "transcript");
		request.put("features", new JSONArray().put(gene).put(transcript));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONArray features = getFeatures();
		assertEquals("Number of features: ", new Integer(1), new Integer(features.length()));
		JSONObject retrievedTranscript = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0);
		assertEquals("Transcript uniquenames: ", transcript.getString("uniquename"), retrievedTranscript.getString("uniquename"));
	}
	
	public void testDuplicateTranscript() throws JSONException, IOException {
		System.out.println("== testDuplicateTranscript() ==");
		JSONObject request = createRequest("duplicate_transcript");
		JSONObject transcript = new JSONObject();
		transcript.put("uniquename", "transcript");
		request.put("features", new JSONArray().put(transcript));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONArray transcripts = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children");
		assertEquals("Number of transcripts: ", new Integer(2), new Integer(transcripts.length()));
	}
	
	public void testDeleteFeature() throws JSONException, IOException {
		System.out.println("== testDeleteFeature() ==");
		JSONObject responseFeatures = deleteFeature("gene");
		JSONArray features = getFeatures();
		assertEquals("Number of features (response features): ", new Integer(0), new Integer(responseFeatures.getJSONArray("features").length()));
		assertEquals("Number of features (get_features): ", new Integer(0), new Integer(features.length()));
	}
	
	public void testMergeTranscriptWithGeneMerge() throws JSONException, IOException {
		System.out.println("== testMergeTranscriptsWithGeneMerge() ==");
		JSONObject request = createRequest("add_feature");
		JSONObject gene1 = createJSONFeature(0, 1500, 1, "gene", "gene1");
		JSONObject gene2 = createJSONFeature(2000, 3500, 1, "gene", "gene2");
		JSONArray features = new JSONArray();
		request.put("features", features);
		features.put(gene1);
		features.put(gene2);
		JSONObject transcript1 = createJSONFeature(0, 1500, 1, "transcript", "transcript1");
		JSONObject transcript2 = createJSONFeature(2000, 2700, 1, "transcript", "transcript2");
		JSONObject transcript3 = createJSONFeature(3000, 3500, 1, "transcript", "transcript3");
		gene1.put("children", new JSONArray().put(transcript1));
		gene2.put("children", new JSONArray().put(transcript2).put(transcript3));
		JSONArray exons1 = new JSONArray();
		transcript1.put("children", exons1);
		exons1.put(createJSONFeature(0, 700, 1, "exon", "exon1_1"));
		exons1.put(createJSONFeature(1000, 1500, 1, "exon", "exon1_2"));
		JSONArray exons2 = new JSONArray();
		transcript2.put("children", exons2);
		exons2.put(createJSONFeature(2000, 2400, 1, "exon", "exon2_1"));
		exons2.put(createJSONFeature(2500, 2700, 1, "exon", "exon2_2"));
		JSONArray exons3 = new JSONArray();
		transcript3.put("children", exons3);
		exons3.put(createJSONFeature(3000, 3500, 1, "exon", "exon3_1"));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("merge_transcripts");
		JSONArray mergeTranscripts = new JSONArray();
		request.put("features", mergeTranscripts);
		mergeTranscripts.put(new JSONObject().put("uniquename", "transcript1"));
		mergeTranscripts.put(new JSONObject().put("uniquename", "transcript2"));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject gene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONObject geneLoc = gene.getJSONObject("location");
		assertEquals("num transcripts gene1 (after-merge): ", new Integer(2), new Integer(gene.getJSONArray("children").length()));
		JSONObject transcript = gene.getJSONArray("children").getJSONObject(0);
		assertEquals("num exons transcript1 (after-merge): ", new Integer(4), new Integer(transcript.getJSONArray("children").length()));
		assertEquals("gene1 fmin (after-merge): ", new Integer(0), new Integer(geneLoc.getInt("fmin")));
		assertEquals("gene1 fmax (after-merge): ", new Integer(3500), new Integer(geneLoc.getInt("fmax")));

		deleteFeature("gene1");
	}
	
	public void testMergeTranscripts() throws JSONException, IOException {
		System.out.println("== testMergeTranscripts() ==");
		JSONObject request = createRequest("add_feature");
		JSONObject gene = createJSONFeature(0, 1500, 1, "gene", "gene");
		request.put("features", new JSONArray().put(gene));
		JSONObject transcript1 = createJSONFeature(100, 1000, 1, "transcript", "transcript1");
		JSONObject transcript2 = createJSONFeature(500, 1500, 1, "transcript", "transcript2");
		JSONArray transcripts = new JSONArray();
		gene.put("children", transcripts);
		transcripts.put(transcript1);
		transcripts.put(transcript2);
		JSONArray exons1 = new JSONArray();
		transcript1.put("children", exons1);
		exons1.put(createJSONFeature(100, 200, 1, "exon", "exon1_1"));
		exons1.put(createJSONFeature(800, 900, 1, "exon", "exon1_2"));
		JSONArray exons2 = new JSONArray();
		transcript2.put("children", exons2);
		exons2.put(createJSONFeature(850, 1000, 1, "exon", "exon2_2"));
		exons2.put(createJSONFeature(500, 700, 1, "exon", "exon2_1"));
		exons2.put(createJSONFeature(1200, 1400, 1, "exon", "exon2_3"));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("Number of transcripts pre merge: ", new Integer(2), new Integer(responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").length()));
		
		request = createRequest("merge_transcripts");
		JSONArray mergeTranscripts = new JSONArray();
		request.put("features", mergeTranscripts);
		mergeTranscripts.put(new JSONObject().put("uniquename", "transcript1"));
		mergeTranscripts.put(new JSONObject().put("uniquename", "transcript2"));
		responseFeatures = sendRequestAndPrintResponse(request);
		
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		assertEquals("Number of transcripts: ", new Integer(1), new Integer(responseGene.getJSONArray("children").length()));
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		assertEquals("Number of exons: ", new Integer(4), new Integer(responseTranscript.getJSONArray("children").length()));
		assertEquals("Merged transcript start: ", new Integer(100), new Integer(responseTranscript.getJSONObject("location").getInt("fmin")));
		assertEquals("Merged transcript end: ", new Integer(1400), new Integer(responseTranscript.getJSONObject("location").getInt("fmax")));
	}

	public void testSetTranslationStart() throws JSONException, IOException {
		System.out.println("== testSetTranslationStart() ==");
		JSONObject request = createRequest("set_translation_start");
		JSONObject transcript = new JSONObject();
		request.put("features", new JSONArray().put(transcript));
		transcript.put("uniquename", "transcript1");
		JSONObject cds = createJSONFeature(100, 1400, 1, "CDS", "cds");
		JSONArray children = new JSONArray();
		transcript.put("children", children);
		children.put(cds);
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);

		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		assertEquals("Number of transcripts: ", new Integer(1), new Integer(responseGene.getJSONArray("children").length()));
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		JSONObject responseCDS = getCDS(responseTranscript);
		JSONObject responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("Translation start: ", new Integer(100), new Integer(responseCDSLocation.getInt("fmin")));
	}

	public void testSetTranslationEnd() throws JSONException, IOException {
		System.out.println("== testSetTranslationEnd() ==");
		JSONObject request = createRequest("set_translation_end");
		JSONObject transcript = new JSONObject();
		request.put("features", new JSONArray().put(transcript));
		transcript.put("uniquename", "transcript1");
		JSONObject cds = createJSONFeature(100, 1500, 1, "CDS", "cds");
		JSONArray children = new JSONArray();
		transcript.put("children", children);
		children.put(cds);
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);

		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		assertEquals("Number of transcripts: ", new Integer(1), new Integer(responseGene.getJSONArray("children").length()));
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		JSONObject responseCDS = getCDS(responseTranscript);
		JSONObject responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("Translation start: ", new Integer(1500), new Integer(responseCDSLocation.getInt("fmax")));
	}
	
	public void testSetTranslationEnds() throws JSONException, IOException {
		System.out.println("== testSetTranslationEnds() ==");
		JSONObject request = createRequest("set_translation_ends");
		JSONObject transcript = new JSONObject();
		request.put("features", new JSONArray().put(transcript));
		transcript.put("uniquename", "transcript1");
		JSONObject cds = createJSONFeature(200, 1400, 1, "CDS", "cds");
		JSONArray children = new JSONArray();
		transcript.put("children", children);
		children.put(cds);
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);

		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		assertEquals("Number of transcripts: ", new Integer(1), new Integer(responseGene.getJSONArray("children").length()));
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		JSONObject responseCDS = getCDS(responseTranscript);
		JSONObject responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("Translation start: ", new Integer(200), new Integer(responseCDSLocation.getInt("fmin")));
		assertEquals("Translation end: ", new Integer(1400), new Integer(responseCDSLocation.getInt("fmax")));
	}
	
	public void testSetLongestORF() throws JSONException, IOException {
		System.out.println("== testSetLongestORF() ==");
		deleteFeature("gene");
		JSONObject gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		JSONObject transcript = createJSONFeature(638, 2628, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		JSONArray exons = new JSONArray();
		transcript.put("children", exons);
		exons.put(createJSONFeature(638, 693, 1, "exon", "exon1"));
		exons.put(createJSONFeature(849, 2223, 1, "exon", "exon2"));
		exons.put(createJSONFeature(2392, 2628, 1, "exon", "exon3"));
		JSONObject request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		
		JSONObject responseTranscript = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0);
		JSONObject responseCDS = getCDS(responseTranscript);
		JSONObject responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("CDS_1 fmin: ", new Integer(638), new Integer(responseCDSLocation.getInt("fmin")));
		assertEquals("CDS_1 fmax: ", new Integer(2628), new Integer(responseCDSLocation.getInt("fmax")));
		
		deleteFeature("gene");
		gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		transcript = createJSONFeature(638, 2626, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		exons = new JSONArray();
		transcript.put("children", exons);
		exons.put(createJSONFeature(638, 693, 1, "exon", "exon1"));
		exons.put(createJSONFeature(849, 2223, 1, "exon", "exon2"));
		exons.put(createJSONFeature(2392, 2626, 1, "exon", "exon3"));
		request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		
		responseTranscript = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);
		responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("CDS_2 fmin: ", new Integer(638), new Integer(responseCDSLocation.getInt("fmin")));
		assertEquals("CDS_2 fmax: ", new Integer(2626), new Integer(responseCDSLocation.getInt("fmax")));
		assertTrue("CDS_2 fmax (partial): ", responseCDSLocation.getBoolean("is_fmax_partial"));
		
		request = createRequest("get_source_feature");
		responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject sourceFeature = responseFeatures.getJSONArray("features").getJSONObject(0);
		request = createRequest("set_source_feature");
		sourceFeature.put("residues", sourceFeature.getString("residues").replaceAll("ATG", "AGG"));
		request.put("features", new JSONArray().put(sourceFeature));
		sendRequestAndPrintResponse(request);
		
		deleteFeature("gene");
		gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		transcript = createJSONFeature(638, 2626, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		exons = new JSONArray();
		transcript.put("children", exons);
		exons.put(createJSONFeature(638, 693, 1, "exon", "exon1"));
		exons.put(createJSONFeature(849, 2223, 1, "exon", "exon2"));
		exons.put(createJSONFeature(2392, 2626, 1, "exon", "exon3"));
		request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);

		responseTranscript = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);
		responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("CDS_3 fmin: ", new Integer(638), new Integer(responseCDSLocation.getInt("fmin")));
		assertTrue("CDS_3 fmin (partial): ", responseCDSLocation.getBoolean("is_fmin_partial"));
		assertEquals("CDS_3 fmax: ", new Integer(962), new Integer(responseCDSLocation.getInt("fmax")));

		request = createRequest("get_source_feature");
		responseFeatures = sendRequestAndPrintResponse(request);
		sourceFeature = responseFeatures.getJSONArray("features").getJSONObject(0);
		request = createRequest("set_source_feature");
		sourceFeature.put("residues", sourceFeature.getString("residues").replaceAll("TAA", "CCC"));
		sourceFeature.put("residues", sourceFeature.getString("residues").replaceAll("TAG", "CCC"));
		sourceFeature.put("residues", sourceFeature.getString("residues").replaceAll("TGA", "CCC"));
		request.put("features", new JSONArray().put(sourceFeature));
		sendRequestAndPrintResponse(request);

		deleteFeature("gene");
		gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		transcript = createJSONFeature(638, 2626, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		exons = new JSONArray();
		transcript.put("children", exons);
		exons.put(createJSONFeature(638, 693, 1, "exon", "exon1"));
		exons.put(createJSONFeature(849, 2223, 1, "exon", "exon2"));
		exons.put(createJSONFeature(2392, 2626, 1, "exon", "exon3"));
		request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);

		responseTranscript = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);
		responseCDSLocation = responseCDS.getJSONObject("location");
		assertEquals("CDS_4 fmin: ", new Integer(638), new Integer(responseCDSLocation.getInt("fmin")));
		assertTrue("CDS_4 fmin (partial): ", responseCDSLocation.getBoolean("is_fmin_partial"));
		assertEquals("CDS_4 fmax: ", new Integer(2626), new Integer(responseCDSLocation.getInt("fmax")));
		assertTrue("CDS_4 fmax (partial): ", responseCDSLocation.getBoolean("is_fmax_partial"));
	}
	
	public void testAddExon() throws IOException, JSONException {
		System.out.println("== testAddExon() ==");
		setSourceFeature();

		deleteFeature("gene");
		JSONObject gene = createJSONFeature(100, 1000, 1, "gene", "gene");
		JSONObject transcript = createJSONFeature(100, 1000, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		JSONObject request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);

//		JSONArray exons = new JSONArray();
//		transcript.put("children", exons);
		JSONObject exon1 = createJSONFeature(100, 200, 1, "exon", "exon1");
		JSONObject exon2 = createJSONFeature(400, 600, 1, "exon", "exon2");
		JSONObject exon3 = createJSONFeature(500, 1000, 1, "exon", "exon3");
		request = createRequest("add_exon");
		request.put("features", new JSONArray().put(transcript).put(exon1).put(exon2).put(exon3));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		assertEquals("transcript num exons: ", new Integer(2), new Integer(responseTranscript.getJSONArray("children").length()));
	}
	
	public void testDeleteExon() throws JSONException, IOException {
		System.out.println("== testDeleteExon() ==");
		JSONArray requestFeatures = getFeatures();
		JSONObject responseGene = requestFeatures.getJSONObject(0);
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		assertEquals("transcript num exons (before delete): ", new Integer(2), new Integer(responseTranscript.getJSONArray("children").length()));
		
		JSONObject request = createRequest("delete_exon");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")).put(new JSONObject().put("uniquename", "exon1")));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		assertEquals("transcript num exons (after delete): ", new Integer(1), new Integer(responseTranscript.getJSONArray("children").length()));
	}
	
	public void testMergeExons() throws JSONException, IOException {
		System.out.println("== testMergeExons() ==");
		
		deleteFeature("gene");
		JSONObject gene = createJSONFeature(100, 1000, 1, "gene", "gene");
		JSONObject transcript = createJSONFeature(100, 1000, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		transcript.put("children", new JSONArray().put(createJSONFeature(100, 200, 1, "exon", "exon1")).put(createJSONFeature(400, 500, 1, "exon", "exon2")));
		JSONObject request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("merge_exons");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "exon1")).put(new JSONObject().put("uniquename", "exon2")));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		assertEquals("transcript num exons (after merge): ", new Integer(1), new Integer(responseTranscript.getJSONArray("children").length()));
		JSONObject responseExon = responseTranscript.getJSONArray("children").getJSONObject(0);
		assertEquals("exon fmin (after merge [exon1, exon2]): ", new Integer(100), new Integer(responseExon.getJSONObject("location").getInt("fmin")));
		assertEquals("exon fmax (after merge [exon1, exon2]): ", new Integer(500), new Integer(responseExon.getJSONObject("location").getInt("fmax")));
	}
	
	public void testSplitExon() throws JSONException, IOException {
		System.out.println("== testSplitExon ==");

		int newLeftMax = 200;
		int newRightMin = 300;
		JSONObject request = createRequest("split_exon");
		JSONArray features = new JSONArray();
		request.put("features", features);
		features.put(new JSONObject().put("uniquename", "exon1").put("location", new JSONObject().put("fmax", newLeftMax).put("fmin", newRightMin)));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		JSONArray responseExons = responseTranscript.getJSONArray("children");
		assertEquals("Number of exons: ", new Integer(2), new Integer(responseExons.length()));
		JSONObject responseExon1 = responseExons.getJSONObject(0);
		JSONObject responseExon2 = responseExons.getJSONObject(1);
		if (responseExon1.getJSONObject("location").getInt("fmin") > responseExon2.getJSONObject("location").getInt("fmin")) {
			JSONObject tmp = responseExon1;
			responseExon1 = responseExon2;
			responseExon2 = tmp;
		}
		assertEquals("Split left exon fmin: ", new Integer(100), new Integer(responseExon1.getJSONObject("location").getInt("fmin")));
		assertEquals("Split left exon fmax: ", new Integer(200), new Integer(responseExon1.getJSONObject("location").getInt("fmax")));
		assertEquals("Split right exon fmin: ", new Integer(300), new Integer(responseExon2.getJSONObject("location").getInt("fmin")));
		assertEquals("Split right exon fmax: ", new Integer(500), new Integer(responseExon2.getJSONObject("location").getInt("fmax")));
	}
	
	public void testSplitTranscript() throws JSONException, IOException {
		System.out.println("== testSplitTranscript() ==");

		JSONObject request = createRequest("split_transcript");
		JSONArray features = new JSONArray();
		request.put("features", features);
		features.put(new JSONObject().put("uniquename", "exon1-left"));
		features.put(new JSONObject().put("uniquename", "exon1-right"));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONArray responseTranscripts = responseGene.getJSONArray("children");
		assertEquals("Number of transcripts: ", new Integer(2), new Integer(responseTranscripts.length()));
		JSONObject responseTranscript1 = responseTranscripts.getJSONObject(0);
		JSONObject responseTranscript2 = responseTranscripts.getJSONObject(1);
		if (responseTranscript1.getJSONObject("location").getInt("fmin") > responseTranscript2.getJSONObject("location").getInt("fmin")) {
			JSONObject tmp = responseTranscript1;
			responseTranscript1 = responseTranscript2;
			responseTranscript2 = tmp;
		}
		assertEquals("Split left transcript fmin: ", new Integer(100), new Integer(responseTranscript1.getJSONObject("location").getInt("fmin")));
		assertEquals("Split left transcript fmax: ", new Integer(200), new Integer(responseTranscript1.getJSONObject("location").getInt("fmax")));
		assertEquals("Split right transcript fmin: ", new Integer(300), new Integer(responseTranscript2.getJSONObject("location").getInt("fmin")));
		assertEquals("Split right transcript fmax: ", new Integer(1000), new Integer(responseTranscript2.getJSONObject("location").getInt("fmax")));

	}
	
	public void testAddSequenceAlteration() throws JSONException, IOException {
		System.out.println("== testAddSequenceAlteration() ==");
		deleteFeature("gene");
		JSONObject gene = createJSONFeature(0, 2735, 1, "gene", "gene");
		JSONObject transcript = createJSONFeature(638, 2628, 1, "transcript", "transcript");
		gene.put("children", new JSONArray().put(transcript));
		JSONArray exons = new JSONArray();
		transcript.put("children", exons);
		exons.put(createJSONFeature(638, 693, 1, "exon", "exon1"));
		exons.put(createJSONFeature(849, 2223, 1, "exon", "exon2"));
		exons.put(createJSONFeature(2392, 2628, 1, "exon", "exon3"));
		JSONObject request = createRequest("add_feature");
		request.put("features", new JSONArray().put(gene));
		sendRequestAndPrintResponse(request);
		
		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		JSONObject responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		JSONObject responseCDS = getCDS(responseTranscript);
		assertEquals("CDS fmin: ", new Integer(638), new Integer(responseCDS.getJSONObject("location").getInt("fmin")));
		assertEquals("CDS fmax: ", new Integer(2628), new Integer(responseCDS.getJSONObject("location").getInt("fmax")));

		request = createRequest("get_residues_with_alterations");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("CDS sequence: ", "ATGAATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", responseFeatures.getJSONArray("features").getJSONObject(0).getString("residues"));
		
		request = createRequest("add_sequence_alteration");
		request.put("features", new JSONArray().put(createJSONFeature(641, 642, 1, "substitution", "substitution1", "T")));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);
		assertEquals("CDS fmin: ", new Integer(638), new Integer(responseCDS.getJSONObject("location").getInt("fmin")));
		assertEquals("CDS fmax: ", new Integer(2628), new Integer(responseCDS.getJSONObject("location").getInt("fmax")));

		request = createRequest("get_residues_with_alterations");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript-CDS")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("CDS sequence: ", "ATGTATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", responseFeatures.getJSONArray("features").getJSONObject(0).getString("residues"));

		request = createRequest("add_sequence_alteration");
		request.put("features", new JSONArray().put(createJSONFeature(700, 701, 1, "substitution", "substitution2", "T")));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);

		request = createRequest("get_residues_with_alterations");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript-CDS")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("CDS sequence: ", "ATGTATCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", responseFeatures.getJSONArray("features").getJSONObject(0).getString("residues"));

		request = createRequest("add_sequence_alteration");
		request.put("features", new JSONArray().put(createJSONFeature(644, 701, 1, "insertion", "insertion1", "ATT")));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);

		request = createRequest("get_residues_with_alterations");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript-CDS")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("CDS sequence: ", "ATGTATATTCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", responseFeatures.getJSONArray("features").getJSONObject(0).getString("residues"));

		request = createRequest("add_sequence_alteration");
		request.put("features", new JSONArray().put(createJSONFeature(2300, 2301, 1, "insertion", "insertion2", "CCC")));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);

		request = createRequest("get_residues_with_alterations");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript-CDS")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("CDS sequence: ", "ATGTATATTCAGTACGGAAGAGAACAGCAAGATACTGGCCTCGTCGGCTCTGGTACAGGACATCGCGATGAATACGGCAATCCCAGGCAAGAGGGTATAATGGACAAGGTGAAAAATGCCGTAGGCATGGGCCCCAGTTCAGGAACCGGCTACAACAATCAGCCTGGTTATGACAATTACGGTAACCCAAGGCAAGAAGGATTAGTAGACAAGGCGAAGGACGCCGTGGGCATGGGTCCGAGTTTAGGAACTGGCTACAATAACCAGCCTGGTTATGACAGTTACGGGAATCGTGAGGGCATTGTGGACAGGGCGAAAGATGCGGTAGGGATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGGTACGACAATTACGGTGACCGAAGGCATGAAGGATTGGCAGACAGAGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACCACCAGCCTGGATATGACAACTACGGCAATCGTGAGGGCGTTGTGGACAAGGCGAAGGATGCGGTAGGCATGGGTCCGAATTCAGGAACTGGCTACAACAACCAGCCTGGTTATGACAGTTATGGTACCCGGAGACAGGAAGGATTGGTAGATAGAGCAAAGGATGCCGTCGGCATGGGCCCCAATTCGGGCACCGGCTATAACAATCAGCCCGGATATGACAACTACGGTAACCCAAGACGCGAAGGAGTGGTAGACAGGGCGAAGGATGCTGTAGGTATGGGGCCTAACTCAGGTTACAACAATCAGCCCGGATATGACAACTACGGCAATCGTGAGGGCATTGTGGACAAAGCCAAGGATGCAGTCGGTGTTGGCCCCCACTCGGGTACTGGCTACCACAACCAGCCCAGCTACGACAACTATGGCAACCCTAGGCAAGAGGGAATCGTGGATAGAGCGAAAGACGCTGTGGGGATGGGACCAAACTCTGGAACTGGCTACAACAACCAGTCTGATTATGACAGTTATGGCAACCCAAGGCACGAAGGCATGCTTGACAAGGCGAAGGATGACTTTGATATGGGCCCCAATTCCGGCACTGGCTATAACAACCGGCCCGGCTATGACACCTATGGGGACCGAAAACACGAGGGAATTGGTGACAAGGTGAGGGACGCAATCGGTACTGGCCCAAACTCCGGATATGACAGCCGCACACCCACCGGAACCGACGCTTACGTGCATGGCAACCATCCCCCTGGTATGCAAGACAGAATCACTGGCGTGAACGAGCCCTCGATCTTAGGTGGACGTGAGAATGTAGACCGCCATGGTTTTGGACACGATGGTCGCCAACATCACGGTCTGCTAGATAATGTTACTCTTCAAAGTGGCCATATTCCTGAGACTATGGTAGGCGGGCGCCGTGTTGAACCTGGATATGATATGACCAAGAGTGCTGGACATCATCTTACTGATCTTGGCCATCACGGTAACGATAGCGGTGTCACTGGATTGGGCCATCACGACACTGATTACGATGAGAGGAGGGGAAAAGGATTTGAAGACCCGATTGATAACAAAACCGGACTTGGATCAGACTACGATACGACCGAGACCGGATCTGGTTATGGTGCCACCGATACTGGTGCTGCACCTCACAAGAAGGGAATCATAACTAAGATCAAGGAGAAGCTGCACCACTAG", responseFeatures.getJSONArray("features").getJSONObject(0).getString("residues"));
		
		request = createRequest("add_sequence_alteration");
		request.put("features", new JSONArray().put(createJSONFeature(638, 639, 1, "substitution", "substitution3", "C")));
		sendRequestAndPrintResponse(request);

		request = createRequest("set_longest_orf");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")));
		responseFeatures = sendRequestAndPrintResponse(request);
		responseGene = responseFeatures.getJSONArray("features").getJSONObject(0);
		responseTranscript = responseGene.getJSONArray("children").getJSONObject(0);
		responseCDS = getCDS(responseTranscript);
		assertEquals("CDS fmin: ", new Integer(890), new Integer(responseCDS.getJSONObject("location").getInt("fmin")));
		assertEquals("CDS fmax: ", new Integer(2628), new Integer(responseCDS.getJSONObject("location").getInt("fmax")));
	}
	
	public void testGetSequenceAlterations() throws JSONException, IOException {
		System.out.println("== testGetSequenceAlterations() ==");
		JSONObject request = createRequest("get_sequence_alterations");
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("Number of sequence alterations: ", new Integer(5), new Integer(responseFeatures.getJSONArray("features").length()));
	}
	
	public void testDeleteSequenceAlteration() throws JSONException, IOException {
		System.out.println("== testDeleteSequenceAlteration() ==");
		JSONObject request = createRequest("delete_sequence_alteration");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "substitution1")).put(new JSONObject().put("uniquename", "substitution2")));
		sendRequestAndPrintResponse(request);
		
		JSONObject responseFeatures = sendRequestAndPrintResponse(createRequest("get_sequence_alterations"));
		assertEquals("Number of sequence alterations: ", new Integer(3), new Integer(responseFeatures.getJSONArray("features").length()));
	}
	
	public void testAddFrameshift() throws JSONException, IOException {
		System.out.println("== testAddFrameshift() ==");
		JSONObject request = createRequest("add_frameshift");
		JSONObject transcript = new JSONObject().put("uniquename", "transcript");
		request.put("features", new JSONArray().put(transcript));
		JSONArray properties = new JSONArray();
		transcript.put("properties", properties);
		properties.put(createJSONFeatureProperty("plus_1_frameshift", "100"));
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		JSONObject frameshift = responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0).getJSONArray("properties").getJSONObject(0);
		assertEquals("Frameshift type: ", "plus_1_frameshift", frameshift.getJSONObject("type").getString("name"));
		assertEquals("Frameshift location: ", new Integer(100), new Integer(frameshift.getInt("value")));
	}

	public void testDeleteFrameshift() throws JSONException, IOException {
		System.out.println("== testDeleteFrameshift() ==");
		JSONObject request = createRequest("delete_frameshift");
		JSONObject transcript = new JSONObject().put("uniquename", "transcript");
		request.put("features", new JSONArray().put(transcript));
		JSONArray properties = new JSONArray();
		transcript.put("properties", properties);
		properties.put(createJSONFeatureProperty("plus_1_frameshift", "100"));
		sendRequestAndPrintResponse(request);
		
		JSONObject responseFeatures = sendRequestAndPrintResponse(createRequest("get_features"));
		assertFalse("Frameshift exists: ", responseFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("children").getJSONObject(0).has("properties"));
	}
	
	public void testSetExonBoundaries() throws JSONException, IOException {
		System.out.println("== testSetFeatureBoundaries() ==");
		JSONObject request = createRequest("set_feature_boundaries");
		//TODO
	}

	/*
	public void testErrors() throws JSONException, IOException {
		System.out.println("== testErrors() ==");
		JSONObject request = createRequest("delete_feature");
		JSONObject responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("Missing \"features\" in JSON: ", responseFeatures.getString("error"), "JSONObject[\"features\"] not found.");
		
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "foo")));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("Feature not found: ", responseFeatures.getString("error"), "Feature with unique name foo not found");
		
		request = createRequest("add_exon");
		JSONObject wrongExon = createJSONFeature(0, 100, -1, "exon", "wrong_exon");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "transcript")).put(wrongExon));
		responseFeatures = sendRequestAndPrintResponse(request);
		request = createRequest("merge_exons");
		request.put("features", new JSONArray().put(new JSONObject().put("uniquename", "exon1")).put(wrongExon));
		responseFeatures = sendRequestAndPrintResponse(request);
		assertEquals("Different strands for exon merge: ", responseFeatures.getString("error"), "mergeExons(): Exons must be in the same strand");
	}
	*/
	
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
		request.put("track", "testTrack");
		request.put("update_datastore", false);
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
