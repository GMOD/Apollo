package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureRelationshipProperty {

    static auditable =  true

    static constraints = {
    }

    Integer featureRelationshipPropertyId;
    CVTerm type;
    FeatureRelationship featureRelationship;
    String value;
    int rank;
//    Set<FeatureRelationshipPropertyPublication> featureRelationshipPropertyPublications = new HashSet<FeatureRelationshipPropertyPublication>(0);

    static hasMany = [
            featureRelationshipPropertyPublications:FeatureRelationshipPropertyPublication
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationshipProperty) ) return false;
        FeatureRelationshipProperty castOther = ( FeatureRelationshipProperty ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getFeatureRelationship()==castOther.getFeatureRelationship()) || ( this.getFeatureRelationship()!=null && castOther.getFeatureRelationship()!=null && this.getFeatureRelationship().equals(castOther.getFeatureRelationship()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getFeatureRelationship() == null ? 0 : this.getFeatureRelationship().hashCode() );

        result = 37 * result + this.getRank();

        return result;
    }

    public FeatureRelationshipProperty generateClone() {
        FeatureRelationshipProperty cloned = new FeatureRelationshipProperty();
        cloned.type = this.type;
        cloned.featureRelationship = this.featureRelationship;
        cloned.value = this.value;
        cloned.rank = this.rank;
        cloned.featureRelationshipPropertyPublications = this.featureRelationshipPropertyPublications;
        return cloned;
    }
}
