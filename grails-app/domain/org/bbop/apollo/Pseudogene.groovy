package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
class Pseudogene extends BiologicalRegion{

    static constraints = {
    }

    String ontologyId = "SO:0000336"// XX:NNNNNNN
    String cvTerm = "Pseudogene"// may have a link
}
