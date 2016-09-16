package org.bbop.apollo.gwt.client.dto.assemblage;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathandunn on 10/1/15.
 */
public class AssemblageInfoConverter {

    public static JSONObject convertAssemblageInfoToJSONObject(AssemblageInfo assemblageInfo) {
        JSONObject jsonObject = new JSONObject();

        if (assemblageInfo.getId() != null) {
            jsonObject.put(FeatureStringEnum.ID.getValue(), new JSONNumber(assemblageInfo.getId()));
        }
        if(assemblageInfo.getName() != null){
            jsonObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(assemblageInfo.getName()));
        }
        jsonObject.put(FeatureStringEnum.DESCRIPTION.getValue(), new JSONString(assemblageInfo.getDescription()));
        if (assemblageInfo.getType() != null) {
            jsonObject.put(FeatureStringEnum.TYPE.getValue(), new JSONString(assemblageInfo.getType()));
        }
        if (assemblageInfo.getPadding() != null) {
            jsonObject.put("padding", new JSONNumber(assemblageInfo.getPadding()));
        }
        if (assemblageInfo.getStart() != null) {
            jsonObject.put(FeatureStringEnum.START.getValue(), new JSONNumber(assemblageInfo.getStart()));
        }
        if (assemblageInfo.getEnd() != null) {
            jsonObject.put(FeatureStringEnum.END.getValue(), new JSONNumber(assemblageInfo.getEnd()));
        }
        jsonObject.put(FeatureStringEnum.SEQUENCE_LIST.getValue(), assemblageInfo.getSequenceList());
        if (assemblageInfo.getPayload() != null) {
            jsonObject.put("payload", assemblageInfo.getPayload());
        }

        return jsonObject;
    }


    public static AssemblageInfo convertJSONObjectToAssemblageInfo(JSONObject jsonObject) {
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        if (jsonObject.containsKey(FeatureStringEnum.ID.getValue())) {
            assemblageInfo.setId((long) jsonObject.get(FeatureStringEnum.ID.getValue()).isNumber().doubleValue());
        }
        if (jsonObject.containsKey(FeatureStringEnum.NAME.getValue()) && jsonObject.get(FeatureStringEnum.NAME.getValue()).isString()!=null) {
            assemblageInfo.setName(jsonObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue());
        }

        if (jsonObject.containsKey("padding")) {
            assemblageInfo.setPadding((int) jsonObject.get("padding").isNumber().doubleValue());
        }
        else{
            assemblageInfo.setPadding(0);
        }
        if (jsonObject.containsKey("payload")) {
            assemblageInfo.setPayload(jsonObject.get("payload").isObject());
        }
        if (jsonObject.containsKey(FeatureStringEnum.START.getValue())) {
            assemblageInfo.setStart((long) jsonObject.get(FeatureStringEnum.START.getValue()).isNumber().doubleValue());
            assemblageInfo.setEnd((long) jsonObject.get(FeatureStringEnum.END.getValue()).isNumber().doubleValue());
        }

        JSONArray sequenceListArray = jsonObject.get("sequenceList").isArray();
        // some weird stuff here
        if (sequenceListArray == null) {
            String sequenceArrayString = jsonObject.get("sequenceList").isString().stringValue();
            sequenceArrayString = sequenceArrayString.replaceAll("\\\\", "");
            sequenceListArray = JSONParser.parseStrict(sequenceArrayString).isArray();
        }
        AssemblageSequenceList assemblageSequenceList = convertJSONArrayToSequenceList(sequenceListArray);
        assemblageInfo.setSequenceList(assemblageSequenceList);

        return assemblageInfo;
    }

    private static AssemblageSequenceList convertJSONArrayToSequenceList(JSONArray sequenceListArray) {
        AssemblageSequenceList assemblageSequenceList = new AssemblageSequenceList();
        for (int i = 0; i < sequenceListArray.size(); i++) {
            AssemblageSequence assemblageSequence = new AssemblageSequence(sequenceListArray.get(i).isObject());
            assemblageSequenceList.addSequence(assemblageSequence);
        }
        return assemblageSequenceList;
    }

    // TODO:
    public static JSONArray convertAssemblageInfoToJSONArray(AssemblageInfo... selectedSet) {
        JSONArray jsonArray = new JSONArray();

        for (AssemblageInfo assemblageInfo : selectedSet) {
            jsonArray.set(jsonArray.size(), convertAssemblageInfoToJSONObject(assemblageInfo));
        }

        return jsonArray;
    }

    public static List<AssemblageInfo> convertFromJsonArray(JSONArray assemblageList) {
        List<AssemblageInfo> assemblageInfoArrayList = new ArrayList<>();
        for (int i = 0; assemblageList != null && i < assemblageList.size(); i++) {
            assemblageInfoArrayList.add(convertJSONObjectToAssemblageInfo(assemblageList.get(i).isObject()));
        }
        return assemblageInfoArrayList;
    }
}
