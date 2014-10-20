package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureProperty {

    static constraints = {
    }

    Integer featurePropertyId;
    CVTerm type;
    Feature feature;
    String value;
    int rank;
//    Set<FeaturePropertyPublication> featurePropertyPublications = new HashSet<FeaturePropertyPublication>(0);

    static hasMany = [
            featurePropertyPublications :  FeaturePropertyPublication
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureProperty) ) return false;
        FeatureProperty castOther = ( FeatureProperty ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );

        result = 37 * result + this.getRank();

        return result;
    }

    public FeatureProperty generateClone() {
        FeatureProperty cloned = new FeatureProperty();
        cloned.type = this.type;
        cloned.feature = this.feature;
        cloned.value = this.value;
        cloned.rank = this.rank;
        cloned.featurePropertyPublications = this.featurePropertyPublications;
        return cloned;
    }

}
