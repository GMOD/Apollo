package org.bbop.apollo

class FeatureDBXref {

    static constraints = {
    }
    Integer featureDBXrefId;
    Feature feature;
    DBXref dbxref;
    boolean isCurrent;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureDBXref) ) return false;
        FeatureDBXref castOther = ( FeatureDBXref ) other;

        return ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && ( (this.getDbxref()==castOther.getDbxref()) || ( this.getDbxref()!=null && castOther.getDbxref()!=null && this.getDbxref().equals(castOther.getDbxref()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );
        result = 37 * result + ( getDbxref() == null ? 0 : this.getDbxref().hashCode() );

        return result;
    }

    public FeatureDBXref generateClone() {
        FeatureDBXref cloned = new FeatureDBXref();
        cloned.feature = this.feature;
        cloned.dbxref = this.dbxref;
        cloned.isCurrent = this.isCurrent;
        return cloned;
    }
}
