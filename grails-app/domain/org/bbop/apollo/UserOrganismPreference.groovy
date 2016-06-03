package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
        nativeTrackList nullable: true
        sequence nullable: true, blank: false
        startbp nullable: true, blank: false
        endbp nullable: true, blank: false
    }

    Organism organism
    Boolean currentOrganism  // this means the "active" client token
    Boolean nativeTrackList
    Sequence sequence
    Integer startbp
    Integer endbp

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof UserOrganismPreference)) return false

        UserOrganismPreference that = (UserOrganismPreference) o

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
