package org.bbop.apollo

/**
 * Note: top-level in the sequence ontology
 */
class Minus1Frameshift extends Frameshift{

    static constraints = {
    }

    static String ontologyId = "SO:0000866"// XX:NNNNNNN
    static String cvTerm = "Minus1Frameshift"

    // add convenience methods
    @Override
    boolean isPlusFrameshift() {
        return false
    }

    @Override
    int getFrameshiftValue() {
        return 1
    }
}
