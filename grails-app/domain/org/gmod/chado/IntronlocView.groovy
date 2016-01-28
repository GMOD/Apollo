package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class IntronlocView implements Serializable {

    Integer exon1Id
    Integer exon2Id
    Integer fmin
    Integer fmax
    Short strand
    Integer srcfeatureId

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append exon1Id
        builder.append exon2Id
        builder.append fmin
        builder.append fmax
        builder.append strand
        builder.append srcfeatureId
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append exon1Id, other.exon1Id
        builder.append exon2Id, other.exon2Id
        builder.append fmin, other.fmin
        builder.append fmax, other.fmax
        builder.append strand, other.strand
        builder.append srcfeatureId, other.srcfeatureId
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["exon1Id", "exon2Id", "fmin", "fmax", "strand", "srcfeatureId"]
        version false
    }

    static constraints = {
        exon1Id nullable: true
        exon2Id nullable: true
        fmin nullable: true
        fmax nullable: true
        strand nullable: true
        srcfeatureId nullable: true
    }
}
