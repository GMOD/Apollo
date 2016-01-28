package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class StatsPathsToRoot implements Serializable {

    Integer cvtermId
    Long totalPaths
    BigDecimal avgDistance
    Integer minDistance
    Integer maxDistance

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvtermId
        builder.append totalPaths
        builder.append avgDistance
        builder.append minDistance
        builder.append maxDistance
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvtermId, other.cvtermId
        builder.append totalPaths, other.totalPaths
        builder.append avgDistance, other.avgDistance
        builder.append minDistance, other.minDistance
        builder.append maxDistance, other.maxDistance
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvtermId", "totalPaths", "avgDistance", "minDistance", "maxDistance"]
        version false
    }

    static constraints = {
        cvtermId nullable: true
        totalPaths nullable: true
        avgDistance nullable: true
        minDistance nullable: true
        maxDistance nullable: true
    }
}
