package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class OrganismFeatureCount implements Serializable {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    Integer organismId
    String genus
    String species
    String commonName
    Integer numFeatures
    Integer cvtermId
    String featureType

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append organismId
        builder.append genus
        builder.append species
        builder.append commonName
        builder.append numFeatures
        builder.append cvtermId
        builder.append featureType
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append organismId, other.organismId
        builder.append genus, other.genus
        builder.append species, other.species
        builder.append commonName, other.commonName
        builder.append numFeatures, other.numFeatures
        builder.append cvtermId, other.cvtermId
        builder.append featureType, other.featureType
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["organismId", "genus", "species", "commonName", "numFeatures", "cvtermId", "featureType"]
        version false
    }

    static constraints = {
        organismId nullable: true
        genus nullable: true
        species nullable: true
        commonName nullable: true
        numFeatures nullable: true
        cvtermId nullable: true
        featureType nullable: true
    }
}
