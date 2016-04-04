package org.bbop.apollo

class FeatureProperty implements Ontological{

    static auditable =  true

    private static final String TAG_VALUE_DELIMITER = "=";

    static constraints = {
        type nullable: true
//        feature nullable: true
        value nullable: false // unique? . . or unique for feature?
        rank nullable: true
        tag nullable: true,blank: false  // this will be null for generic properties
    }

//    Integer featurePropertyId;
    CVTerm type;
    // I think a FeatureProperty can be associated with more than one
    Feature feature;
    String tag;
    String value;
    int rank;

    static hasMany = [
            featurePropertyPublications :  Publication
    ]

    static belongsTo = [
            Feature
    ]

    static mapping = {
        value type: 'text'
    }

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureProperty castOther = ( FeatureProperty ) other;

        if(castOther?.id == this?.id) return true
        if(castOther?.rank != this?.rank) return false
        if(castOther?.value != this?.value) return false
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getValue() == null ? 0 : this.getValue().hashCode() );

        result = 37 * result + this.getRank();

        return result;
    }

}
