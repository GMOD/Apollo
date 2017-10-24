package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.core.client.GWT;

import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class OrganismInfoConverter {


    public static OrganismInfo convertFromJson(JSONObject object) {
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setId(object.get("id").isNumber().toString());
        organismInfo.setName(object.get("commonName").isString().stringValue());
        if (object.get("sequences") != null && object.get("sequences").isNumber() != null) {
            organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
        } else {
            organismInfo.setNumSequences(0);
        }
        if (object.get("annotationCount") != null) {
            organismInfo.setNumFeatures((int) object.get("annotationCount").isNumber().doubleValue());
        } else {
            organismInfo.setNumFeatures(0);
        }
        organismInfo.setDirectory(object.get("directory").isString().stringValue());
        if (object.get("valid") != null) {
            organismInfo.setValid(object.get("valid").isBoolean().booleanValue());
        }
        if (object.get("genus") != null && object.get("genus").isString() != null) {
            organismInfo.setGenus(object.get("genus").isString().stringValue());
        }
        if (object.get("species") != null && object.get("species").isString() != null) {
            organismInfo.setSpecies(object.get("species").isString().stringValue());
        }
        if (object.get("blatdb") != null && object.get("blatdb").isString() != null) {
            organismInfo.setBlatDb(object.get("blatdb").isString().stringValue());
        }
        if (object.get("nonDefaultTranslationTable") != null && object.get("nonDefaultTranslationTable").isString() != null) {
            organismInfo.setNonDefaultTranslationTable(object.get("nonDefaultTranslationTable").isString().stringValue());
        }
        if (object.get("publicMode") != null) {
            organismInfo.setPublicMode(object.get("publicMode").isBoolean().booleanValue());
        }
        if (object.get("editable") != null) {
            organismInfo.setEditable(object.get("editable").isBoolean().booleanValue());
        }
        organismInfo.setCurrent(object.get("currentOrganism") != null && object.get("currentOrganism").isBoolean().booleanValue());
        return organismInfo;
    }

    public static List<OrganismInfo> convertFromJsonArray(JSONArray organismList) {
        List<OrganismInfo> organismInfoList = new ArrayList<>();

        for (int i = 0; i < organismList.size(); i++) {
            OrganismInfo organismInfo = convertFromJson(organismList.get(i).isObject());
            organismInfoList.add(organismInfo);
        }

        return organismInfoList;
    }

    /**
     * @param organismInfo
     * @return
     */
    public static JSONObject convertOrganismInfoToJSONObject(OrganismInfo organismInfo) {
        JSONObject object = new JSONObject();
        if (organismInfo.getId() != null) {
            object.put("id", new JSONString(organismInfo.getId()));
        }
        object.put("commonName", new JSONString(organismInfo.getName()));
        object.put("directory", new JSONString(organismInfo.getDirectory()));
        if (organismInfo.getGenus() != null) {
            object.put("genus", new JSONString(organismInfo.getGenus()));
        }
        if (organismInfo.getSpecies() != null) {
            object.put("species", new JSONString(organismInfo.getSpecies()));
        }
        if (organismInfo.getNumSequences() != null) {
            object.put("sequences", new JSONNumber(organismInfo.getNumFeatures()));
        }
        if (organismInfo.getNumFeatures() != null) {
            object.put("annotationCount", new JSONNumber(organismInfo.getNumFeatures()));
        }
        if (organismInfo.getBlatDb() != null) {
            object.put("blatdb", new JSONString(organismInfo.getBlatDb()));
        }
        if (organismInfo.getNonDefaultTranslationTable() != null) {
            object.put("nonDefaultTranslationTable", new JSONString(organismInfo.getNonDefaultTranslationTable()));
        }

        GWT.log("convertOrganismInfoToJSONObject "+organismInfo.getPublicMode());
        object.put("publicMode", JSONBoolean.getInstance(organismInfo.getPublicMode()));

        return object;
    }



    public static List<OrganismInfo> convertJSONStringToOrganismInfoList(String jsonString) {
        JSONValue returnValue = JSONParser.parseStrict(jsonString);
        List<OrganismInfo> organismInfoList = new ArrayList<>();
        JSONArray array = returnValue.isArray();
        return convertFromJsonArray(array);
    }

}
