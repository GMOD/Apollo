package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class Gff3view implements Serializable {

    Integer featureId
    String ref
    String source
    String type
    Integer fstart
    Integer fend
    String score
    String strand
    String phase
    Integer seqlen
    String name
    Integer organismId

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append featureId
        builder.append ref
        builder.append source
        builder.append type
        builder.append fstart
        builder.append fend
        builder.append score
        builder.append strand
        builder.append phase
        builder.append seqlen
        builder.append name
        builder.append organismId
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append featureId, other.featureId
        builder.append ref, other.ref
        builder.append source, other.source
        builder.append type, other.type
        builder.append fstart, other.fstart
        builder.append fend, other.fend
        builder.append score, other.score
        builder.append strand, other.strand
        builder.append phase, other.phase
        builder.append seqlen, other.seqlen
        builder.append name, other.name
        builder.append organismId, other.organismId
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["featureId", "ref", "source", "type", "fstart", "fend", "score", "strand", "phase", "seqlen", "name", "organismId"]
        version false
    }

    static constraints = {
        featureId nullable: true
        ref nullable: true
        source nullable: true
        type nullable: true, maxSize: 1024
        fstart nullable: true
        fend nullable: true
        score nullable: true
        strand nullable: true
        phase nullable: true
        seqlen nullable: true
        name nullable: true
        organismId nullable: true
    }
}
