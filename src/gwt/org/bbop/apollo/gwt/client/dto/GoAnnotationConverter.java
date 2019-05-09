package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.bbop.apollo.gwt.shared.go.Reference;
import org.bbop.apollo.gwt.shared.go.WithOrFrom;

/**
 * Created by ndunn on 3/31/15.
 */
public class GoAnnotationConverter {

    public static GoAnnotation convertFromJson(JSONObject object){
        GoAnnotation goAnnotation = new GoAnnotation();



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

    public static JSONObject convertToJson(GoAnnotation goAnnotation){
        JSONObject object = new JSONObject();

        // TODO: an NPE in here, somehwere
        if (goAnnotation.getId() != null) {
            object.put("id", new JSONNumber(goAnnotation.getId()));
        }
        object.put("gene",new JSONString(goAnnotation.getGoGene()));
        object.put("goTerm",new JSONString(goAnnotation.getGoTerm()));
        object.put("geneRelationship",new JSONString(goAnnotation.getGeneRelationship()));
        object.put("evidenceCode",new JSONString(goAnnotation.getEvidenceCode()));
        object.put("negate",JSONBoolean.getInstance(goAnnotation.isNegate()));

        // TODO: finish this
        JSONArray withArray = new JSONArray();
        JSONArray referenceArray = new JSONArray();

        for(WithOrFrom withOrFrom  : goAnnotation.getWithOrFromList()){
            withArray.set(withArray.size(),new JSONString(withOrFrom.getDisplay()));
        }

        for(Reference reference : goAnnotation.getReferenceList()){
            referenceArray.set(referenceArray.size(),new JSONString(reference.getReferenceString()));
        }

        object.put("withOrFrom",withArray);
        object.put("references",referenceArray);


        return object;
    }
}
