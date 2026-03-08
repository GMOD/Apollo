package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 4/20/15.
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
        if (object.get("count") != null) sequenceInfo.setCount((int) object.get("count").isNumber().doubleValue());
        sequenceInfo.setDefault(object.get("aDefault") != null && object.get("aDefault").isBoolean().booleanValue());


        // set the preferences if they are there
        if (object.containsKey("startBp")) {
            sequenceInfo.setStartBp(object.get("startBp").isNumber() != null ? object.get("startBp").isNumber().doubleValue() : null);
        }
        if (object.containsKey("endBp")) {
            sequenceInfo.setEndBp(object.get("endBp").isNumber() != null ? object.get("endBp").isNumber().doubleValue() : null);
        }

        return sequenceInfo;
    }

    public static List<SequenceInfo> convertFromJsonArray(JSONArray sequenceList) {
        List<SequenceInfo> sequenceInfoArrayList = new ArrayList<>();


        for (int i = 0; sequenceList != null && i < sequenceList.size(); i++) {
            sequenceInfoArrayList.add(convertFromJson(sequenceList.get(i).isObject()));
        }

        return sequenceInfoArrayList;
    }
}
