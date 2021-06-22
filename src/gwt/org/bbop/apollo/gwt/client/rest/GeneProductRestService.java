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
import org.bbop.apollo.gwt.client.dto.GeneProductConverter;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.shared.geneProduct.GeneProduct;
import org.bbop.apollo.gwt.shared.geneProduct.Reference;
import org.bbop.apollo.gwt.shared.geneProduct.WithOrFrom;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class GeneProductRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static List<GeneProduct> generateGeneProducts(AnnotationInfo annotationInfo, JSONArray geneProducts){
        List<GeneProduct> geneProductList = new ArrayList<>();
        for(int i = 0 ; i < geneProducts.size() ; i++){
            JSONObject annotationObject = geneProducts.get(i).isObject();
            GeneProduct geneProduct = new GeneProduct();
            geneProduct.setFeature(annotationInfo.getUniqueName());
            geneProduct.setProductName(annotationObject.get("productName").isString().stringValue());
            geneProduct.setEvidenceCode(annotationObject.get("evidenceCode").isString().stringValue());
            geneProduct.setEvidenceCodeLabel(annotationObject.get("evidenceCodeLabel").isString().stringValue());
            geneProduct.setAlternate(annotationObject.get("alternate").isBoolean().booleanValue());
            if(annotationObject.containsKey("reference")){
                String[] referenceString = annotationObject.get("reference").isString().stringValue().split(":");
                Reference reference = new Reference(referenceString[0], referenceString[1]);
                geneProduct.setReference(reference);
            }
            else{
                geneProduct.setReference(Reference.createEmptyReference());
            }
            if(annotationObject.containsKey("withOrFrom")){
                List<WithOrFrom> withOrFromList = new ArrayList<>();
                JSONArray goWithOrFromArray = annotationObject.get("withOrFrom").isArray();
                if(goWithOrFromArray==null){
                    String goWithString = annotationObject.get("withOrFrom").isString().stringValue();
                    goWithOrFromArray = JSONParser.parseStrict(goWithString).isArray();
                }
                for(int j = 0 ; j < goWithOrFromArray.size() ; j++){
                    WithOrFrom withOrFrom = new WithOrFrom(goWithOrFromArray.get(j).isString().stringValue());
                    withOrFromList.add(withOrFrom);
                }
                geneProduct.setWithOrFromList(withOrFromList);
            }
            List<String> notesList = new ArrayList<>();
            JSONArray notesJsonArray = annotationObject.get("notes").isArray();
            if(notesJsonArray==null){
                String notes = annotationObject.get("notes").isString().stringValue();
                notesJsonArray = JSONParser.parseStrict(notes).isArray();
            }
            for(int j = 0 ; j < notesJsonArray.size() ; j++){
                notesList.add(notesJsonArray.get(j).isString().stringValue());
            }
            geneProduct.setNoteList(notesList);
            geneProductList.add(geneProduct);
        }
        return geneProductList;
    }

    public static void saveGeneProduct(RequestCallback requestCallback, GeneProduct geneProduct) {
        RestService.sendRequest(requestCallback, "geneProduct/save", "data=" + GeneProductConverter.convertToJson(geneProduct).toString());
    }

    public static void updateGeneProduct(RequestCallback requestCallback, GeneProduct geneProduct) {
        RestService.sendRequest(requestCallback, "geneProduct/update", "data=" + GeneProductConverter.convertToJson(geneProduct).toString());
    }

    public static void deleteGeneProduct(RequestCallback requestCallback, GeneProduct geneProduct) {
        RestService.sendRequest(requestCallback, "geneProduct/delete", "data=" + GeneProductConverter.convertToJson(geneProduct).toString());
    }

    public static void getGeneProduct(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uniqueName",new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put("organism",new JSONString(organismInfo.getId()));
        RestService.sendRequest(requestCallback, "geneProduct/", "data=" + jsonObject.toString());
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

        GeneProductRestService.lookupTerm(requestCallback,TERM_LOOKUP_SERVER + evidenceCurie);
    }
}
