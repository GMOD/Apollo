package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nathan Dunn on 4/20/15.
 */
public class SequenceInfoConverter {
    public static SequenceInfo convertFromJson(JSONObject object) {
        SequenceInfo sequenceInfo = new SequenceInfo();
        sequenceInfo.setId((long) object.get("id").isNumber().doubleValue());
        sequenceInfo.setName(object.get("name").isString().stringValue());
        sequenceInfo.setStart((int) object.get("start").isNumber().doubleValue());
        sequenceInfo.setEnd((int) object.get("end").isNumber().doubleValue());
        sequenceInfo.setLength((int) object.get("length").isNumber().doubleValue());
        sequenceInfo.setSelected(object.get("selected") != null && object.get("selected").isBoolean().booleanValue());
        sequenceInfo.setDefault(object.get("aDefault") != null && object.get("aDefault").isBoolean().booleanValue());
        return sequenceInfo ;
    }

    public static List<SequenceInfo> convertFromJsonArray(JSONArray sequenceList) {
        List<SequenceInfo> sequenceInfoArrayList = new ArrayList<>();


        for(int i = 0 ; sequenceList!=null && i < sequenceList.size() ; i++){
            sequenceInfoArrayList.add(convertFromJson(sequenceList.get(i).isObject()));
        }

        return sequenceInfoArrayList;
    }
}
