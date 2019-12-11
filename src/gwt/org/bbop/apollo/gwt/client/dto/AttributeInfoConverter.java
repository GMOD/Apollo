package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

public class AttributeInfoConverter {

    public static AttributeInfo convertToAttributeFromObject(JSONObject jsonObject) {
        AttributeInfo dbXrefInfo = new AttributeInfo();
        dbXrefInfo.setTag(jsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue());
        dbXrefInfo.setValue(jsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue());
        return dbXrefInfo;
    }

    public static List<AttributeInfo> convertToAttributeFromArray(JSONArray array) {
        List<AttributeInfo> dbXrefInfoList = new ArrayList<>();
        for(int i = 0 ; i < array.size() ; i++){
            dbXrefInfoList.add(convertToAttributeFromObject(array.get(i).isObject()));
        }
        return dbXrefInfoList;
    }

    public static JSONObject convertToJson(AttributeInfo dbXrefInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.TAG.getValue(),new JSONString(dbXrefInfo.getTag()));
        jsonObject.put(FeatureStringEnum.VALUE.getValue(),new JSONString(dbXrefInfo.getValue()));
        return jsonObject;
    }
}
