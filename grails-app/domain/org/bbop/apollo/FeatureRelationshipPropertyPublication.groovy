package org.bbop.apollo

class FeatureRelationshipPropertyPublication {

    static constraints = {
    }

    Integer featureRelationshipPropertyPublicationId;
    Publication publication;
    FeatureRelationshipProperty featureRelationshipProperty;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureRelationshipPropertyPublication) ) return false;
        FeatureRelationshipPropertyPublication castOther = ( FeatureRelationshipPropertyPublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeatureRelationshipProperty()==castOther.getFeatureRelationshipProperty()) || ( this.getFeatureRelationshipProperty()!=null && castOther.getFeatureRelationshipProperty()!=null && this.getFeatureRelationshipProperty().equals(castOther.getFeatureRelationshipProperty()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeatureRelationshipProperty() == null ? 0 : this.getFeatureRelationshipProperty().hashCode() );
        return result;
    }

    public FeatureRelationshipPropertyPublication generateClone() {
        FeatureRelationshipPropertyPublication cloned = new FeatureRelationshipPropertyPublication();
        cloned.publication = this.publication;
        cloned.featureRelationshipProperty = this.featureRelationshipProperty;
        return cloned;
    }

}
