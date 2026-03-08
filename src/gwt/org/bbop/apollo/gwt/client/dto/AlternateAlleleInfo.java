package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import com.google.gwt.core.client.GWT;

import java.util.*;

/**
 * Created by deepak.unni3 on 9/12/16.
 */

public class AlternateAlleleInfo {
    private String bases;
    private Float alleleFrequency;
    private String provenance;
    private ArrayList<AllelePropertyInfo> alleleInfo = new ArrayList<>();

    public AlternateAlleleInfo(HashMap<String, String> alternateAllele) {
        for (String key : alternateAllele.keySet()) {
            if (key.equals(FeatureStringEnum.BASES.getValue())) {
                this.bases = alternateAllele.get(key);
            }
            else if (key.equals(FeatureStringEnum.ALLELE_FREQUENCY.getValue())) {
                this.alleleFrequency = Float.parseFloat(alternateAllele.get(key));
            }
            else if (key.equals(FeatureStringEnum.PROVENANCE.getValue())) {
                this.provenance = alternateAllele.get(key);
            }
            else {
                // allele_info
                AllelePropertyInfo allelePropertyInfo = new AllelePropertyInfo();
                allelePropertyInfo.setTag(key);
                allelePropertyInfo.setValue(alternateAllele.get(key));
                this.alleleInfo.add(allelePropertyInfo);
            }
        }
    }

    public AlternateAlleleInfo(JSONObject alternateAlleleJsonObject) {
        if (alternateAlleleJsonObject.containsKey(FeatureStringEnum.BASES.getValue())) this.bases = alternateAlleleJsonObject.get(FeatureStringEnum.BASES.getValue()).isString().stringValue();
        if (alternateAlleleJsonObject.containsKey(FeatureStringEnum.ALLELE_FREQUENCY.getValue())) this.alleleFrequency = Float.parseFloat(alternateAlleleJsonObject.get(FeatureStringEnum.ALLELE_FREQUENCY.getValue()).isString().stringValue());
        if (alternateAlleleJsonObject.containsKey(FeatureStringEnum.PROVENANCE.getValue())) this.provenance = alternateAlleleJsonObject.get(FeatureStringEnum.PROVENANCE.getValue()).isString().stringValue();
        if (alternateAlleleJsonObject.containsKey(FeatureStringEnum.ALLELE_INFO.getValue())) {
            JSONArray allelePropertyInfoJsonArray = alternateAlleleJsonObject.get(FeatureStringEnum.ALLELE_INFO.getValue()).isArray();
            for (int i = 0; i < allelePropertyInfoJsonArray.size(); i++) {
                JSONObject allelePropertyInfoJsonObject = allelePropertyInfoJsonArray.get(i).isObject();
                AllelePropertyInfo allelePropertyInfo = new AllelePropertyInfo(allelePropertyInfoJsonObject, this.bases);
                this.alleleInfo.add(allelePropertyInfo);
            }
        }
    }

    public String getBases() {
        return this.bases;
    }

    public void setBases(String bases) {
        this.bases = bases;
    }

    public Float getAlleleFrequency() {
        return this.alleleFrequency;
    }

    public String getAlleleFrequencyAsString() {
        return String.valueOf(this.alleleFrequency);
    }

    public void setAlleleFrequency(Float alleleFrequency) {
        this.alleleFrequency = alleleFrequency;
    }

    public String getProvenance() {
        return this.provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public ArrayList<AllelePropertyInfo> getAlleleInfo() {
        return this.alleleInfo;
    }

    public JSONArray getAlleleInfoAsJsonArray() {
        JSONArray alleleInfoJsonArray = new JSONArray();
        int alleleInfoJsonArrayIndex = 0;
        for (AllelePropertyInfo allelePropertyInfo : this.alleleInfo) {
            JSONObject alleleInfoJsonObject = allelePropertyInfo.convertToJsonObject();
            alleleInfoJsonArray.set(alleleInfoJsonArrayIndex, alleleInfoJsonObject);
            alleleInfoJsonArrayIndex++;
        }
        return alleleInfoJsonArray;
    }

    public void setAlleleInfo(JSONArray alleleInfoJsonArray, JSONObject alternateAlleleJsonObject) {
        ArrayList<AllelePropertyInfo> alleleInfoArray = new ArrayList<>();
        for (int i = 0; i < alleleInfoJsonArray.size(); i++) {
            JSONObject alleleInfoJsonObject = alleleInfoJsonArray.get(i).isObject();
            AllelePropertyInfo allelePropertyInfo = new AllelePropertyInfo(alleleInfoJsonObject, alternateAlleleJsonObject.get(FeatureStringEnum.BASES.getValue()).isString().stringValue());
            alleleInfoArray.add(allelePropertyInfo);
        }
        GWT.log("Setting allele info: " + alleleInfoArray.get(0).getBases());
        this.alleleInfo = alleleInfoArray;
    }

    public JSONObject convertToJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.BASES.getValue(), new JSONString(this.bases));
        if (this.alleleFrequency != null) jsonObject.put(FeatureStringEnum.ALLELE_FREQUENCY.getValue(),new JSONString(String.valueOf(this.alleleFrequency)));
        if (this.provenance != null) jsonObject.put(FeatureStringEnum.PROVENANCE.getValue(), new JSONString(this.provenance));

        // allele_info
        if (this.alleleInfo != null) {
            JSONArray alleleInfoArray = new JSONArray();
            JSONObject alleleInfoObject = new JSONObject();
            int index = 0;
            alleleInfoArray.set(index, alleleInfoObject);
            for (AllelePropertyInfo allelePropertyInfo : this.alleleInfo) {
                JSONObject allelePropertyInfoJsonObject = allelePropertyInfo.convertToJsonObject();
                alleleInfoArray.set(index, allelePropertyInfoJsonObject );
                index++;
            }
            jsonObject.put(FeatureStringEnum.ALLELE_INFO.getValue(), alleleInfoArray);
        }

        return jsonObject;
    }
}
