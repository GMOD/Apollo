package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by deepak.unni3 on 9/16/16.
 */
public class AttributeInfo {

    private String tag;
    private String value;

    public AttributeInfo() {

    }

    public AttributeInfo(JSONObject variantPropertyInfoJsonObject) {
        String tag = variantPropertyInfoJsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue();
        this.tag = tag;

        String value = null ;
        if(variantPropertyInfoJsonObject.containsKey(FeatureStringEnum.VALUE.getValue())){
            value = variantPropertyInfoJsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue();
        }
        this.value = value;
    }

    public AttributeInfo(String tag, String value) {
        this.tag = tag ;
        this.value = value ;
    }

    public String getTag() { return this.tag; }

    public void setTag(String tag) { this.tag = tag; }

    public String getValue() { return this.value; }

    public void setValue(String value) { this.value = value; }

    public JSONObject convertToJsonObject() {
        JSONObject variantPropertyJsonObject = new JSONObject();
        variantPropertyJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
        variantPropertyJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
        return variantPropertyJsonObject;
    }
}
