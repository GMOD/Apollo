package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Anchor;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.shared.go.Aspect;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.bbop.apollo.gwt.shared.go.Reference;
import org.bbop.apollo.gwt.shared.go.WithOrFrom;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class GoRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static List<GoAnnotation> generateGoAnnotations(AnnotationInfo annotationInfo, JSONArray goAnnotations){
        List<GoAnnotation> goAnnotationList = new ArrayList<>();
        for(int i = 0 ; i < goAnnotations.size() ; i++){
            JSONObject goAnnotationObject = goAnnotations.get(i).isObject();
            GoAnnotation goAnnotation = new GoAnnotation();
            goAnnotation.setGene(annotationInfo.getUniqueName());
            goAnnotation.setAspect(Aspect.valueOf(goAnnotationObject.get("aspect").isString().stringValue()));
            goAnnotation.setGoTerm(goAnnotationObject.get("goTerm").isString().stringValue());
            goAnnotation.setGoTermLabel(goAnnotationObject.get("goTermLabel").isString().stringValue());
            goAnnotation.setGeneRelationship(goAnnotationObject.get("geneRelationship").isString().stringValue());
            goAnnotation.setEvidenceCode(goAnnotationObject.get("evidenceCode").isString().stringValue());
            goAnnotation.setEvidenceCodeLabel(goAnnotationObject.get("evidenceCodeLabel").isString().stringValue());
            goAnnotation.setNegate(goAnnotationObject.get("negate").isBoolean().booleanValue());


            if(goAnnotationObject.containsKey("reference")){
                String[] referenceString = goAnnotationObject.get("reference").isString().stringValue().split(":");
                Reference reference = new Reference(referenceString[0], referenceString[1]);
                goAnnotation.setReference(reference);
            }
            else{
                goAnnotation.setReference(Reference.createEmptyReference());
            }


            List<WithOrFrom> withOrFromList = new ArrayList<>();
            if(goAnnotationObject.containsKey("withOrFrom")) {
                JSONArray goWithOrFromArray = goAnnotationObject.get("withOrFrom").isArray();
                if (goWithOrFromArray == null) {
                    String goWithString = goAnnotationObject.get("withOrFrom").isString().stringValue();
                    goWithOrFromArray = JSONParser.parseStrict(goWithString).isArray();
                }
                for (int j = 0; j < goWithOrFromArray.size(); j++) {
                    WithOrFrom withOrFrom = new WithOrFrom(goWithOrFromArray.get(j).isString().stringValue());
                    withOrFromList.add(withOrFrom);
                }
            }
            else{
                String jsonString = Reference.UNKNOWN + ":" + Reference.NOT_PROVIDED;
                withOrFromList.add(new WithOrFrom(jsonString));

            }
            goAnnotation.setWithOrFromList(withOrFromList);

            
            List<String> notesList = new ArrayList<>();
            JSONArray notesJsonArray = goAnnotationObject.get("notes").isArray();
            if(notesJsonArray==null){
                String notes = goAnnotationObject.get("notes").isString().stringValue();
                notesJsonArray = JSONParser.parseStrict(notes).isArray();
            }
            for(int j = 0 ; j < notesJsonArray.size() ; j++){
                notesList.add(notesJsonArray.get(j).isString().stringValue());
            }
            goAnnotation.setNoteList(notesList);
            goAnnotationList.add(goAnnotation);
        }
        return goAnnotationList;
    }

    public static void saveGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/save", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void updateGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/update", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void deleteGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/delete", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void getGoAnnotation(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uniqueName",new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put("organism",new JSONString(organismInfo.getId()));
        RestService.sendRequest(requestCallback, "goAnnotation/", "data=" + jsonObject.toString());
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

        GoRestService.lookupTerm(requestCallback,TERM_LOOKUP_SERVER + evidenceCurie);
    }
}
