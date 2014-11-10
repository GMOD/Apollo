package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
class Pseudogene extends Gene{

    static constraints = {
    }

    static String ontologyId = "SO:0000336"// XX:NNNNNNN
    static String cvTerm = "Pseudogene"// may have a link

    static String alternateCvTerm = "pseudogene"
}
