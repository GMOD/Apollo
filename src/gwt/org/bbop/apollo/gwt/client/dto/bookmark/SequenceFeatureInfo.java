package org.bbop.apollo.gwt.client.dto.bookmark;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class SequenceFeatureInfo extends JSONObject{

    public String getName(){
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
    }

    public void setFeatures(SequenceFeatureList featuresArray) {
        put(FeatureStringEnum.FEATURES.getValue(),featuresArray);
    }
}
