package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureCVTermProperty {

    static constraints = {
    }

    Integer featureCVTermPropertyId;
    CVTerm type;
    FeatureCVTerm featureCVTerm;
    String value;
    int rank;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureCVTermProperty) ) return false;
        FeatureCVTermProperty castOther = ( FeatureCVTermProperty ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getFeatureCVTerm()==castOther.getFeatureCVTerm()) || ( this.getFeatureCVTerm()!=null && castOther.getFeatureCVTerm()!=null && this.getFeatureCVTerm().equals(castOther.getFeatureCVTerm()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getFeatureCVTerm() == null ? 0 : this.getFeatureCVTerm().hashCode() );

        result = 37 * result + this.getRank();
        return result;
    }

    public FeatureCVTermProperty generateClone() {
        FeatureCVTermProperty cloned = new FeatureCVTermProperty();
        cloned.type = this.type;
        cloned.featureCVTerm = this.featureCVTerm;
        cloned.value = this.value;
        cloned.rank = this.rank;
        return cloned;
    }
}
