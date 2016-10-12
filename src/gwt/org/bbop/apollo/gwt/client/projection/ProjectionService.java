package org.bbop.apollo.gwt.client.projection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocationInfo;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocations;
import org.bbop.apollo.gwt.client.dto.assemblage.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.projection.Coordinate;
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection;
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for generating projection libraries.
 * <p>
 * Created by nathandunn on 10/10/16.
 */
public class ProjectionService {


    private static Map<String, MultiSequenceProjection> projectionMap = new HashMap<>();

    public ProjectionService() {
        exportStaticMethod();
    }

    static MultiSequenceProjection createProjectionFromAssemblageInfo(AssemblageInfo assemblageInfo) {
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection();
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();

        List<ProjectionSequence> projectionSequenceList = new ArrayList<>();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < assemblageSequenceList.size(); i++) {
            AssemblageSequence assemblageSequence = assemblageSequenceList.getSequence(i);

            ProjectionSequence projectionSequence = generateProjectSequenceFromAssemblageSequence(assemblageSequence);
            projectionSequence.setOrder(i);
            projectionSequenceList.add(projectionSequence);

            SequenceFeatureInfo sequenceFeatureInfo = assemblageSequence.getFeature();
            if (sequenceFeatureInfo != null && sequenceFeatureInfo.hasLocation()) {
                FeatureLocations featureLocations = sequenceFeatureInfo.getLocation();
                List<Coordinate> theseCoordinates = generateCoordinatesFromFeatureLocations(featureLocations, projectionSequence);
                coordinates.addAll(theseCoordinates);
            } else {
                Coordinate fullCoordinate = new Coordinate(projectionSequence.getStart(), projectionSequence.getEnd(), projectionSequence);
                coordinates.add(fullCoordinate);
            }
        }

        multiSequenceProjection.addProjectionSequences(projectionSequenceList);

        multiSequenceProjection.addCoordinates(coordinates);
        multiSequenceProjection.calculateOffsets();

//        List<ProjectionSequence> projectionSequenceList = convertJsonArrayToSequences((JSON.parse(assemblage.sequenceList) as JSONArray),assemblage.organism.commonName)
//
//        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
//        multiSequenceProjection.addProjectionSequences(projectionSequenceList)
//        multiSequenceProjection.addCoordinates(coordinates)
//        multiSequenceProjection.calculateOffsets()
//        Map<String,ProjectionSequence> projectionSequenceMap = [:]
//
//        multiSequenceProjection.projectedSequences.each {
//            projectionSequenceMap.put(it.name,it)
//        }
////        List<String> sequenceNames = multiSequenceProjection.projectedSequences.name
//        // TODO: speed this up by caching sequences
//        Sequence.findAllByNameInList(projectionSequenceMap.keySet() as List<String>).each {
//            def projectionSequence = projectionSequenceMap.get(it.name)
//            projectionSequence.unprojectedLength = it.length
//        }

