package org.bbop.apollo

class CVTerm {

    static constraints = {
    }
    CV cv;
    DBXref dbxref;
    String name;
    String definition;
    int isObsolete;
    int isRelationshipType;

    static hasMany = [
            childCVTermRelationships   : CVTermRelationship
            , parentCVTermRelationships: CVTermRelationship
            , parentCVTermPaths        : CVTermPath
            , childCVTermPaths         : CVTermPath
    ]

    static mappedBy = [
            parentCVTermRelationships : "objectCVTerm"
            , childCVTermRelationships: "subjectCVTerm"
            , parentCVTermPaths       : "subjectCVTerm"
            , childCVTermPaths        : "objectCVTerm"
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        CVTerm castOther = (CVTerm) other;

        return ((this.getCv() == castOther.getCv()) || (this.getCv() != null && castOther.getCv() != null && this.getCv().equals(castOther.getCv()))) && ((this.getName() == castOther.getName()) || (this.getName() != null && castOther.getName() != null && this.getName().equals(castOther.getName()))) && (this.getIsObsolete() == castOther.getIsObsolete());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + (getCv() == null ? 0 : this.getCv().hashCode());

        result = 37 * result + (getName() == null ? 0 : this.getName().hashCode());

        result = 37 * result + this.getIsObsolete();





        return result;
    }

    public CVTerm generateClone() {
        CVTerm cloned = new CVTerm();
        cloned.cv = this.cv;
        cloned.dbxref = this.dbxref;
        cloned.name = this.name;
        cloned.definition = this.definition;
        cloned.isObsolete = this.isObsolete;
        cloned.isRelationshipType = this.isRelationshipType;
        cloned.childCVTermRelationships = this.childCVTermRelationships;
        cloned.parentCVTermRelationships = this.parentCVTermRelationships;
        cloned.parentCVTermPaths = this.parentCVTermPaths;
        cloned.childCVTermPaths = this.childCVTermPaths;
        return cloned;
    }
}
