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
    Integer startbp
    Integer endbp

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof UserOrganismPreferenceDTO)) return false

        UserOrganismPreferenceDTO that = (UserOrganismPreferenceDTO) o

        if (id != that.id) return false
        if (organism != that.organism) return false
        if (sequence != that.sequence) return false

        return true
    }

    int hashCode() {
        int result
        result = organism.hashCode()
        result = 31 * result + sequence.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}
