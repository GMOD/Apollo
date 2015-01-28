package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;

/**
 * Created by ndunn on 1/28/15.
 */
public class AnnotationRestService {

   public static JSONObject convertAnnotationInfoToJSONObject(AnnotationInfo annotationInfo){
       JSONObject jsonObject = new JSONObject();

       jsonObject.put("name",new JSONString(annotationInfo.getName()));
       jsonObject.put("symbol",new JSONString(annotationInfo.getSymbol()));
       jsonObject.put("description",new JSONString(annotationInfo.getDescription()));
       jsonObject.put("type",new JSONString(annotationInfo.getType()));
       jsonObject.put("fmin",new JSONNumber(annotationInfo.getMin()));
       jsonObject.put("fmax",new JSONNumber(annotationInfo.getMax()));
       jsonObject.put("strand",new JSONNumber(annotationInfo.getStrand()));


       return jsonObject;

   }
}
