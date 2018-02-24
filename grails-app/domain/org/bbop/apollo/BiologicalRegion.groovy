package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
abstract class BiologicalRegion extends Region{

    static constraints = {
    }

    static String cvTerm = "biological_region"
    static String ontologyId = "SO:0001411"// XX:NNNNNNN
    static String alternateCvTerm = "BiologicalRegion"


}
