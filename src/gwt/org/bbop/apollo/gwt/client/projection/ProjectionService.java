package org.bbop.apollo.gwt.client.projection;

import org.bbop.apollo.projection.*;

/**
 * This class is responsible for generating projection libraries.
 *
 * Created by nathandunn on 10/10/16.
 */
public class ProjectionService {


    MultiSequenceProjection getProjectionForString(String projectionString){
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
        return null ;
    }

    Integer projectValue(Integer input,String referenceString){
        MultiSequenceProjection projection = getProjectionForString(referenceString);
        return projection.projectValue( input);
    }

    public static native void exportStaticMethod() /*-{
        @wnd.projectValue = $entry(@org.bbop.apollo.gwt.client.projection.ProjectionService::projectValue(L/java/lang/Double;Ljava/lang/String;));
    }-*/;
}
