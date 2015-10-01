package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

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


    public static JSONObject convertAnnotationInfoToJSONObject(AnnotationInfo annotationInfo){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("name",new JSONString(annotationInfo.getName()));
        jsonObject.put("uniquename",new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put("symbol",annotationInfo.getSymbol()!=null ? new JSONString(annotationInfo.getSymbol()):new JSONString(""));
        jsonObject.put("description",annotationInfo.getDescription()!=null ? new JSONString(annotationInfo.getDescription()):new JSONString(""));
        jsonObject.put("type",new JSONString(annotationInfo.getType()));
        jsonObject.put("fmin",annotationInfo.getMin()!=null ? new JSONNumber(annotationInfo.getMin()): null);
        jsonObject.put("fmax",annotationInfo.getMax()!=null ? new JSONNumber(annotationInfo.getMax()): null);
        jsonObject.put("strand",annotationInfo.getStrand()!=null ? new JSONNumber(annotationInfo.getStrand()): null);


        return jsonObject;

    }

}
