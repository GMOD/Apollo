package org.bbop.apollo.preference


/**
 * This class mirrors UserOrganismPreference, but NEVER persists, making it lighter-weight
 */
class UserOrganismPreferenceDTO {

    Long id
    OrganismDTO organism
    Boolean currentOrganism  // this means the "active" client token
    Boolean nativeTrackList
    SequenceDTO sequence
    UserDTO user
    Integer startbp
    Integer endbp
    String clientToken


    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof UserOrganismPreferenceDTO)) return false

        UserOrganismPreferenceDTO that = (UserOrganismPreferenceDTO) o

//        if (id != that.id) return false
//        if (organism != that.organism) return false
//        if (sequence != that.sequence) return false
//        if (user != that.user) return false
        if (clientToken != that.clientToken) return false

        return true
    }

    int hashCode() {
        int result = clientToken.hashCode()
//        result = organism.hashCode()
//        result = 31 * result + sequence.hashCode()
//        result = 31 * result + id.hashCode()
//        result = 31 * result + user.hashCode()
        return result
    }
}
