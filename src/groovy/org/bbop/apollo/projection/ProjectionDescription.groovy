package org.bbop.apollo.projection

/**
 * A description of features
 * Created by nathandunn on 9/24/15.
 */
class ProjectionDescription {

    public final static String ALL_FEATURES = "ALL"

    List<String> referenceTracks // typically one
    List<ProjectionSequence> sequenceList // an ordered array of sequences
    String type
    List<String> featureNames // a list of Features  // dfeault is a single entry ALL
    Integer padding // the padding around the reference

}
