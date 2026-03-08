package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by deepak.unni3 on 9/16/16.
 */
public class StatusInfo {

    private String status;

    public StatusInfo() {
    }

    public StatusInfo(JSONObject variantPropertyInfoJsonObject) {
        String value = null ;
        if(variantPropertyInfoJsonObject.containsKey(FeatureStringEnum.COMMENT.getValue())){
            value = variantPropertyInfoJsonObject.get(FeatureStringEnum.COMMENT.getValue()).isString().stringValue();
        }
        this.status = value;
    }

    public StatusInfo(String value) {
        this.status = value ;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JSONObject convertToJsonObject() {
        JSONObject variantPropertyJsonObject = new JSONObject();
        variantPropertyJsonObject.put(FeatureStringEnum.STATUS.getValue(), new JSONString(this.status));
        return variantPropertyJsonObject;
    }
}
