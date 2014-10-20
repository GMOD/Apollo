package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureRelationship {

    static constraints = {
    }

    private Integer featureRelationshipId;
    private CVTerm type;
    private Feature objectFeature;
    private Feature subjectFeature;
    private String value;
    private int rank;
//    private Set<FeatureRelationshipProperty> featureRelationshipProperties = new HashSet<FeatureRelationshipProperty>(0);
//    private Set<FeatureRelationshipPublication> featureRelationshipPublications = new HashSet<FeatureRelationshipPublication>(0);

    static hasMany = [
            featureRelationshipProperties : FeatureRelationshipProperty
            ,featureRelationshipPublications: FeatureRelationshipPublication
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationship) ) return false;
        FeatureRelationship castOther = ( FeatureRelationship ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getObjectFeature()==castOther.getObjectFeature()) || ( this.getObjectFeature()!=null && castOther.getObjectFeature()!=null && this.getObjectFeature().equals(castOther.getObjectFeature()) ) ) && ( (this.getSubjectFeature()==castOther.getSubjectFeature()) || ( this.getSubjectFeature()!=null && castOther.getSubjectFeature()!=null && this.getSubjectFeature().equals(castOther.getSubjectFeature()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getObjectFeature() == null ? 0 : this.getObjectFeature().hashCode() );
        result = 37 * result + ( getSubjectFeature() == null ? 0 : this.getSubjectFeature().hashCode() );

        result = 37 * result + this.getRank();


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
