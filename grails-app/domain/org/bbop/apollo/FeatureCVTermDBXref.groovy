package org.bbop.apollo

class FeatureCVTermDBXref {

    static constraints = {
    }

    Integer featureCVTermDBXrefId;
    FeatureCVTerm featureCVTerm;
    DBXref dbxref;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureCVTermDBXref) ) return false;
        FeatureCVTermDBXref castOther = ( FeatureCVTermDBXref ) other;

        return ( (this.getFeatureCVTerm()==castOther.getFeatureCVTerm()) || ( this.getFeatureCVTerm()!=null && castOther.getFeatureCVTerm()!=null && this.getFeatureCVTerm().equals(castOther.getFeatureCVTerm()) ) ) && ( (this.getDbxref()==castOther.getDbxref()) || ( this.getDbxref()!=null && castOther.getDbxref()!=null && this.getDbxref().equals(castOther.getDbxref()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getFeatureCVTerm() == null ? 0 : this.getFeatureCVTerm().hashCode() );
        result = 37 * result + ( getDbxref() == null ? 0 : this.getDbxref().hashCode() );
        return result;
    }

    public FeatureCVTermDBXref generateClone() {
        FeatureCVTermDBXref cloned = new FeatureCVTermDBXref();
        cloned.featureCVTerm = this.featureCVTerm;
        cloned.dbxref = this.dbxref;
        return cloned;
    }
}
