package org.bbop.apollo

/**
 * Note: top-level in the sequence ontology
 */
class Plus1Frameshift extends Frameshift{

    static constraints = {
    }

    String ontologyId = "SO:0000868"// XX:NNNNNNN
    String cvTerm = "Plus1Frameshift"

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
