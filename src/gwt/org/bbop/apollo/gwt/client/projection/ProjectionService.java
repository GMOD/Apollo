package org.bbop.apollo.gwt.client.projection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.projection.DiscontinuousProjection;
import org.bbop.apollo.gwt.shared.projection.Coordinate;

/**
 * Created by nathandunn on 2/16/16.
 */
public class ProjectionService {

    private static ProjectionStore projectionStore = new ProjectionStore();

    public ProjectionService(){
        exportStaticMethod();
    }

    public static String projectValue(String refSeqObject,String inputNumber){
        // handle as a projection
        GWT.log("REF SEQ LABEL: "+refSeqObject);
        JSONObject refSeqJsonObject = JSONParser.parseStrict(refSeqObject).isObject();
        String refSeqLabel = refSeqJsonObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue();
        Double doubleValue = Double.parseDouble(inputNumber);
        if(refSeqLabel.startsWith("{")){
            DiscontinuousProjection discontinuousProjection = projectionStore.getProjection(refSeqLabel);
            if(discontinuousProjection==null){
                discontinuousProjection = createProjection(refSeqLabel,refSeqJsonObject);
                projectionStore.storeProjection(refSeqLabel,discontinuousProjection);
            }
            Integer intValue = ((int) Math.round(doubleValue));
            intValue = discontinuousProjection.projectValue(intValue);
//            intValue += 7 ;
            return intValue.toString();
        }
        else{
//            inputNumber += "3";
            return inputNumber;
        }
    }

    private static DiscontinuousProjection createProjection(String refSeqLabel, JSONObject refSeqJsonObject) {
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection();
        GWT.log(refSeqLabel);
        String refSeqDescription =  refSeqLabel.substring(0,refSeqLabel.lastIndexOf(":"));

//         {"padding":0, "projection":"None", "referenceTrack":[], "sequenceList":[{"name":"Group1.1"}], "label":"Group1.1"}:-1..-1

//        {
//            "sequenceList":[
//                {
//                    "name":"chr1",
//                    "features":[
//                        {"name":"SOX9","fmin":123,"fmax":789}
//                    ],
//                    "folding":[
//                        {"fmin":123,"fmax":789}
//                    ]
//                }
//            ]
//        }
        JSONObject projectionDescription = JSONParser.parseStrict(refSeqDescription).isObject();
        Double length = refSeqJsonObject.get("length").isNumber().doubleValue();
        JSONArray sequenceListArray = projectionDescription.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isArray();
        for(int i = 0 ; i < sequenceListArray.size() ; i++){
            JSONObject sequenceObject = sequenceListArray.get(i).isObject();
            // doing nothing with that
            String sequenceName = sequenceObject.get(FeatureStringEnum.NAME.getValue()).isString().stringValue();

            // handle features
            if(sequenceObject.containsKey(FeatureStringEnum.FEATURES.getValue())){
                JSONArray featuresArray = sequenceObject.get(FeatureStringEnum.FEATURES.getValue()).isArray();
            }

            if(sequenceObject.containsKey(FeatureStringEnum.FOLDING.getValue())){
                JSONArray foldingArray = sequenceObject.get(FeatureStringEnum.FOLDING.getValue()).isArray();
                for(int foldingIndex = 0 ; foldingIndex < foldingArray.size() ; foldingIndex++){
                    JSONObject foldingObject = foldingArray.get(foldingIndex).isObject();
                    Double fmin = foldingObject.get(FeatureStringEnum.FMIN.getValue()).isNumber().doubleValue();
                    Double fmax = foldingObject.get(FeatureStringEnum.FMAX.getValue()).isNumber().doubleValue();
//                    discontinuousProjection.addInterval((int) Math.round(fmin), (int) Math.round(fmax),5);
                    discontinuousProjection.foldInterval((int) Math.round(fmin), (int) Math.round(fmax),5, (int) Math.round(length));
                }
            }

        }

        return discontinuousProjection;
    }

    public static native void exportStaticMethod() /*-{
        $wnd.projectValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectValue(Ljava/lang/String;Ljava/lang/String;));
    }-*/;
}
