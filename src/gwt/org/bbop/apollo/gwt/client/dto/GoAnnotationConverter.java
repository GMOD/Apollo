package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.go.Aspect;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.bbop.apollo.gwt.shared.go.Reference;
import org.bbop.apollo.gwt.shared.go.WithOrFrom;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class GoAnnotationConverter {

  public static GoAnnotation convertFromJson(JSONObject object) {
    GoAnnotation goAnnotation = new GoAnnotation();

//                    "geneRelationship":"RO:0002326", "goTerm":"GO:0031084", "references":"[\"ref:12312\"]", "gene":
//                    "1743ae6c-9a37-4a41-9b54-345065726d5f", "negate":false, "evidenceCode":"ECO:0000205", "withOrFrom":
//                    "[\"adf:12312\"]"
    goAnnotation.setId(Math.round(object.get("id").isNumber().doubleValue()));
    goAnnotation.setAspect(Aspect.valueOf(object.get("aspect").isString().stringValue()));
    goAnnotation.setGene(object.get("feature").isString().stringValue());
    goAnnotation.setGoTerm(object.get("goTerm").isString().stringValue());
    if(object.containsKey("goTermLabel")){
      goAnnotation.setGoTermLabel(object.get("goTermLabel").isString().stringValue());
    }
    if(object.containsKey("evidenceCodeLabel")){
      goAnnotation.setEvidenceCodeLabel(object.get("evidenceCodeLabel").isString().stringValue());
    }
    goAnnotation.setGeneRelationship(object.get("geneRelationship").isString().stringValue());
    goAnnotation.setEvidenceCode(object.get("evidenceCode").isString().stringValue());
    goAnnotation.setReference(new Reference(object.get("reference").isString().stringValue()));
    goAnnotation.setNegate(object.get("negate").isBoolean().booleanValue());


    List<String> noteList = new ArrayList<>();
    if (object.containsKey("notes")) {
      String notesString = object.get("notes").isString().stringValue();
      JSONArray notesArray = JSONParser.parseLenient(notesString).isArray();
      for (int i = 0; i < notesArray.size(); i++) {
        noteList.add(notesArray.get(i).isString().stringValue());
      }
    }
    goAnnotation.setNoteList(noteList);

    List<WithOrFrom> withOrFromList = new ArrayList<>();
    if (object.get("withOrFrom").isString() != null) {
      String withOrFromString = object.get("withOrFrom").isString().stringValue();
      JSONArray withOrFromArray = JSONParser.parseLenient(withOrFromString).isArray();
      for (int i = 0; i < withOrFromArray.size(); i++) {
        WithOrFrom withOrFrom ;
        if(withOrFromArray.get(i).isString()!=null){
          withOrFrom = new WithOrFrom(withOrFromArray.get(i).isString().stringValue());
        }
        else{
          withOrFrom = new WithOrFrom("Value is an error, please edit or delete: "+withOrFromArray.get(i));
        }
        withOrFromList.add(withOrFrom);
      }
    }
    goAnnotation.setWithOrFromList(withOrFromList);

    return goAnnotation;
  }

  public static JSONObject convertToJson(GoAnnotation goAnnotation) {
    JSONObject object = new JSONObject();

    // TODO: an NPE in here, somehwere
    if (goAnnotation.getId() != null) {
      object.put("id", new JSONNumber(goAnnotation.getId()));
    }
    object.put("aspect", new JSONString(goAnnotation.getAspect().name()));
    object.put("feature", new JSONString(goAnnotation.getGene()));
    object.put("goTerm", new JSONString(goAnnotation.getGoTerm()));
    object.put("goTermLabel", new JSONString(goAnnotation.getGoTermLabel()));
    object.put("geneRelationship", new JSONString(goAnnotation.getGeneRelationship()));
    object.put("evidenceCode", new JSONString(goAnnotation.getEvidenceCode()));
    object.put("evidenceCodeLabel", new JSONString(goAnnotation.getEvidenceCodeLabel()));
    object.put("negate", JSONBoolean.getInstance(goAnnotation.isNegate()));
    object.put("reference", new JSONString(goAnnotation.getReference().getReferenceString()));

    // TODO: finish this
    JSONArray withArray = new JSONArray();

    for (WithOrFrom withOrFrom : goAnnotation.getWithOrFromList()) {
      withArray.set(withArray.size(), new JSONString(withOrFrom.getDisplay()));
    }

    JSONArray notesArray = new JSONArray();
    for (String note : goAnnotation.getNoteList()) {
      notesArray.set(notesArray.size(), new JSONString(note));
    }


    object.put("withOrFrom", withArray);
    object.put("notes", notesArray);


    return object;
  }
}
