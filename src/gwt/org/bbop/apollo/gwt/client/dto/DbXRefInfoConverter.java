package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

public class DbXRefInfoConverter {

    public static DbXrefInfo convertToDbXrefFromObject(JSONObject jsonObject) {
        DbXrefInfo dbXrefInfo = new DbXrefInfo();
        dbXrefInfo.setTag(jsonObject.get(FeatureStringEnum.TAG.getValue()).isString().stringValue());
        dbXrefInfo.setValue(jsonObject.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue());
        return dbXrefInfo;
    }

    public static List<DbXrefInfo> convertToDbXrefFromArray(JSONArray array) {
        List<DbXrefInfo> dbXrefInfoList = new ArrayList<>();
        for(int i = 0 ; i < array.size() ; i++){
            dbXrefInfoList.add(convertToDbXrefFromObject(array.get(i).isObject()));
        }
        return dbXrefInfoList;
    }

    public static JSONObject convertToJson(DbXrefInfo dbXrefInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.TAG.getValue(),new JSONString(dbXrefInfo.getTag()));
        jsonObject.put(FeatureStringEnum.VALUE.getValue(),new JSONString(dbXrefInfo.getValue()));
        return jsonObject;
    }

    public static DbXrefInfo convertFromJson(JSONObject object) {
        DbXrefInfo dbXrefInfo = new DbXrefInfo();
        dbXrefInfo.setTag(object.get(FeatureStringEnum.TAG.getValue()).isString().stringValue());
        dbXrefInfo.setValue(object.get(FeatureStringEnum.VALUE.getValue()).isString().stringValue());
        return dbXrefInfo;
    }
}