        return multiSequenceProjection;
    }

    private static List<Coordinate> generateCoordinatesFromFeatureLocations(FeatureLocations featureLocations, ProjectionSequence projectionSequence) {

        List<Coordinate> coordinateList = new ArrayList<>();
        for (int i = 0; i < featureLocations.size(); i++) {
            FeatureLocationInfo featureLocationInfo = featureLocations.getFeatureLocationInfo(i);
            Coordinate coordinate = new Coordinate(featureLocationInfo.getMin(), featureLocationInfo.getMax(), projectionSequence);
            coordinateList.add(coordinate);
        }
        return coordinateList;
    }

    private static ProjectionSequence generateProjectSequenceFromAssemblageSequence(AssemblageSequence assemblageSequence) {
        ProjectionSequence projectionSequence = new ProjectionSequence();
        projectionSequence.setName(assemblageSequence.getName());
        projectionSequence.setStart(assemblageSequence.getStart());
        projectionSequence.setEnd(assemblageSequence.getEnd());
        projectionSequence.setUnprojectedLength(assemblageSequence.getLength());
        projectionSequence.setReverse(assemblageSequence.getReverse());

        if (assemblageSequence.getFeature() != null) {
            String featureName = assemblageSequence.getFeature().getName();
            List<String> featureNameList = new ArrayList<>();
            featureNameList.add(featureName);
            projectionSequence.setFeatures(featureNameList);
        }

        return projectionSequence;
    }

    /**
     * {"id":5778, "name":"Feature Region 1", "description":"GB52238-RA (Group11.4)::GB52236-RA (Group11.4)::GB53498-RA (GroupUn87)", "padding":0, "start":10057, "end":30529, "sequenceList":[{"name":"Group11.4", "start":10057, "end":18796, "reverse":false, "feature":{"start":10057, "name":"GB52238-RA", "end":18796, "parent_id":"Group11.4"}},{"name":"Group11.4", "start":52653, "end":59162, "reverse":false, "feature":{"start":52653, "name":"GB52236-RA", "end":59162, "parent_id":"Group11.4"}},{"name":"GroupUn87", "start":29196, "end":30529, "reverse":true, "feature":{"start":29196, "name":"GB53498-RA", "end":30529, "parent_id":"GroupUn87"}}]}:10057..30529
     *
     * @param projectionString
     * @return
     */
    static MultiSequenceProjection getProjectionForString(String projectionString) {
        Integer index = projectionString.lastIndexOf(":");
        projectionString = projectionString.substring(0, index);
        JSONObject projectionObject = JSONParser.parseStrict(projectionString).isObject();
        AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(projectionObject);
        return createProjectionFromAssemblageInfo(assemblageInfo);
    }

    public static Long projectValue(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectValue(referenceString, (long) input);
    }

    public static Long projectValue(String referenceString, Long input) {
        GWT.log("trying to project a value in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        Long projectedValue = projection.projectValue(input);
        GWT.log("projected a value " + projectedValue + " for " + input);
        return projectedValue;
    }

    public static Long projectReverseValue(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectReverseValue(referenceString, (long) input);
    }


    public static Long projectReverseValue(String referenceString, Long input) {
        GWT.log("trying to project a value in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        Long projectedValue = projection.projectReverseValue(input);
        GWT.log("projected a value " + projectedValue + " for " + input);
        return projectedValue;
    }

    public static String projectReverseSequence(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectReverseSequence(referenceString, (long) input);
    }


    public static String projectReverseSequence(String referenceString, Long input) {
        GWT.log("trying to project a sequence in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        ProjectionSequence projectionSequence = projection.getReverseProjectionSequence(input);
//        GWT.log("projected a value "+ projectedValue + " for " + input);
        // TODO: convert to a JSON object
        return projectionSequence.getName();
    }

    public static JavaScriptObject getReverseProjection(String referenceString, String inputString) {
        Integer input = Integer.parseInt(inputString);
        return getReverseProjection(referenceString, (long) input);
    }

    public static JavaScriptObject getReverseProjection(String referenceString, Long input) {
        GWT.log("trying to project a sequence in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("originalValue", new JSONNumber(input));

        ProjectionSequence projectionSequence = projection.getReverseProjectionSequence(input);
        assert projectionSequence != null;
//        if(projectionSequence!=null){
        Integer order = projectionSequence.getOrder();
        Integer numberOfSequences = projection.size();
        Long reverseValue = projection.projectReverseValue(input);
        reverseValue = reverseValue - projectionSequence.getOriginalOffset() ;
        // TODO: looking at RefSeqProjectionService::projectSequence()
        // need to take into account order?
        if (projectionSequence.getReverse()) {
            reverseValue = projectionSequence.getUnprojectedLength() - reverseValue ;
//            // ??
//            reverseValue = reverseValue + projectionSequence.getOriginalOffset();
////            reverseValue = reverseValue - projectionSequence.getOriginalOffset();
//            if (order == 0) {
//            } else if (order == numberOfSequences - 1) {
//
//            }
//            // one of the middle ones
//            else{
//
//            }
//
        } else {

        }


        Window.alert("input["+input + "]->["+reverseValue+"]");

//            reverseValue += projectionSequence.getOriginalOffset();

        jsonObject.put("sequence", convertToJsonObject(projectionSequence));
        jsonObject.put("reverseValue", new JSONNumber(reverseValue));


        JavaScriptObject javaScriptObject = JsonUtils.safeEval(jsonObject.toString());
        // TODO: convert to a JSON object
        return javaScriptObject;
    }

    private static JSONObject convertToJsonObject(ProjectionSequence projectionSequence) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.NAME.getValue(), new JSONString(projectionSequence.getName()));
        jsonObject.put(FeatureStringEnum.START.getValue(), new JSONNumber(projectionSequence.getStart()));
        jsonObject.put(FeatureStringEnum.END.getValue(), new JSONNumber(projectionSequence.getEnd()));
        jsonObject.put(FeatureStringEnum.REVERSE.getValue(), JSONBoolean.getInstance(projectionSequence.getReverse()));
        jsonObject.put("offset", new JSONNumber(projectionSequence.getOffset()));
        jsonObject.put("originalOffset", new JSONNumber(projectionSequence.getOriginalOffset()));
        return jsonObject;
    }


    public static native void exportStaticMethod() /*-{
        $wnd.projectValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectValue(Ljava/lang/String;Ljava/lang/String;));
        $wnd.projectReverseValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectReverseValue(Ljava/lang/String;Ljava/lang/String;));
        $wnd.projectReverseSequence = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectReverseSequence(Ljava/lang/String;Ljava/lang/String;));
        $wnd.getReverseProjection = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getReverseProjection(Ljava/lang/String;Ljava/lang/String;));
    }-*/;
}
