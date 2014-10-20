package org.bbop.apollo

class FeatureRelationshipPublication {

    static constraints = {
    }

    Integer featureRleationshipPublication;
    Publication publication;
    FeatureRelationship featureRelationship;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationshipPublication) ) return false;
        FeatureRelationshipPublication castOther = ( FeatureRelationshipPublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeatureRelationship()==castOther.getFeatureRelationship()) || ( this.getFeatureRelationship()!=null && castOther.getFeatureRelationship()!=null && this.getFeatureRelationship().equals(castOther.getFeatureRelationship()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeatureRelationship() == null ? 0 : this.getFeatureRelationship().hashCode() );
        return result;
    }

    public FeatureRelationshipPublication generateClone() {
        FeatureRelationshipPublication cloned = new FeatureRelationshipPublication();
        cloned.publication = this.publication;
        cloned.featureRelationship = this.featureRelationship;
        return cloned;
    }
}
