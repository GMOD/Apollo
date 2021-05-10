package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.AnnotatorPanel;
import org.bbop.apollo.gwt.client.VariantDetailPanel;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.geneProduct.GeneProduct;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.bbop.apollo.gwt.shared.provenance.Provenance;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.List;
import java.util.Set;

/**
 * Created by ndunn on 1/28/15.
 */
public class AnnotationRestService extends RestService {


    public static JSONObject convertAnnotationInfoToJSONObject(AnnotationInfo annotationInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(annotationInfo.getName()));
        jsonObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put(FeatureStringEnum.SYMBOL.getValue(), annotationInfo.getSymbol() != null ? new JSONString(annotationInfo.getSymbol()) : new JSONString(""));
        jsonObject.put(FeatureStringEnum.STATUS.getValue(), annotationInfo.getStatus() != null ? new JSONString(annotationInfo.getStatus()) : null);
        jsonObject.put(FeatureStringEnum.DESCRIPTION.getValue(), annotationInfo.getDescription() != null ? new JSONString(annotationInfo.getDescription()) : new JSONString(""));
        jsonObject.put(FeatureStringEnum.TYPE.getValue(), new JSONString(annotationInfo.getType()));
        jsonObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        jsonObject.put(FeatureStringEnum.SYNONYMS.getValue(), annotationInfo.getSynonyms() != null ? new JSONString(annotationInfo.getSynonyms()) : null );

        if (VariantDetailPanel.variantTypes.contains(annotationInfo.getType())) {
            if (annotationInfo.getReferenceAllele() != null)
                jsonObject.put(FeatureStringEnum.REFERENCE_ALLELE.getValue(), new JSONString(annotationInfo.getReferenceAllele()));
            if (annotationInfo.getAlternateAlleles() != null)
                jsonObject.put(FeatureStringEnum.ALTERNATE_ALLELES.getValue(), annotationInfo.getAlternateAllelesAsJsonArray());
            if (annotationInfo.getVariantProperties() != null)
                jsonObject.put(FeatureStringEnum.VARIANT_INFO.getValue(), annotationInfo.getVariantPropertiesAsJsonArray());
        }
        jsonObject.put(FeatureStringEnum.FMIN.getValue(), annotationInfo.getMin() != null ? new JSONNumber(annotationInfo.getMin()) : null);
        jsonObject.put(FeatureStringEnum.FMAX.getValue(), annotationInfo.getMax() != null ? new JSONNumber(annotationInfo.getMax()) : null);
        jsonObject.put(FeatureStringEnum.IS_FMIN_PARTIAL.getValue(),  JSONBoolean.getInstance(annotationInfo.getPartialMin()) );
        jsonObject.put(FeatureStringEnum.IS_FMAX_PARTIAL.getValue(), JSONBoolean.getInstance(annotationInfo.getPartialMax()) );
        jsonObject.put(FeatureStringEnum.OBSOLETE.getValue(), JSONBoolean.getInstance(annotationInfo.getObsolete()) );
        jsonObject.put(FeatureStringEnum.STRAND.getValue(), annotationInfo.getStrand() != null ? new JSONNumber(annotationInfo.getStrand()) : null);

