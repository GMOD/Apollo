package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureProperty implements Ontological{

    static auditable =  true

    private static final String TAG_VALUE_DELIMITER = "=";

    static constraints = {
        type nullable: true
//        feature nullable: true
        value nullable: false // unique? . . or unique for feature?
        rank nullable: true
    }

//    Integer featurePropertyId;
    CVTerm type;
    // I think a FeatureProperty can be associated with more than one
    Feature feature;
    String value;
    int rank;

    static hasMany = [
            featurePropertyPublications :  Publication
    ]

    static belongsTo = [
            Feature
    ]

//    void addFeature(Feature feature){
//        features.add(feature)
//    }
//
//    Feature getSingleFeature(){
//            return features?.iterator()?.next()
//    }

    public boolean equals(Object other) {
//        if ( (this == other ) ) return true;
//        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureProperty) ) return false;
        FeatureProperty castOther = ( FeatureProperty ) other;

        if(castOther?.id == this?.id) return true
//        if(castOther.ontologyId != this.ontologyId) return false
        if(castOther?.rank != this?.rank) return false
        if(castOther?.value != this?.value) return false

//        if(castOther?.features?.size() != this?.features?.size()) return false
//
//        // iterate over them
//        for(Feature feature in castOther?.features){
//            if(this.features)
//        }


//        return ( (this.getType()==castOther.getType())
//                || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ))    \
//        && ( (this.getFeatures()==castOther.getFeatures())   \
//                || ( this.getFeatures()!=null && castOther.getFeatures()!=null && this.getFeatures().equals(castOther.getFeatures()) ) )   \
//                (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getValue() == null ? 0 : this.getValue().hashCode() );

        result = 37 * result + this.getRank();

        return result;
    }

//    public FeatureProperty generateClone() {
//        FeatureProperty cloned = new FeatureProperty();
//        cloned.type = this.type;
//        cloned.features = this.features;
//        cloned.value = this.value;
//        cloned.rank = this.rank;
//        cloned.featurePropertyPublications = this.featurePropertyPublications;
//        return cloned;
//    }

    String getValue(){
        if(value.contains(TAG_VALUE_DELIMITER)){
            return value.split(TAG_VALUE_DELIMITER)[1]
        }
        return value
    }

    String getTag() {
        if(value.contains(TAG_VALUE_DELIMITER)){
            return value.split(TAG_VALUE_DELIMITER)[0]
        }
        return ""
    }
}
