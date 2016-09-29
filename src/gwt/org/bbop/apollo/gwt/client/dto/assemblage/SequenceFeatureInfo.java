package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class SequenceFeatureInfo extends JSONObject {

    public SequenceFeatureInfo() { }

    public SequenceFeatureInfo(JSONObject fromJson){
        for (String key : fromJson.keySet()) {
            this.put(key, fromJson.get(key));
        }
    }

    public String getName() {
        return get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
    }

    public void setName(String groupName) {
        put(FeatureStringEnum.NAME.getValue(), new JSONString(groupName));
    }

    public void setFeature(SequenceFeatureInfo featuresObject) {
        put(FeatureStringEnum.FEATURE.getValue(), featuresObject);
    }

    public void setStart(Long min) {
        put(FeatureStringEnum.START.getValue(), new JSONNumber(min));
    }

    public void setEnd(Long max) {
        put(FeatureStringEnum.END.getValue(), new JSONNumber(max));
    }



    public Long getStart() {
        return Math.round(get(FeatureStringEnum.START.getValue()).isNumber().doubleValue());
    }

    public Long getEnd() {
        return Math.round(get(FeatureStringEnum.END.getValue()).isNumber().doubleValue());
    }

    public void setCollapsed(boolean collapsed) {
        put(FeatureStringEnum.COLLAPSED.getValue(),JSONBoolean.getInstance(collapsed));
    }

    public boolean isCollapsed() {
        if(containsKey(FeatureStringEnum.COLLAPSED.getValue())){
            return get(FeatureStringEnum.COLLAPSED.getValue()).isBoolean().booleanValue();
        }
        return false ;
    }

    public void setParentId(String parentId){
        put(FeatureStringEnum.PARENT_ID.getValue(),new JSONString(parentId));
    }

    public String getParentId(){
        return get(FeatureStringEnum.PARENT_ID.getValue()).isString().stringValue();
    }

}
