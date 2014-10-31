package org.bbop.apollo

class SequenceAlteration extends SequenceFeature{


    static constraints = {
    }

    String cvTerm  = "SequenceAlteration"
    String ontologyId = "SO:0001059"

    /** Get the offset added by the sequence alteration.
     *
     * @return Offset added by the sequence alteration
     */
    public int getOffset() {
        return 0;
    }

}
