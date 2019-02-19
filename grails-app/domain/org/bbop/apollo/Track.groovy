package org.bbop.apollo

/**
 * Created by nathandunn on 2/19/2019
 *
 * This stores detail about a track that could potentially over-ride existing track behavior in the file-system.
 * For example permissions.
 */
class Track {

    static constraints = {
        isPublic nullable: true
        key nullable: false
        organism nullable: false
    }
    Boolean isPublic
    String key
    Organism organism


}
