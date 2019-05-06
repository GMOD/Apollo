package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;

/**
 * Created by ndunn on 3/31/15.
 */
public class GoConverter {

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

        if (goAnnotation.getId() != null) {
            object.put("id", new JSONNumber(goAnnotation.getId()));
        }

        object.put("go",new JSONString(goAnnotation.getGoTerm().getLinkDisplay()));

        // TODO: finish this




        return object;
    }
}
