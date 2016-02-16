package org.bbop.apollo

class CVTermPath {

    static constraints = {
    }

     CVTerm type;
     CVTerm subjectCVTerm;
     CVTerm objectCVTerm;
     CV cv;
     Integer pathDistance;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        CVTermPath castOther = ( CVTermPath ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getSubjectCVTerm()==castOther.getSubjectCVTerm()) || ( this.getSubjectCVTerm()!=null && castOther.getSubjectCVTerm()!=null && this.getSubjectCVTerm().equals(castOther.getSubjectCVTerm()) ) ) && ( (this.getObjectCVTerm()==castOther.getObjectCVTerm()) || ( this.getObjectCVTerm()!=null && castOther.getObjectCVTerm()!=null && this.getObjectCVTerm().equals(castOther.getObjectCVTerm()) ) ) && ( (this.getPathDistance()==castOther.getPathDistance()) || ( this.getPathDistance()!=null && castOther.getPathDistance()!=null && this.getPathDistance().equals(castOther.getPathDistance()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getSubjectCVTerm() == null ? 0 : this.getSubjectCVTerm().hashCode() );

        result = 37 * result + ( getObjectCVTerm() == null ? 0 : this.getObjectCVTerm().hashCode() );
        result = 37 * result + ( getPathDistance() == null ? 0 : this.getPathDistance().hashCode() );
        return result;
    }

    public CVTermPath generateClone() {
        CVTermPath cloned = new CVTermPath();
        cloned.type = this.type;
        cloned.subjectCVTerm = this.subjectCVTerm;
        cloned.cv = this.cv;
        cloned.objectCVTerm = this.objectCVTerm;
        cloned.pathDistance = this.pathDistance;
        return cloned;
    }

}
