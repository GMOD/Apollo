package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class PhenotypeCVTerm {

    static constraints = {
    }

    Integer phenotypeCVTermId;
    CVTerm cvterm;
    Phenotype phenotype;
    int rank;

    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof PhenotypeCVTerm) ) return false;
        PhenotypeCVTerm castOther = ( PhenotypeCVTerm ) other;

        return ( (this.getCvterm()==castOther.getCvterm()) || ( this.getCvterm()!=null && castOther.getCvterm()!=null && this.getCvterm().equals(castOther.getCvterm()) ) ) && ( (this.getPhenotype()==castOther.getPhenotype()) || ( this.getPhenotype()!=null && castOther.getPhenotype()!=null && this.getPhenotype().equals(castOther.getPhenotype()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getCvterm() == null ? 0 : this.getCvterm().hashCode() );
        result = 37 * result + ( getPhenotype() == null ? 0 : this.getPhenotype().hashCode() );
        result = 37 * result + this.getRank();
        return result;
    }

    public PhenotypeCVTerm generateClone() {
        PhenotypeCVTerm cloned = new PhenotypeCVTerm();
        cloned.cvterm = this.cvterm;
        cloned.phenotype = this.phenotype;
        cloned.rank = this.rank;
        return cloned;
    }


}
