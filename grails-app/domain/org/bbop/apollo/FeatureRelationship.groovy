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
    Feature parentFeature; // parent  / object
    Feature childFeature; // child   / subject
    String value; // unused, but could be used like metadata (strength / quality of connection)
    int rank;
    static String ontologyId = "part_of"
    
//    static belongsTo = [parentFeature: Feature]

    static hasMany = [
            featureRelationshipProperties : FeatureProperty
            ,featureRelationshipPublications: Publication
    ]


    public boolean equals(Object other) {
//        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationship) ) return false;
        FeatureRelationship castOther = ( FeatureRelationship ) other;
        if(this?.id == castOther?.id) return true

        return  this.parentFeature ==castOther.parentFeature  \
                && this.childFeature ==  castOther.childFeature
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + ( parentFeature == null ? 0 : this.parentFeature.hashCode() );
        result = 37 * result + ( childFeature == null ? 0 : this.childFeature.hashCode() );

        result = 37 * result + this.rank;


        return result;
    }

    public FeatureRelationship generateClone() {
        FeatureRelationship cloned = new FeatureRelationship();
//        cloned.type = this.type;
        cloned.parentFeature = this.parentFeature;
        cloned.childFeature = this.childFeature;
        cloned.value = this.value;
        cloned.rank = this.rank;
        cloned.featureRelationshipProperties = this.featureRelationshipProperties;
        cloned.featureRelationshipPublications = this.featureRelationshipPublications;
        return cloned;
    }
}
