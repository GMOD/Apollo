package org.bbop.apollo

import java.util.ArrayList;

class Indel extends SequenceAlteration {

    static constraints = {
        minorAlleleFrequency min: 0.0F, max: 1.0F, scale: 3, nullable: true
    }

    String referenceBases
    String alternateBases
    Float minorAlleleFrequency
    static String cvTerm = "indel"
    static String ontologyId = "SO:1000032"
    static String alternateCvTerm = "indel"
}
