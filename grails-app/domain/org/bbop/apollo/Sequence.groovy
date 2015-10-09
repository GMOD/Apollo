package org.bbop.apollo

class Sequence {

    static auditable = true

    static constraints = {
        name nullable: false
        start nullable: false
        end nullable: false
        organism nullable: true
    }


    // feature locations instead of features
    static hasMany = [
        featureLocations: FeatureLocation,
    ]

    static mapping = {
        cache usage: 'read-only'
        end column: 'sequence_end'
        start column: 'sequence_start'
        featureLocations cascade: 'all-delete-orphan'
    }

    static belongsTo = [Organism]


    String name
    Organism organism

    // SourceFeature properties
    Integer length
    Integer seqChunkSize
    Integer start
    Integer end
}
