package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class Dfeatureloc implements Serializable {

    Integer featurelocId
    Integer featureId
    Integer srcfeatureId
    Integer nbeg
    Boolean isNbegPartial
    Integer nend
    Boolean isNendPartial
    Short strand
    Integer phase
    String residueInfo
    Integer locgroup
    Integer rank

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append featurelocId
        builder.append featureId
        builder.append srcfeatureId
        builder.append nbeg
        builder.append isNbegPartial
        builder.append nend
        builder.append isNendPartial
        builder.append strand
        builder.append phase
        builder.append residueInfo
        builder.append locgroup
        builder.append rank
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append featurelocId, other.featurelocId
        builder.append featureId, other.featureId
        builder.append srcfeatureId, other.srcfeatureId
        builder.append nbeg, other.nbeg
        builder.append isNbegPartial, other.isNbegPartial
        builder.append nend, other.nend
        builder.append isNendPartial, other.isNendPartial
        builder.append strand, other.strand
        builder.append phase, other.phase
        builder.append residueInfo, other.residueInfo
        builder.append locgroup, other.locgroup
        builder.append rank, other.rank
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["featurelocId", "featureId", "srcfeatureId", "nbeg", "isNbegPartial", "nend", "isNendPartial", "strand", "phase", "residueInfo", "locgroup", "rank"]
        version false
    }

    static constraints = {
        featurelocId nullable: true
        featureId nullable: true
        srcfeatureId nullable: true
        nbeg nullable: true
        isNbegPartial nullable: true
        nend nullable: true
        isNendPartial nullable: true
        strand nullable: true
        phase nullable: true
        residueInfo nullable: true
        locgroup nullable: true
        rank nullable: true
    }
}
