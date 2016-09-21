package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.Assemblage;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 9/30/15.
 */
public class SequenceFeatureInfo extends JSONObject {

    //    private Integer min;
//    private Integer max;
//    private SequenceFeatureInfo feature;
//    private boolean reverseComplement = false ;
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


    public void setReverseComplement(boolean reverseComplement) {
        put(FeatureStringEnum.REVERSE_COMPLEMENT.getValue(), JSONBoolean.getInstance(reverseComplement));
    }

    public Long getStart() {
        return Math.round(get(FeatureStringEnum.START.getValue()).isNumber().doubleValue());
    }

    public Long getEnd() {
        return Math.round(get(FeatureStringEnum.END.getValue()).isNumber().doubleValue());
    }

    // TODO: this needs to be set in JSON somewhere
    private Boolean collapsed = false ;

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed ;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setParentId(String parentId){
        put(FeatureStringEnum.PARENT_ID.getValue(),new JSONString(parentId));
    }

    public String getParentId(){
        return get(FeatureStringEnum.PARENT_ID.getValue()).isString().stringValue();
    }

}
