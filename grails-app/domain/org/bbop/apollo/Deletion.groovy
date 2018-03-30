package org.bbop.apollo

class Deletion extends SequenceAlteration{

    static constraints = {
    }

    Integer deletionLength

    static String cvTerm  = "Deletion"
    static String ontologyId = "SO:0000159"
    static String alternateCvTerm = "deletion"

    @Override
    int getOffset() {
        return deletionLength
    }
}
