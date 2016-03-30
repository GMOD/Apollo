package org.bbop.apollo

class CVTermRelationship {

    static constraints = {
    }

     CVTerm type;
     CVTerm subjectCVTerm;
     CVTerm objectCVTerm;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        CVTermRelationship castOther = ( CVTermRelationship ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getSubjectCVTerm()==castOther.getSubjectCVTerm()) || ( this.getSubjectCVTerm()!=null && castOther.getSubjectCVTerm()!=null && this.getSubjectCVTerm().equals(castOther.getSubjectCVTerm()) ) ) && ( (this.getObjectCVTerm()==castOther.getObjectCVTerm()) || ( this.getObjectCVTerm()!=null && castOther.getObjectCVTerm()!=null && this.getObjectCVTerm().equals(castOther.getObjectCVTerm()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getSubjectCVTerm() == null ? 0 : this.getSubjectCVTerm().hashCode() );
        result = 37 * result + ( getObjectCVTerm() == null ? 0 : this.getObjectCVTerm().hashCode() );
        return result;
    }

    public CVTermRelationship generateClone() {
        CVTermRelationship cloned = new CVTermRelationship();
        cloned.type = this.type;
        cloned.subjectCVTerm = this.subjectCVTerm;
        cloned.objectCVTerm = this.objectCVTerm;
        return cloned;
    }

}
