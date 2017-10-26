package org.bbop.apollo

class Sequence {

    static auditable = true

    static constraints = {
        name nullable: false
        start nullable: false
        end nullable: false
        organism nullable: true
        seqChunkSize nullable: true
    }


    // feature locations instead of features
    static hasMany = [
        featureLocations: FeatureLocation,
        sequenceChunks: SequenceChunk
    ]

    static mapping = {
        cache usage: 'read-only'
        end column: 'sequence_end'
        start column: 'sequence_start'
        featureLocations cascade: 'all-delete-orphan'
        sequenceChunks cascade: 'all-delete-orphan'
    }

    static belongsTo = [Organism]


    String name
    Organism organism
    Integer length
    Integer seqChunkSize
    Integer start
    Integer end
}
