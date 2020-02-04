package org.bbop.apollo


import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Organism implements JsonMetadata {

    static auditable = true

    static constraints = {
        comment nullable: true
        abbreviation nullable: true
        species nullable: true
        genus nullable: true
        valid nullable: true
        blatdb nullable: true
        commonName nullable: false
        genomeFasta nullable: true
        obsolete nullable: true
        genomeFastaIndex nullable: true
        nonDefaultTranslationTable nullable: true, blank: false
        dataAddedViaWebServices nullable: true
        metadata(display: false, blank: true,nullable: true)
        officialGeneSetTrack nullable: true, blank: false
    }

    String abbreviation;
    String genus;
    String species;
    String commonName;
    String comment;
    Boolean valid;
    boolean publicMode;
    boolean obsolete
    String blatdb;
    String directory
    String genomeFasta
    String genomeFastaIndex
    String nonDefaultTranslationTable
    String metadata
    Boolean dataAddedViaWebServices
    String officialGeneSetTrack

    static hasMany = [
            organismProperties: OrganismProperty
            , organismDBXrefs : OrganismDBXref
            , sequences       : Sequence
            , userPermissions : UserOrganismPermission
            , groupPermissions: GroupOrganismPermission
    ]

    String getTrackList() {
        if (!directory) {
            return null
        } else {
            return directory + "/trackList.json"
        }
    }

    String getRefseqFile() {
        if (!directory) {
            return null
        } else {
            return directory + "/seq/refSeqs.json"
        }
    }

    String getGenomeFastaFileName() {
        return genomeFasta ? directory + File.separator + genomeFasta : null
    }

    String getGenomeFastaIndexFileName() {
        return genomeFastaIndex ? directory + File.separator + genomeFastaIndex : null
    }

    static mapping = {
        publicMode defaultValue: true
        obsolete defaultValue: false
        metadata type: 'text'
    }

}
