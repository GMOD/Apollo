package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureRelationship {

    static auditable =  true

    static constraints = {
    }

//    Integer featureRelationshipId;
    CVTerm type;
    Feature objectFeature; // parent?
    Feature subjectFeature; // child?
    String value;
    int rank;

    static hasMany = [
            featureRelationshipProperties : FeatureRelationshipProperty
            ,featureRelationshipPublications: FeatureRelationshipPublication
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationship) ) return false;
        FeatureRelationship castOther = ( FeatureRelationship ) other;

        return ( (this.type==castOther.type) || ( this.type!=null && castOther.type!=null && this.type.equals(castOther.type) ) ) && ( (this.getObjectFeature()==castOther.getObjectFeature()) || ( this.getObjectFeature()!=null && castOther.getObjectFeature()!=null && this.getObjectFeature().equals(castOther.getObjectFeature()) ) ) && ( (this.getSubjectFeature()==castOther.getSubjectFeature()) || ( this.getSubjectFeature()!=null && castOther.getSubjectFeature()!=null && this.getSubjectFeature().equals(castOther.getSubjectFeature()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( type == null ? 0 : this.type.hashCode() );
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