        return jsonObject;

    }

    static JSONObject generateTypeObject(String type){
      JSONObject featureTypeObject = new JSONObject();
      JSONObject cvObject = new JSONObject();
      cvObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(FeatureStringEnum.SEQUENCE.getValue()));
      featureTypeObject.put(FeatureStringEnum.CV.getValue(),cvObject);
      featureTypeObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(type));
      return featureTypeObject;
    }

    static JSONObject generateLocationObject(AnnotationInfo annotationInfo){
      JSONObject locationObject = new JSONObject();
      locationObject.put(FeatureStringEnum.FMIN.getValue(), annotationInfo.getMin() != null ? new JSONNumber(annotationInfo.getMin()) : null);
      locationObject.put(FeatureStringEnum.FMAX.getValue(), annotationInfo.getMax() != null ? new JSONNumber(annotationInfo.getMax()) : null);
      locationObject.put(FeatureStringEnum.IS_FMIN_PARTIAL.getValue(),  JSONBoolean.getInstance(annotationInfo.getPartialMin()) );
      locationObject.put(FeatureStringEnum.IS_FMAX_PARTIAL.getValue(), JSONBoolean.getInstance(annotationInfo.getPartialMax()) );
      locationObject.put(FeatureStringEnum.STRAND.getValue(), annotationInfo.getStrand() != null ? new JSONNumber(annotationInfo.getStrand()) : null);
      return locationObject;
  }

  /**
   * Creates a transcript with a matching exon
   * @param requestCallback
   * @param annotationInfo
   * @return
   */
  public static void createTranscriptWithExon(RequestCallback requestCallback, AnnotationInfo annotationInfo) {
    JSONObject jsonObject = new JSONObject();
    JSONArray featuresArray = new JSONArray();
    jsonObject.put(FeatureStringEnum.SEQUENCE.getValue(),new JSONString(annotationInfo.getSequence()));
    jsonObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
    JSONObject featureObject = new JSONObject();
    featuresArray.set(featuresArray.size(), featureObject);


    //          {\"track\":\"Group11.18\",\"features\":[{\"location\":{\"fmin\":3464814,\"fmax\":3464958,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB44961-RA\",\"orig_id\":\"GB44961-RA\",\"children\":[{\"location\":{\"fmin\":3464814,\"fmax\":3464958,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\",\"clientToken\":\"66322431814575743501200095773\"}
    JSONArray childrenArray = new JSONArray();
    JSONObject childObject = new JSONObject();
    childObject.put(FeatureStringEnum.LOCATION.getValue(),generateLocationObject(annotationInfo));
    childObject.put(FeatureStringEnum.TYPE.getValue(),generateTypeObject("exon"));
    childrenArray.set(0,childObject);
    featureObject.put(FeatureStringEnum.CHILDREN.getValue(),childrenArray);


    featureObject.put(FeatureStringEnum.LOCATION.getValue(),generateLocationObject(annotationInfo));
    featureObject.put(FeatureStringEnum.TYPE.getValue(),generateTypeObject(annotationInfo.getType()));
    featureObject.put(FeatureStringEnum.DESCRIPTION.getValue(),new JSONString("created with search hit") );
    if(annotationInfo.getSynonyms()!=null){
      featureObject.put(FeatureStringEnum.SYNONYMS.getValue(),new JSONString(annotationInfo.getSynonyms()) );
    }


    sendRequest(requestCallback, "annotationEditor/addTranscript", "data=" + jsonObject.toString());
  }


  public static JSONObject deleteAnnotations(RequestCallback requestCallback, Set<AnnotationInfo> annotationInfoSet) {
        JSONObject jsonObject = new JSONObject();
        JSONArray featuresArray = new JSONArray();
        jsonObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        for (AnnotationInfo annotationInfo : annotationInfoSet) {
            JSONObject uniqueNameObject = new JSONObject();
            uniqueNameObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(annotationInfo.getUniqueName()));
            featuresArray.set(featuresArray.size(), uniqueNameObject);
        }

        sendRequest(requestCallback, "annotationEditor/deleteFeature", "data=" + jsonObject.toString());
        return jsonObject;
    }

    private static JSONObject generateSequenceObject(Set<SequenceInfo> sequenceInfoSet){
        JSONObject jsonObject = new JSONObject();
        JSONArray sequencesArray = new JSONArray();
        jsonObject.put(FeatureStringEnum.SEQUENCE.getValue(), sequencesArray);

        for (SequenceInfo sequenceInfo : sequenceInfoSet) {
            JSONObject sequenceIdObject = new JSONObject();
            sequenceIdObject.put(FeatureStringEnum.ID.getValue(), new JSONNumber(sequenceInfo.getId()));
            sequencesArray.set(sequencesArray.size(), sequenceIdObject);
        }
        return jsonObject ;
    }

    public static JSONObject deleteAnnotationsFromSequences(RequestCallback requestCallback, Set<SequenceInfo> sequenceInfoSet) {
        JSONObject jsonObject = generateSequenceObject(sequenceInfoSet);
        sendRequest(requestCallback, "annotationEditor/deleteFeaturesForSequences", "data=" + jsonObject.toString());
        return jsonObject;
    }

    public static void updateCommonPath(RequestCallback requestCallback, String directory) {
        sendRequest(requestCallback, "annotator/updateCommonPath", "directory="+directory);
    }

    public static JSONObject deleteVariantAnnotationsFromSequences(RequestCallback requestCallback, Set<SequenceInfo> sequenceInfoSet) {
        JSONObject jsonObject = generateSequenceObject(sequenceInfoSet);
        sendRequest(requestCallback, "annotationEditor/deleteVariantEffectsForSequences", "data=" + jsonObject.toString());
        return jsonObject;
    }


  public static void findAnnotationByUniqueName(RequestCallback requestCallback,String uniqueName){

    String url = Annotator.getRootUrl() + "annotator/findAnnotationsForSequence/?searchUniqueName=true&annotationName="+uniqueName;
    long requestIndex = AnnotatorPanel.getNextRequestIndex();
    url += "&request="+requestIndex;
    url += "&statusString=" ;
    sendRequest(requestCallback, url);

  }

  public static JSONObject addFunctionalAnnotations(RequestCallback requestCallback, JSONObject jsonObject) {
    RestService.sendRequest(requestCallback,"annotator/addFunctionalAnnotations","data="+jsonObject.toString());
    return jsonObject;
  }
}
