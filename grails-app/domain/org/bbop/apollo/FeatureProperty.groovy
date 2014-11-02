package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureProperty {

    static auditable =  true

    static constraints = {
        type nullable: false
//        feature nullable: true
        value nullable: false
        rank nullable: true
    }

//    Integer featurePropertyId;
    CVTerm type;
    // I think a FeatureProperty can be associated with more than one
//    Feature feature;
    String value;
    int rank;

    static hasMany = [
            featurePropertyPublications :  FeaturePropertyPublication
            ,features: Feature
    ]

    static belongsTo = [
            Feature
    ]

    void addFeature(Feature feature){
        features.add(feature)
    }

    Feature getSingleFeature(){
            return features?.iterator()?.next()
    }

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
