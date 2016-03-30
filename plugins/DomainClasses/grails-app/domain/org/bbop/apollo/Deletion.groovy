package org.bbop.apollo

class Deletion extends SequenceAlteration{

    Integer deletionLength

    static constraints = {
    }

    static String cvTerm  = "Deletion"
    static String ontologyId = "SO:0000159"
    static String alternateCvTerm = "deletion"

    @Override
    int getOffset() {
        // TODO: add offset
        return deletionLength
    }
}
