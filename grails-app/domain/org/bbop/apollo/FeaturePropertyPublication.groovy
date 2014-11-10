package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeaturePropertyPublication {

    static constraints = {
    }

//    Integer featurePropertyPublicationId;
    Publication publication;
    FeatureProperty featureProperty;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeaturePropertyPublication) ) return false;
        FeaturePropertyPublication castOther = ( FeaturePropertyPublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeatureProperty()==castOther.getFeatureProperty()) || ( this.getFeatureProperty()!=null && castOther.getFeatureProperty()!=null && this.getFeatureProperty().equals(castOther.getFeatureProperty()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeatureProperty() == null ? 0 : this.getFeatureProperty().hashCode() );
        return result;
    }

    public FeaturePropertyPublication generateClone() {
        FeaturePropertyPublication cloned = new FeaturePropertyPublication();
        cloned.publication = this.publication;
        cloned.featureProperty = this.featureProperty;
        return cloned;
    }
}
