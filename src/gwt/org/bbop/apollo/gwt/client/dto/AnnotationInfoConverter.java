package org.bbop.apollo.gwt.client.dto;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathandunn on 7/14/15.
 */
public class AnnotationInfoConverter {


    public static List<AnnotationInfo> convertFromJsonArray(JSONArray array ) {
        List<AnnotationInfo> annotationInfoList = new ArrayList<>();

        for(int i = 0 ; i < array.size() ;i++){
            annotationInfoList.add(convertFromJsonArray(array.get(i).isObject()));
        }

        return annotationInfoList ;
    }

    public static AnnotationInfo convertFromJsonArray(JSONObject object) {
        return convertFromJsonArray(object, true);
    }

    private static AnnotationInfo convertFromJsonArray(JSONObject object, boolean processChildren) {
        AnnotationInfo annotationInfo = new AnnotationInfo();
        annotationInfo.setName(object.get("name").isString().stringValue());
        annotationInfo.setType(object.get("type").isObject().get("name").isString().stringValue());
        if (object.get("symbol") != null) {
            annotationInfo.setSymbol(object.get("symbol").isString().stringValue());
        }
        if (object.get("description") != null) {
            annotationInfo.setDescription(object.get("description").isString().stringValue());
        }
        annotationInfo.setMin((int) object.get("location").isObject().get("fmin").isNumber().doubleValue());
        annotationInfo.setMax((int) object.get("location").isObject().get("fmax").isNumber().doubleValue());
        annotationInfo.setStrand((int) object.get("location").isObject().get("strand").isNumber().doubleValue());
        annotationInfo.setUniqueName(object.get("uniquename").isString().stringValue());
        annotationInfo.setSequence(object.get("sequence").isString().stringValue());
        if (object.get("owner") != null) {
            annotationInfo.setOwner(object.get("owner").isString().stringValue());
        }
        annotationInfo.setDate(object.get("date_last_modified").toString());
        List<String> noteList = new ArrayList<>();
        if (object.get("notes") != null) {
            JSONArray jsonArray = object.get("notes").isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                String note = jsonArray.get(i).isString().stringValue();
                noteList.add(note);
            }
        }
        annotationInfo.setNoteList(noteList);

        if (processChildren && object.get("children") != null) {
            JSONArray jsonArray = object.get("children").isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                AnnotationInfo childAnnotation = convertFromJsonArray(jsonArray.get(i).isObject(), true);
                annotationInfo.addChildAnnotation(childAnnotation);
            }
        }

        return annotationInfo;
    }

}
