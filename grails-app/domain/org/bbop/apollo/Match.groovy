package org.bbop.apollo

/**
 * In the ontology, this is a "is_a" relationship . .. not sure if it makes sense to keep it that way, though
 */
class Match extends Region{

    static constraints = {
    }

    // added
    AnalysisFeature analysisFeature;

    static String ontologyId = "SO:0000343"// XX:NNNNNNN
    static String cvTerm = "Match"// may have a link

    // add convenience methods
}
