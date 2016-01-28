package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class GoCountAnalysis implements Serializable {

    String cvname
    Integer cvtermId
    Integer analysisId
    Integer organismId
    Integer featureCount

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvname
        builder.append cvtermId
        builder.append analysisId
        builder.append organismId
        builder.append featureCount
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvname, other.cvname
        builder.append cvtermId, other.cvtermId
        builder.append analysisId, other.analysisId
        builder.append organismId, other.organismId
        builder.append featureCount, other.featureCount
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvname", "cvtermId", "analysisId", "organismId", "featureCount"]
        version false
    }

    static constraints = {
        cvname nullable: true
        cvtermId nullable: true
        analysisId nullable: true
        organismId nullable: true
        featureCount nullable: true
    }
}
