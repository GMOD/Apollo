package org.bbop.apollo

class FeatureRelationship implements  Ontological{

    static auditable =  true

    static constraints = {
        rank nullable: true
        value nullable: true
    }

    Feature parentFeature;
    Feature childFeature;
    String value; // unused, but could be used like metadata (strength / quality of connection)
    int rank;
    static String ontologyId = "part_of"
    
    static hasMany = [
            featureRelationshipProperties : FeatureProperty
            ,featureRelationshipPublications: Publication
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
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
        cloned.parentFeature = this.parentFeature;
        cloned.childFeature = this.childFeature;
        cloned.value = this.value;
        cloned.rank = this.rank;
        cloned.featureRelationshipProperties = this.featureRelationshipProperties;
        cloned.featureRelationshipPublications = this.featureRelationshipPublications;
        return cloned;
    }
}
