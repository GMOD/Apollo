package org.bbop.apollo

/**
 * Replaces tracks in config.xml/tracks
 */
class Track {

    static constraints = {
        genome nullable: true
        refSeqFile nullable: true
        dataDirectory nullable: true
        organismName nullable: true
        translationTableLocation nullable: true
    }

    static hasMany = [
            users:User
    ]

    static belongsTo = [User]

    String name
    Genome genome

    String refSeqFile
    String dataDirectory
    String sequenceType
    String sequenceCV
    String organismName


    String translationTableLocation
    String spliceDonorSite = "GT"
    String spliceAcceptor = "AG"
//    String refSeqFile
}
