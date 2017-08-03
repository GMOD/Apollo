package org.bbop.apollo.gwt.client.assemblage;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by nathandunn on 10/5/16.
 */
public class FeatureLocationInfo extends JSONObject{

    public FeatureLocationInfo(JSONObject fromJson){
        for (String key : fromJson.keySet()) {
            this.put(key, fromJson.get(key));
        }
    }

    public Long getMin(){
        return Math.round(get(FeatureStringEnum.FMIN.getValue()).isNumber().doubleValue());
    }

    public void setMin(Long min){
        put(FeatureStringEnum.FMIN.getValue(),new JSONNumber(min));
    }

    public Long getMax(){
        return Math.round(get(FeatureStringEnum.FMAX.getValue()).isNumber().doubleValue());
    }

    public void setMax(Long max){
        put(FeatureStringEnum.FMAX.getValue(),new JSONNumber(max));
    }

    // reverse to the name
    public String getName() {
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
    }
}
