package org.bbop.apollo

/**
 * In the ontology, this is a "is_a" relationship . .. not sure if it makes sense to keep it that way, though
 */
class Region extends SequenceFeature {

    static constraints = {
    }


    static String cvTerm = "region"
    static String ontologyId = "SO:0000001"// XX:NNNNNNN
    static String alternateCvTerm = "Region"

    // add convenience methods
}
