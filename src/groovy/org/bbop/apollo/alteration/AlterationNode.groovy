package org.bbop.apollo.alteration

import groovy.json.JsonBuilder
import org.bbop.apollo.Allele
import org.bbop.apollo.SequenceAlteration

/**
 * Created by deepak.unni3 on 2/8/17.
 */
class AlterationNode {
    String uniquename
    int fmin
    int fmax
    String alterationResidue
    String type
    String alterationType
    int offset
    int cumulativeOffset // the sum of all offsets before the current alteration
    ArrayList<OverlapInfo> overlapInfo

    public AlterationNode(SequenceAlteration sequenceAlteration) {
        this.uniquename = sequenceAlteration.uniqueName
        this.fmin = sequenceAlteration.fmin
        this.fmax = sequenceAlteration.fmax
        this.alterationResidue = sequenceAlteration.alterationResidue
        this.type = sequenceAlteration.class.simpleName
        this.alterationType = sequenceAlteration.alterationType
        this.offset = getOffset(sequenceAlteration)
    }

    public AlterationNode(SequenceAlteration sequenceAlteration, Allele allele) {
        this.uniquename = sequenceAlteration.uniqueName
        this.fmin = sequenceAlteration.fmin
        this.fmax = sequenceAlteration.fmax
        this.alterationResidue = allele.alterationResidue
        this.type = sequenceAlteration.class.simpleName
        this.alterationType = sequenceAlteration.alterationType
        this.offset = getOffset(allele)
    }

    public int getOffset(SequenceAlteration sequenceAlteration) {
        int offset
        if (this.type == "Insertion") {
            offset = sequenceAlteration.alterationResidue.length()
        }
        else if (this.type == "Deletion") {
            offset = -(this.fmax - this.fmin)
        }
        else {
            offset = 0
        }
        return offset
    }

    public int getOffset(Allele allele) {
        int offset
        if (this.type == "Insertion") {
            offset = allele.alterationResidue.length()
        }
        else if (this.type == "Deletion") {
            offset = -(this.fmax - this.fmin)
        }
        else {
            offset = 0
        }
        return offset
    }

    @Override
    public String toString() {
        return new JsonBuilder( this ).toPrettyString()
    }
}


