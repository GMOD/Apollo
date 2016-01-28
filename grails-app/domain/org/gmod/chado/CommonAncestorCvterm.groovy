package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CommonAncestorCvterm implements Serializable {

    Integer cvterm1Id
    Integer cvterm2Id
    Integer ancestorCvtermId
    Integer pathdistance1
    Integer pathdistance2
    Integer totalPathdistance

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvterm1Id
        builder.append cvterm2Id
        builder.append ancestorCvtermId
        builder.append pathdistance1
        builder.append pathdistance2
        builder.append totalPathdistance
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvterm1Id, other.cvterm1Id
        builder.append cvterm2Id, other.cvterm2Id
        builder.append ancestorCvtermId, other.ancestorCvtermId
        builder.append pathdistance1, other.pathdistance1
        builder.append pathdistance2, other.pathdistance2
        builder.append totalPathdistance, other.totalPathdistance
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvterm1Id", "cvterm2Id", "ancestorCvtermId", "pathdistance1", "pathdistance2", "totalPathdistance"]
        version false
    }

    static constraints = {
        cvterm1Id nullable: true
        cvterm2Id nullable: true
        ancestorCvtermId nullable: true
        pathdistance1 nullable: true
        pathdistance2 nullable: true
        totalPathdistance nullable: true
    }
}
