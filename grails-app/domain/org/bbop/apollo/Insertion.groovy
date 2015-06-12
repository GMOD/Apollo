package org.bbop.apollo

class Insertion extends SequenceAlteration{


    static constraints = {
    }

    static String cvTerm  = "Insertion"
    static String ontologyId = "SO:0000667"
    static String alternateCvTerm = "insertion"

    @Override
    int getOffset() {
        return alterationResidue.length()
    }
}
