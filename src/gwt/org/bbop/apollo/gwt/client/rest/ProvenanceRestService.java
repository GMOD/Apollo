package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Anchor;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.ProvenanceConverter;
import org.bbop.apollo.gwt.shared.provenance.Reference;
import org.bbop.apollo.gwt.shared.provenance.WithOrFrom;
import org.bbop.apollo.gwt.shared.provenance.Provenance;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class ProvenanceRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static List<Provenance> generateProvenances(AnnotationInfo annotationInfo, JSONArray provenances){
        List<Provenance> provenanceList = new ArrayList<>();
        for(int i = 0 ; i < provenances.size() ; i++){
            JSONObject provenanceObject = provenances.get(i).isObject();

            Provenance provenance = new Provenance();
            provenance.setFeature(annotationInfo.getUniqueName());
            provenance.setField(provenanceObject.get("field").isString().stringValue());

            provenance.setEvidenceCode(provenanceObject.get("evidenceCode").isString().stringValue());
            provenance.setEvidenceCodeLabel(provenanceObject.get("evidenceCodeLabel").isString().stringValue());

            if(provenanceObject.containsKey("reference")){
                String[] referenceString = provenanceObject.get("reference").isString().stringValue().split(":");
                Reference reference = new Reference(referenceString[0], referenceString[1]);
                provenance.setReference(reference);
            }
            else{
                provenance.setReference(Reference.createEmptyReference());
            }

            List<WithOrFrom> withOrFromList = new ArrayList<>();

            if(provenanceObject.containsKey("withOrFrom")) {
                JSONArray goWithOrFromArray = provenanceObject.get("withOrFrom").isArray();
                if (goWithOrFromArray == null) {
                    String goWithString = provenanceObject.get("withOrFrom").isString().stringValue();
                    goWithOrFromArray = JSONParser.parseStrict(goWithString).isArray();
                }
                for (int j = 0; j < goWithOrFromArray.size(); j++) {
                    WithOrFrom withOrFrom = new WithOrFrom(goWithOrFromArray.get(j).isString().stringValue());
                    withOrFromList.add(withOrFrom);
                }
                provenance.setWithOrFromList(withOrFromList);
            }

            List<String> notesList = new ArrayList<>();
            JSONArray notesJsonArray = provenanceObject.get("notes").isArray();
            if(notesJsonArray==null){
                String notes = provenanceObject.get("notes").isString().stringValue();
                notesJsonArray = JSONParser.parseStrict(notes).isArray();
            }
            for(int j = 0 ; j < notesJsonArray.size() ; j++){
                notesList.add(notesJsonArray.get(j).isString().stringValue());
            }
            provenance.setNoteList(notesList);
            provenanceList.add(provenance);
        }
        return provenanceList;
    }

    public static void saveProvenance(RequestCallback requestCallback, Provenance provenance) {
        RestService.sendRequest(requestCallback, "provenance/save", "data=" + ProvenanceConverter.convertToJson(provenance).toString());
    }

    public static void updateProvenance(RequestCallback requestCallback, Provenance provenance) {
        RestService.sendRequest(requestCallback, "provenance/update", "data=" + ProvenanceConverter.convertToJson(provenance).toString());
    }

    public static void deleteProvenance(RequestCallback requestCallback, Provenance provenance) {
        RestService.sendRequest(requestCallback, "provenance/delete", "data=" + ProvenanceConverter.convertToJson(provenance).toString());
    }

    public static void getProvenance(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uniqueName",new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put("organism",new JSONString(organismInfo.getId()));
        RestService.sendRequest(requestCallback, "provenance/", "data=" + jsonObject.toString());
    }

    private static void lookupTerm(RequestCallback requestCallback, String url) {
        RestService.generateBuilder(requestCallback,RequestBuilder.GET,url);
    }

    public static void lookupTerm(final Anchor anchor, String evidenceCurie) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnObject = JSONParser.parseStrict(response.getText()).isObject();
                anchor.setHTML(returnObject.get("label").isString().stringValue());
                if(returnObject.containsKey("definition")){
                    anchor.setTitle(returnObject.get("definition").isString().stringValue());
                }
                else{
                    anchor.setTitle(returnObject.get("label").isString().stringValue());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to do lookup: "+exception.getMessage());
            }
        };

        ProvenanceRestService.lookupTerm(requestCallback,TERM_LOOKUP_SERVER + evidenceCurie);
    }
}
