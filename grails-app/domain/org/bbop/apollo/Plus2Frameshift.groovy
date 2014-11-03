package org.bbop.apollo

/**
 * Note: top-level in the sequence ontology
 */
class Plus2Frameshift extends Frameshift{

    static constraints = {
    }

    static String ontologyId = "SO:0000860"// XX:NNNNNNN
    static String cvTerm = "Plus2Frameshift"

    // add convenience methods
    @Override
    boolean isPlusFrameshift() {
        return true
    }

    @Override
    int getFrameshiftValue() {
        return 2
    }
}
