package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfoConverter;

/**
 * Created by Nathan Dunn on 3/31/15.
 */
public class AppInfoConverter {

    public static AppStateInfo convertFromJson(JSONObject object){
        AppStateInfo appStateInfo = new AppStateInfo() ;

        if(object.get("currentOrganism")!=null) {
            appStateInfo.setCurrentOrganism(OrganismInfoConverter.convertFromJson(object.isObject().get("currentOrganism").isObject()));
        }

        if(object.get("currentAssemblage")!=null ){
            appStateInfo.setCurrentAssemblage(AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(object.isObject().get("currentAssemblage").isObject()));
        }
        appStateInfo.setOrganismList(OrganismInfoConverter.convertFromJsonArray(object.get("organismList").isArray()));
        if(object.containsKey("currentStartBp")){
            appStateInfo.setCurrentStartBp((long) object.get("currentStartBp").isNumber().doubleValue());
        }
        if(object.containsKey("currentEndBp")) {
            appStateInfo.setCurrentEndBp((long) object.get("currentEndBp").isNumber().doubleValue());
        }

        return appStateInfo ;
    }
}
