package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class BookmarkSequence extends JSONObject {

    public String getName() {
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public SequenceFeatureList getFeatures() {
        if(containsKey(FeatureStringEnum.FEATURES.getValue())){
            return (SequenceFeatureList) get(FeatureStringEnum.FEATURES.getValue());
        }
        return null ;
    }

    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(),new JSONString(groupName));
    }
}
