package org.bbop.apollo.gwt.client.projection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocationInfo;
import org.bbop.apollo.gwt.client.assemblage.FeatureLocations;
import org.bbop.apollo.gwt.client.dto.assemblage.*;
import org.bbop.apollo.gwt.client.rest.AssemblageRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.projection.Coordinate;
import org.bbop.apollo.gwt.shared.projection.DiscontinuousProjection;
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


    // TODO: cache this instead of rebuilding
    private static Map<String, MultiSequenceProjection> projectionMap = new HashMap<>();

    public ProjectionService() {
        exportStaticMethod();
    }

    public static MultiSequenceProjection createProjectionFromAssemblageInfo(AssemblageInfo assemblageInfo) {
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
            // TODO: simplify these two lines
            if (assemblageSequence.hasLocation()) {
                try {
                    FeatureLocations featureLocations = assemblageSequence.getLocation();
                    List<Coordinate> theseCoordinates = generateCoordinatesFromFeatureLocations(featureLocations, projectionSequence);
                    coordinates.addAll(theseCoordinates);
                } catch (Exception e) {
                    GWT.log("has error!: " + e.fillInStackTrace().toString());
                }
            } else if (sequenceFeatureInfo != null && sequenceFeatureInfo.hasLocation()) {
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
    public static MultiSequenceProjection getProjectionForString(String projectionString) {
        Integer index = projectionString.lastIndexOf(":");
        if (!projectionString.endsWith("}") && index > 0) {
            projectionString = projectionString.substring(0, index);
        }
        JSONObject projectionObject = JSONParser.parseStrict(projectionString).isObject();
        AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(projectionObject);
        return createProjectionFromAssemblageInfo(assemblageInfo);
    }

    public static Long getProjectionLength(String projectionString) {
        MultiSequenceProjection multiSequenceProjection = getProjectionForString(projectionString);
        return multiSequenceProjection.getLength();
    }

    public static Long projectValue(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectValue(referenceString, (long) input);
    }

    public static Long projectValue(String referenceString, Long input) {
//        GWT.log("trying to project a value in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        Long projectedValue = projection.projectValue(input);
//        GWT.log("projected a value " + projectedValue + " for " + input);
        return projectedValue;
    }

    public static Long projectReverseValue(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectReverseValue(referenceString, (long) input);
    }


    public static Long projectReverseValue(String referenceString, Long input) {
//        GWT.log("trying to project a value in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        Long projectedValue = projection.projectReverseValue(input);
//        GWT.log("projected a value " + projectedValue + " for " + input);
        return projectedValue;
    }

    public static String projectReverseSequence(String referenceString, String otherType) {
        Integer input = Integer.parseInt(otherType);
        return projectReverseSequence(referenceString, (long) input);
    }


    public static String projectReverseSequence(String referenceString, Long input) {
//        GWT.log("trying to project a sequence in GWT: " + input);
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
//        GWT.log("trying to project a sequence in GWT: " + input);
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("originalValue", new JSONNumber(input));

        ProjectionSequence projectionSequence = projection.getReverseProjectionSequence(input);
        if (projectionSequence == null) {
            GWT.log("no sequence found for " + input);
            return JsonUtils.safeEval(new JSONObject().toString());
        }

        Long reverseValue = projection.projectLocalReverseValue(input);

        jsonObject.put("sequence", convertToJsonObject(projectionSequence));
        jsonObject.put("reverseValue", new JSONNumber(reverseValue));

        JavaScriptObject javaScriptObject = JsonUtils.safeEval(jsonObject.toString());
        // TODO: convert to a JSON object
        return javaScriptObject;
    }

    public static JavaScriptObject getBorders(String referenceString, String leftBorder, String rightBorder) {

        Long projectedLeft = (long) Integer.parseInt(leftBorder) + 1;
        Long projectedRight = (long) Integer.parseInt(rightBorder) + 1;
        JSONArray returnArray = new JSONArray();

        if (projectedLeft >= 0 && projectedRight >= 0) {
//            GWT.log(projectedLeft + " " + projectedRight);
            MultiSequenceProjection projection = getProjectionForString(referenceString);
            projection.calculateOffsets();

            List<ProjectionSequence> projectionSequenceList = projection.getReverseProjectionSequences(projectedLeft, projectedRight);

//            GWT.log("returned value: " + projectedLeft + " " + projectedRight + " -> " + projectionSequenceList.size());

            // TODO: getting these to generate the boundaries
//            Long reverseProjectionLeft = projection.projectReverseValue(projectedLeft);
//            Long reverseProjectionRight = projection.projectReverseValue(projectedRight);

            // assume that these are returned by order
            for (int i = 0; i < projectionSequenceList.size(); i++) {
                ProjectionSequence projectionSequence = projectionSequenceList.get(i);

                Long offset = projectionSequence.getOffset();
                Long length = projectionSequence.getLength();

                // in this case we have both a left and a right boundary at projected right
                if (projectedLeft >= offset && projectedLeft <= offset + length && projectedRight > offset + length) {
                    // generate both a left and a right one
                    JSONObject sequenceObject1 = convertToJsonObject(projectionSequence);
                    sequenceObject1.put("type", new JSONString("right"));
                    // (A2 - N1 ) / (N2 - N1 )
                    Float position = (projectionSequence.getLength() - projectedLeft) / (float) (projectedRight - projectedLeft);
                    sequenceObject1.put("position", new JSONNumber(position));
                    returnArray.set(returnArray.size(), sequenceObject1);

                    JSONObject sequenceObject2 = convertToJsonObject(projectionSequence);
                    sequenceObject2.put("type", new JSONString("left"));
                    sequenceObject2.put("position", new JSONNumber(position));
                    returnArray.set(returnArray.size(), sequenceObject2);
                }
            }
        }

        return JsonUtils.safeEval(returnArray.toString());
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


    public static Long calculatedProjectedLength(AssemblageInfo assemblageInfo) {
        MultiSequenceProjection multiSequenceProjection = createProjectionFromAssemblageInfo(assemblageInfo);
        return multiSequenceProjection.getLength();
    }

    public static boolean regionContainsFolds(String fminString, String fmaxString, String referenceSequenceString) {
        Integer fmin = Integer.parseInt(fminString);
        Integer fmax = Integer.parseInt(fmaxString);
        MultiSequenceProjection multiSequenceProjection = getProjectionForString(referenceSequenceString);
        Coordinate reverseCoordinate = multiSequenceProjection.projectReverseCoordinate((long) fmin, (long) fmax);
        return (fmax - fmin != reverseCoordinate.getMax() - reverseCoordinate.getMin());
    }

    private static void projectFeatures(String features, String refSeqString) {
        JSONObject projectionCommand = new JSONObject();

        JSONArray featuresArray = JSONParser.parseStrict(features).isArray();

        refSeqString = refSeqString.substring(0, refSeqString.lastIndexOf(":"));
        JSONObject referenceProjection = JSONParser.parseStrict(refSeqString).isObject();
        projectionCommand.put(FeatureStringEnum.SEQUENCE.getValue(), referenceProjection);
        projectionCommand.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        AssemblageRestService.projectFeatures(projectionCommand);
    }

    private static void foldSelectedTranscript(String features, String refSeqString) {
        JSONObject projectionCommand = new JSONObject();

        JSONArray featuresArray = JSONParser.parseStrict(features).isArray();

        refSeqString = refSeqString.substring(0, refSeqString.lastIndexOf(":"));
        JSONObject referenceProjection = JSONParser.parseStrict(refSeqString).isObject();
        projectionCommand.put(FeatureStringEnum.SEQUENCE.getValue(), referenceProjection);
        projectionCommand.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        AssemblageRestService.foldTranscripts(projectionCommand);
    }

    private static void foldBetweenExons(String features, String refSeqString) {
        JSONObject projectionCommand = new JSONObject();

        JSONArray featuresArray = JSONParser.parseStrict(features).isArray();

        refSeqString = refSeqString.substring(0, refSeqString.lastIndexOf(":"));
        JSONObject referenceProjection = JSONParser.parseStrict(refSeqString).isObject();
        projectionCommand.put(FeatureStringEnum.SEQUENCE.getValue(), referenceProjection);
        projectionCommand.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        AssemblageRestService.foldBetweenExons(projectionCommand);
    }

    private static void removeFolds(String features, String refSeqString) {
        JSONObject projectionCommand = new JSONObject();

        JSONArray featuresArray = JSONParser.parseStrict(features).isArray();

        refSeqString = refSeqString.substring(0, refSeqString.lastIndexOf(":"));
        JSONObject referenceProjection = JSONParser.parseStrict(refSeqString).isObject();
        projectionCommand.put(FeatureStringEnum.SEQUENCE.getValue(), referenceProjection);
        projectionCommand.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        AssemblageRestService.removeFolds(projectionCommand);
    }

    /**
     * @param projection
     * @param startBase  - start projected value
     * @param endBase    - end projected value
     * @return
     */
    private static JSONArray getReversedFolds(MultiSequenceProjection projection, Integer startBase, Integer endBase) {
        JSONArray foldValues = new JSONArray();
        for (ProjectionSequence projectionSequence : projection.getProjectedSequences()) {
            DiscontinuousProjection discontinuousProjection = projection.getSequenceDiscontinuousProjectionMap().get(projectionSequence);
            Coordinate firstCoordate = null;
            Coordinate lastCoordinate = null;
            for (Coordinate coordinate : discontinuousProjection.getCoordinates()) {
                if (firstCoordate == null) {
                    firstCoordate = coordinate;
                } else {
                    lastCoordinate = firstCoordate;
                    firstCoordate = coordinate;

                    Long leftEdge = discontinuousProjection.projectValue(firstCoordate.getMax());
                    Long rightEdge = discontinuousProjection.projectValue(lastCoordinate.getMin());

//                    GWT.log("startBase "+startBase + " vs left "+firstCoordate.getMax() + " projecteed left: " + discontinuousProjection.projectValue(firstCoordate.getMax()));
//                    GWT.log("endBase "+endBase + " vs right "+lastCoordinate.getMin() + " projectied right: " + discontinuousProjection.projectValue(lastCoordinate.getMin()));

                    if(leftEdge >= startBase && rightEdge <= endBase ){
                        JSONObject jsonObject = new JSONObject();
                        Long foldPoint = projection.projectReverseValue(firstCoordate.getMax());
                        jsonObject.put("foldPoint", new JSONNumber(foldPoint));
                        jsonObject.put("left", new JSONNumber(firstCoordate.getMax()));
                        jsonObject.put("right", new JSONNumber(lastCoordinate.getMin()));
                        foldValues.set(foldValues.size(), jsonObject);
                    }
//                    }
                }
            }
        }

        return foldValues;
    }

    private static JavaScriptObject getFolds(String locationString, String startBaseString, String endBaseString) {

        JSONObject jsonObject = JSONParser.parseStrict(locationString).isObject();

//        Long fmin = Math.round(jsonObject.get(FeatureStringEnum.FMIN.getValue()).isNumber().doubleValue());
//        Long fmax = Math.round(jsonObject.get(FeatureStringEnum.FMAX.getValue()).isNumber().doubleValue());
        String sequenceString = jsonObject.get(FeatureStringEnum.SEQUENCE.getValue()).isString().stringValue();


        JSONArray sequenceListArray = JSONParser.parseStrict(sequenceString).isArray();


        AssemblageSequenceList assemblageSequenceList = AssemblageInfoConverter.convertJSONArrayToSequenceList(sequenceListArray);
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        assemblageInfo.setSequenceList(assemblageSequenceList);
        MultiSequenceProjection multiSequenceProjection = createProjectionFromAssemblageInfo(assemblageInfo);

        // Note: parsing to a Long fails (renders 0).  Would have to parse to a double (or pass in a number) and move to a long.
        Integer startBase = Integer.valueOf(startBaseString);
        Integer endBase = Integer.valueOf(endBaseString);
//        GWT.log("parsed startBase "+startBase + " from startBaseString '" + startBaseString+"'");
//        GWT.log("parsed endBase "+endBase + " from endBaseString '" + endBaseString+"'");
        JSONArray foldPoints = getReversedFolds(multiSequenceProjection, startBase, endBase);

        return JsonUtils.safeEval(foldPoints.toString());
    }

    private static JavaScriptObject getFoldsForRegion(String refSeqString, String startBaseString, String endBaseString) {

//        JSONObject jsonObject = JSONParser.parseStrict(locationString).isObject();
//        GWT.log("input refseqname '"+refSeqString+"'");
        if (!refSeqString.endsWith("}")) {
//            String locationString = refSeqString.substring(refSeqString.lastIndexOf(":") + 1, refSeqString.length());
//            Long projectedFmin = Long.parseLong(locationString.split("\\.\\.")[0]);
//            Long projectedFmax = Long.parseLong(locationString.split("\\.\\.")[1]);
            refSeqString = refSeqString.substring(0, refSeqString.lastIndexOf(":"));
        }
        JSONObject referenceProjection = JSONParser.parseStrict(refSeqString).isObject();
//        Long fmin = Math.round(referenceProjection.get(FeatureStringEnum.FMIN.getValue()).isNumber().doubleValue());
//        Long fmax = Math.round(referenceProjection.get(FeatureStringEnum.FMAX.getValue()).isNumber().doubleValue());


        JSONArray sequenceListArray ;
        if (referenceProjection.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isString() != null) {
            String sequenceString = referenceProjection.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isString().stringValue();
            sequenceListArray = JSONParser.parseStrict(sequenceString).isArray();

        } else {
            sequenceListArray = referenceProjection.get(FeatureStringEnum.SEQUENCE_LIST.getValue()).isArray();
        }

        AssemblageSequenceList assemblageSequenceList = AssemblageInfoConverter.convertJSONArrayToSequenceList(sequenceListArray);
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        assemblageInfo.setSequenceList(assemblageSequenceList);
        MultiSequenceProjection multiSequenceProjection = createProjectionFromAssemblageInfo(assemblageInfo);

        // TODO: filter for between fmin / fmax ?
        Integer startBase = Integer.parseInt(startBaseString);
        Integer endBase = Integer.parseInt(endBaseString);

        // these might be the same as above

        JSONArray foldPoints = getReversedFolds(multiSequenceProjection, startBase, endBase);
//        GWT.log(foldPoints.toString());

        return JsonUtils.safeEval(foldPoints.toString());
    }

    public static native void exportStaticMethod() /*-{
        $wnd.projectValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectValue(Ljava/lang/String;Ljava/lang/String;));
        $wnd.projectReverseValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectReverseValue(Ljava/lang/String;Ljava/lang/String;));
        $wnd.projectReverseSequence = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectReverseSequence(Ljava/lang/String;Ljava/lang/String;));
        $wnd.getReverseProjection = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getReverseProjection(Ljava/lang/String;Ljava/lang/String;));
        $wnd.getBorders = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getBorders(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        $wnd.getProjectionLength = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getProjectionLength(Ljava/lang/String;));
        $wnd.regionContainsFolds = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::regionContainsFolds(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        $wnd.projectFeatures = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectFeatures(Ljava/lang/String;Ljava/lang/String;));

        $wnd.foldSelectedTranscript = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::foldSelectedTranscript(Ljava/lang/String;Ljava/lang/String;));
        $wnd.foldBetweenExons = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::foldBetweenExons(Ljava/lang/String;Ljava/lang/String;));
        $wnd.removeFolds = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::removeFolds(Ljava/lang/String;Ljava/lang/String;));
        $wnd.getFolds = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getFolds(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        $wnd.getFoldsForRegion = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::getFoldsForRegion(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
    }-*/;

}
