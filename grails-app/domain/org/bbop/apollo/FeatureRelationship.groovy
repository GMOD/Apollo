package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureRelationship implements  Ontological{

    static auditable =  true

    static constraints = {
//        type nullable: true
        rank nullable: true
        value nullable: true
    }

//    Integer featureRelationshipId;
//    CVTerm type;
    Feature objectFeature; // parent?
    Feature subjectFeature; // child?
    String value; // unused, but could be used like metadata (strength / quality of connection)
    int rank;
    static String ontologyId = "part_of"

    static hasMany = [
            featureRelationshipProperties : FeatureRelationshipProperty
            ,featureRelationshipPublications: FeatureRelationshipPublication
    ]


    public boolean equals(Object other) {
//        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationship) ) return false;
        FeatureRelationship castOther = ( FeatureRelationship ) other;
        if(this?.id == castOther?.id) return true

        return  this.objectFeature ==castOther.objectFeature  \
                && this.subjectFeature ==  castOther.subjectFeature
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + ( objectFeature == null ? 0 : this.objectFeature.hashCode() );
        result = 37 * result + ( subjectFeature == null ? 0 : this.subjectFeature.hashCode() );

        result = 37 * result + this.rank;


        return result;
    }

    public FeatureRelationship generateClone() {
        FeatureRelationship cloned = new FeatureRelationship();
        cloned.type = this.type;
        cloned.objectFeature = this.objectFeature;
        cloned.subjectFeature = this.subjectFeature;
        cloned.value = this.value;
        cloned.rank = this.rank;
        cloned.featureRelationshipProperties = this.featureRelationshipProperties;
        cloned.featureRelationshipPublications = this.featureRelationshipPublications;
        return cloned;
    }
}
