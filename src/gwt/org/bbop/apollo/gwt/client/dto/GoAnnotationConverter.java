package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
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
        GWT.log("json object: "+object.toString());

        goAnnotation.setId(Math.round(object.get("id").isNumber().doubleValue()));
        goAnnotation.setGene(object.get("gene").isString().stringValue());
        goAnnotation.setGoTerm(object.get("goTerm").isString().stringValue());
        goAnnotation.setGeneRelationship(object.get("geneRelationship").isString().stringValue());
        goAnnotation.setEvidenceCode(object.get("evidenceCode").isString().stringValue());
        goAnnotation.setNegate(object.get("negate").isBoolean().booleanValue());


        String referencesString = object.get("references").isString().stringValue();
        JSONArray referenceArray = JSONParser.parseStrict(referencesString).isArray();
        List<Reference> referenceList = new ArrayList<>();
        for (int i = 0; i < referenceArray.size(); i++) {
            Reference reference = new Reference(referenceArray.get(i).isString().stringValue());
            referenceList.add(reference);
        }
        goAnnotation.setReferenceList(referenceList);

        String withOrFromString = object.get("withOrFrom").isString().stringValue();
        JSONArray withOrFromArray = JSONParser.parseStrict(withOrFromString).isArray();
        List<WithOrFrom> withOrFromList = new ArrayList<>();
        for (int i = 0; i < withOrFromArray.size(); i++) {
            WithOrFrom withOrFrom = new WithOrFrom(withOrFromArray.get(i).isString().stringValue());
            withOrFromList.add(withOrFrom);
        }
        goAnnotation.setWithOrFromList(withOrFromList);

//        goAnnotation.setReferenceList(object.get("references").isArray());
//        goAnnotation.setWithOrFromList(object.get("withOrFrom").isArray());


//        if(object.get("currentOrganism")!=null) {
//            appStateInfo.setCurrentOrganism(OrganismInfoConverter.convertFromJson(object.isObject().get("currentOrganism").isObject()));
//        }
//
//        if(object.get("currentSequence")!=null ){
//            appStateInfo.setCurrentSequence(SequenceInfoConverter.convertFromJson(object.isObject().get("currentSequence").isObject()));
//        }
//        appStateInfo.setOrganismList(OrganismInfoConverter.convertFromJsonArray(object.get("organismList").isArray()));
//        if(object.containsKey("currentStartBp")){
//            appStateInfo.setCurrentStartBp((int) object.get("currentStartBp").isNumber().doubleValue());
//        }
//        if(object.containsKey("currentEndBp")) {
//            appStateInfo.setCurrentEndBp((int) object.get("currentEndBp").isNumber().doubleValue());
//        }
//        if(object.containsKey(FeatureStringEnum.COMMON_DATA_DIRECTORY.getValue())) {
//            appStateInfo.setCommonDataDirectory( object.get(FeatureStringEnum.COMMON_DATA_DIRECTORY.getValue()).isString().stringValue());
//        }

        return goAnnotation;
    }

    public static JSONObject convertToJson(GoAnnotation goAnnotation) {
        JSONObject object = new JSONObject();

        // TODO: an NPE in here, somehwere
        if (goAnnotation.getId() != null) {
            object.put("id", new JSONNumber(goAnnotation.getId()));
        }
        object.put("gene", new JSONString(goAnnotation.getGene()));
        object.put("goTerm", new JSONString(goAnnotation.getGoTerm()));
        object.put("geneRelationship", new JSONString(goAnnotation.getGeneRelationship()));
        object.put("evidenceCode", new JSONString(goAnnotation.getEvidenceCode()));
        object.put("negate", JSONBoolean.getInstance(goAnnotation.isNegate()));

        // TODO: finish this
        JSONArray withArray = new JSONArray();
        JSONArray referenceArray = new JSONArray();

        for (WithOrFrom withOrFrom : goAnnotation.getWithOrFromList()) {
            withArray.set(withArray.size(), new JSONString(withOrFrom.getDisplay()));
        }

        for (Reference reference : goAnnotation.getReferenceList()) {
            referenceArray.set(referenceArray.size(), new JSONString(reference.getReferenceString()));
        }

        object.put("withOrFrom", withArray);
        object.put("references", referenceArray);


        return object;
    }
}
