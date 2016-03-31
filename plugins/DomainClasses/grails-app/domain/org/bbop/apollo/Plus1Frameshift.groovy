package org.bbop.apollo

/**
 * Note: top-level in the sequence ontology
 */
class Plus1Frameshift extends Frameshift{

    static constraints = {
    }

    static String ontologyId = "SO:0000868"// XX:NNNNNNN
    static String cvTerm = "Plus1Frameshift"

    // add convenience methods
    @Override
    boolean isPlusFrameshift() {
        return true
    }

    @Override
    int getFrameshiftValue() {
        return 1
    }
}
