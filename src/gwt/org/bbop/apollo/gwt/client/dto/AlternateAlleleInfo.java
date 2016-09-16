package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.*;

/**
 * Created by deepak.unni3 on 9/12/16.
 */

public class AlternateAlleleInfo {
    private String bases;
    private Float alleleFrequency;
    private String provenance;
    private HashMap<String, String> alleleInfo = new HashMap<>();

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
                this.alleleInfo.put(key, alternateAllele.get(key));
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

    public HashMap getAlleleInfo() {
        return this.alleleInfo;
    }

    public JSONArray getAlleleInfoAsJsonArray() {
        JSONArray alleleInfoJsonArray = new JSONArray();
        int alleleInfoJsonArrayIndex = 0;
        for (String key : this.alleleInfo.keySet()) {
            JSONObject alleleInfoJsonObject = new JSONObject();
            alleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(key));
            alleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.alleleInfo.get(key)));
            alleleInfoJsonArray.set(alleleInfoJsonArrayIndex, alleleInfoJsonObject);
            alleleInfoJsonArrayIndex++;
        }

        return alleleInfoJsonArray;
    }

    public void setAlleleInfo(JSONArray alleleInfoJsonArray) {
        HashMap<String, String> alleleInfoMap = new HashMap<>();
        for (int i = 0; i < alleleInfoJsonArray.size(); i++) {
            JSONObject alleleInfoJsonObject = alleleInfoJsonArray.get(i).isObject();
            String tag = alleleInfoJsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue();
            String value = alleleInfoJsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue();
            alleleInfoMap.put(tag, value);
        }
        this.alleleInfo = alleleInfoMap;
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
            for (String key : this.alleleInfo.keySet()) {
                alleleInfoObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(key));
                alleleInfoObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(alleleInfo.get(key)));
                alleleInfoArray.set(index, alleleInfoObject);
                index++;
            }
            jsonObject.put(FeatureStringEnum.ALLELE_INFO.getValue(), alleleInfoArray);
        }

        return jsonObject;
    }
}
