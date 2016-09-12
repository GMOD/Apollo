package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.VariantDetailPanel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/28/15.
 */
public class AnnotationRestService {

   public static JSONObject convertAnnotationInfoToJSONObject(AnnotationInfo annotationInfo){
       JSONObject jsonObject = new JSONObject();
       jsonObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(annotationInfo.getName()));
       jsonObject.put(FeatureStringEnum.UNIQUENAME.getValue(),new JSONString(annotationInfo.getUniqueName()));
       jsonObject.put(FeatureStringEnum.SYMBOL.getValue(),annotationInfo.getSymbol()!=null ? new JSONString(annotationInfo.getSymbol()):new JSONString(""));
       jsonObject.put(FeatureStringEnum.DESCRIPTION.getValue(),annotationInfo.getDescription()!=null ? new JSONString(annotationInfo.getDescription()):new JSONString(""));
       jsonObject.put(FeatureStringEnum.TYPE.getValue(),new JSONString(annotationInfo.getType()));

       if (VariantDetailPanel.variantTypes.contains(annotationInfo.getType())) {
           jsonObject.put(FeatureStringEnum.REFERENCE_BASES.getValue(), annotationInfo.getReferenceBases() != null ? new JSONString(annotationInfo.getReferenceBases()) : new JSONString(""));
           jsonObject.put(FeatureStringEnum.ALTERNATE_ALLELES.getValue(), annotationInfo.getAlternateAlleles() != null ? annotationInfo.getAlternateAllelesAsJsonArray() : new JSONString(""));

       }
       jsonObject.put(FeatureStringEnum.FMIN.getValue(),annotationInfo.getMin()!=null ? new JSONNumber(annotationInfo.getMin()): null);
       jsonObject.put(FeatureStringEnum.FMAX.getValue(),annotationInfo.getMax()!=null ? new JSONNumber(annotationInfo.getMax()): null);
       jsonObject.put(FeatureStringEnum.STRAND.getValue(),annotationInfo.getStrand()!=null ? new JSONNumber(annotationInfo.getStrand()): null);

       return jsonObject;

   }
}
