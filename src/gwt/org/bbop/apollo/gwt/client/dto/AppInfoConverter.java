package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfoConverter;

/**
 * Created by Nathan Dunn on 3/31/15.
 */
public class AppInfoConverter {

    public static AppStateInfo convertFromJson(JSONObject object){
        AppStateInfo appStateInfo = new AppStateInfo() ;

        if(object.get("currentOrganism")!=null) {
            appStateInfo.setCurrentOrganism(OrganismInfoConverter.convertFromJson(object.isObject().get("currentOrganism").isObject()));
        }

        if(object.get("currentBookmark")!=null ){
//            appStateInfo.setCurrentSequence(SequenceInfoConverter.convertFromJson(object.isObject().get("currentSequence").isObject()));
            appStateInfo.setCurrentBookmark(BookmarkInfoConverter.convertJSONObjectToBookmarkInfo(object.isObject().get("currentBookmark").isObject()));
        }
        appStateInfo.setOrganismList(OrganismInfoConverter.convertFromJsonArray(object.get("organismList").isArray()));
        if(object.containsKey("currentStartBp")){
            appStateInfo.setCurrentStartBp((int) object.get("currentStartBp").isNumber().doubleValue());
        }
        if(object.containsKey("currentEndBp")) {
            appStateInfo.setCurrentEndBp((int) object.get("currentEndBp").isNumber().doubleValue());
        }

        return appStateInfo ;
    }
}
