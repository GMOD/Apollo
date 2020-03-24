package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.geneProduct.Reference;
import org.bbop.apollo.gwt.shared.geneProduct.WithOrFrom;
import org.bbop.apollo.gwt.shared.geneProduct.GeneProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class GeneProductConverter {

  public static GeneProduct convertFromJson(JSONObject object) {
    GeneProduct geneProduct = new GeneProduct();

    geneProduct.setId(Math.round(object.get("id").isNumber().doubleValue()));
    geneProduct.setFeature(object.get("feature").isString().stringValue());
    geneProduct.setProductName(object.get("productName").isString().stringValue());
    geneProduct.setAlternate(object.get("alternate").isBoolean().booleanValue());
    if(object.containsKey("evidenceCodeLabel")){
      geneProduct.setEvidenceCodeLabel(object.get("evidenceCodeLabel").isString().stringValue());
    }
    geneProduct.setEvidenceCode(object.get("evidenceCode").isString().stringValue());
    geneProduct.setReference(new Reference(object.get("reference").isString().stringValue()));

    List<String> noteList = new ArrayList<>();
    if (object.containsKey("notes")) {
      String notesString = object.get("notes").isString().stringValue();
      JSONArray notesArray = JSONParser.parseLenient(notesString).isArray();
      for (int i = 0; i < notesArray.size(); i++) {
        noteList.add(notesArray.get(i).isString().stringValue());
      }
    }
    geneProduct.setNoteList(noteList);

    List<WithOrFrom> withOrFromList = new ArrayList<>();
    if (object.get("withOrFrom").isString() != null) {
      String withOrFromString = object.get("withOrFrom").isString().stringValue();
      JSONArray withOrFromArray = JSONParser.parseLenient(withOrFromString).isArray();
      for (int i = 0; i < withOrFromArray.size(); i++) {
        WithOrFrom withOrFrom = new WithOrFrom(withOrFromArray.get(i).isString().stringValue());
        withOrFromList.add(withOrFrom);
      }
    }
    geneProduct.setWithOrFromList(withOrFromList);

    return geneProduct;
  }

  public static JSONObject convertToJson(GeneProduct geneProduct) {
    JSONObject object = new JSONObject();

    // TODO: an NPE in here, somehwere
    if (geneProduct.getId() != null) {
      object.put("id", new JSONNumber(geneProduct.getId()));
    }
    object.put("feature", new JSONString(geneProduct.getFeature()));
    object.put("productName", new JSONString(geneProduct.getProductName()));
    object.put("alternate", JSONBoolean.getInstance(geneProduct.isAlternate()));
    object.put("evidenceCode", new JSONString(geneProduct.getEvidenceCode()));
    object.put("evidenceCodeLabel", new JSONString(geneProduct.getEvidenceCodeLabel()));
    object.put("reference", new JSONString(geneProduct.getReference().getReferenceString()));
    JSONArray notesArray = new JSONArray();
    if(geneProduct.getNoteList()!=null && geneProduct.getNoteList().size()>0){
      for (String note : geneProduct.getNoteList()) {
        notesArray.set(notesArray.size(), new JSONString(note));
      }
    }

    // TODO: finish this
    JSONArray withArray = new JSONArray();

    for (WithOrFrom withOrFrom : geneProduct.getWithOrFromList()) {
      withArray.set(withArray.size(), new JSONString(withOrFrom.getDisplay()));
    }

    object.put("withOrFrom", withArray);
    object.put("notes", notesArray);

    return object;
  }
}
