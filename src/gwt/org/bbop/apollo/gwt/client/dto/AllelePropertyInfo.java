package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

public class AllelePropertyInfo {

    private String variant;
    private String bases;
    private String tag;
    private String value;

    public AllelePropertyInfo() {

    }

    public AllelePropertyInfo(JSONObject allelePropertyInfoJsonObject) {
        String tag = allelePropertyInfoJsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue();
        String value = allelePropertyInfoJsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue();
        this.tag = tag;
        this.value = value;
    }

    public AllelePropertyInfo(JSONObject allelePropertyInfoJsonObject, String bases) {
        String tag = allelePropertyInfoJsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue();
        String value = allelePropertyInfoJsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue();
        this.bases = bases;
        this.tag = tag;
        this.value = value;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getBases() {
        return bases;
    }

    public void setBases(String bases) {
        this.bases = bases;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return this.tag;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() { return this.value; }

    public JSONObject convertToJsonObject () {
        JSONObject allelePropertyInfoJsonObject = new JSONObject();
        if (this.tag != null) {
            allelePropertyInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
        }
        if (this.value != null) {
            allelePropertyInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
        }
        return allelePropertyInfoJsonObject;
    }
}
