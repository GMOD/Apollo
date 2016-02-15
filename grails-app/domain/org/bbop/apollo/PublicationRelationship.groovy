package org.bbop.apollo

class PublicationRelationship {

    static constraints = {
    }

    Publication subjectPublication;
    Publication objectPublication;
    CVTerm type;

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        PublicationRelationship castOther = (PublicationRelationship) other;

        return ((this.getSubjectPublication() == castOther.getSubjectPublication()) || (this.getSubjectPublication() != null && castOther.getSubjectPublication() != null && this.getSubjectPublication().equals(castOther.getSubjectPublication()))) && ((this.getType() == castOther.getType()) || (this.getType() != null && castOther.getType() != null && this.getType().equals(castOther.getType()))) && ((this.getObjectPublication() == castOther.getObjectPublication()) || (this.getObjectPublication() != null && castOther.getObjectPublication() != null && this.getObjectPublication().equals(castOther.getObjectPublication())));
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + (getSubjectPublication() == null ? 0 : this.getSubjectPublication().hashCode());
        result = 37 * result + (getType() == null ? 0 : this.getType().hashCode());
        result = 37 * result + (getObjectPublication() == null ? 0 : this.getObjectPublication().hashCode());
        return result;
    }

    public PublicationRelationship generateClone() {
        PublicationRelationship cloned = new PublicationRelationship();
        cloned.subjectPublication = this.subjectPublication;
        cloned.type = this.type;
        cloned.objectPublication = this.objectPublication;
        return cloned;
    }
}
