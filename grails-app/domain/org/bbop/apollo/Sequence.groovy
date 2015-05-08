package org.bbop.apollo

/**
 * Replaces tracks in config.xml/tracks
 */
class Sequence {

    static auditable = true

    static constraints = {
        name nullable: false
        start nullable: false
        end nullable: false
        organism nullable: true
        refSeqFile nullable: true
        translationTableLocation nullable: true
        spliceDonorSite nullable: true
        spliceAcceptor nullable: true
    }


    // feature locations instead of features
    static hasMany = [
            featureLocations: FeatureLocation
            , sequenceChunks: SequenceChunk
    ]

    static mapping = {
        end column: "sequence_end"
        start column: "sequence_start"
        featureLocations cascade: 'all-delete-orphan'
        sequenceChunks cascade: 'all-delete-orphan'
    }

    static belongsTo = [Organism]


    String name
    Organism organism

    // TODO: remove these as they should be redundant with organism
    String refSeqFile

    // TODO: remove these as they should be redundant with organism
    String translationTableLocation
    String spliceDonorSite = "GT"
    String spliceAcceptor = "AG"

    // SourceFeature properties
    Integer length
    Integer seqChunkSize
    String seqChunkPrefix
    Integer start
    Integer end
    // TODO: remove these as they should be redundant with organism
    String sequenceDirectory

}
