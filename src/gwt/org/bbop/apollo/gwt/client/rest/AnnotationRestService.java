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
       jsonObject.put("uniquename",new JSONString(annotationInfo.getUniqueName()));
       jsonObject.put("track", new JSONString(annotationInfo.getSequence()));
       jsonObject.put("symbol",annotationInfo.getSymbol()!=null ? new JSONString(annotationInfo.getSymbol()):new JSONString(""));
       jsonObject.put("description",annotationInfo.getDescription()!=null ? new JSONString(annotationInfo.getDescription()):new JSONString(""));
       jsonObject.put("type",new JSONString(annotationInfo.getType()));
       jsonObject.put("fmin",annotationInfo.getMin()!=null ? new JSONNumber(annotationInfo.getMin()): null);
       jsonObject.put("fmax",annotationInfo.getMax()!=null ? new JSONNumber(annotationInfo.getMax()): null);
       jsonObject.put("strand",annotationInfo.getStrand()!=null ? new JSONNumber(annotationInfo.getStrand()): null);


       return jsonObject;

   }
}
