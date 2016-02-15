package org.bbop.apollo

class OrganismDBXref {

    static constraints = {
    }

     DBXref dbxref;
     Organism organism;

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        OrganismDBXref castOther = ( OrganismDBXref ) other;

        return ( (this.getDbxref()==castOther.getDbxref()) || ( this.getDbxref()!=null && castOther.getDbxref()!=null && this.getDbxref().equals(castOther.getDbxref()) ) ) && ( (this.getOrganism()==castOther.getOrganism()) || ( this.getOrganism()!=null && castOther.getOrganism()!=null && this.getOrganism().equals(castOther.getOrganism()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getDbxref() == null ? 0 : this.getDbxref().hashCode() );
        result = 37 * result + ( getOrganism() == null ? 0 : this.getOrganism().hashCode() );
        return result;
    }

    public OrganismDBXref generateClone() {
        OrganismDBXref cloned = new OrganismDBXref();
        cloned.dbxref = this.dbxref;
        cloned.organism = this.organism;
        return cloned;
    }

}
