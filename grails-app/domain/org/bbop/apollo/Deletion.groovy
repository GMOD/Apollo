package org.bbop.apollo

class Deletion extends SequenceAlteration{

    static constraints = {
    }


    Integer deletionLength
    static String cvTerm = "deletion"
    static String ontologyId = "SO:0000159"
    static String alternateCvTerm = "Deletion"

    @Override
    int getOffset() {
        return deletionLength
    }
}
