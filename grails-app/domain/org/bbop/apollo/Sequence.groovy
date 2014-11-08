package org.bbop.apollo

/**
 * Replaces tracks in config.xml/tracks
 */
class Sequence {

    static auditable = true

    static constraints = {
        organism nullable: true
        refSeqFile nullable: true
        dataDirectory nullable: true
        translationTableLocation nullable: true
        spliceDonorSite nullable: true
        spliceAcceptor nullable: true
//        sequenceCV nullable: true
//        sequenceType nullable: true
//        sourceFeature nullable: true
    }


    // feature locations instead of features
    static hasMany = [
            users:User
            ,groups:GroupAnnotation
            , featureLocations: FeatureLocation
    ]

    static mapping = {
//        table "grails_sequence"
        end column: "sequence_end"
        start column: "sequence_start"

//        password column: "grails_password"
    }

    static belongsTo = [User]

    String name
    Organism organism

    String refSeqFile
    String dataDirectory
//    String sequenceType
//    String sequenceCV
//    String organismName


    String translationTableLocation
    String spliceDonorSite = "GT"
    String spliceAcceptor = "AG"

    // SourceFeature properties
//    FeatureLazyResidues sourceFeature
    Integer length
    Integer seqChunkSize
    String seqChunkPrefix
    Integer start
    Integer end
    String sequenceDirectory

}
