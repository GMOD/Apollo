package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeaturePublication {

    static constraints = {
    }

//    Integer featurePublicationId;
    Publication publication;
    Feature feature;

    static hasMany = [
            featurePublicationProperties : FeaturePublicationProperty
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeaturePublication) ) return false;
        FeaturePublication castOther = ( FeaturePublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );

        return result;
    }

    public FeaturePublication generateClone() {
        FeaturePublication cloned = new FeaturePublication();
        cloned.publication = this.publication;
        cloned.feature = this.feature;
        cloned.featurePublicationProperties = this.featurePublicationProperties;
        return cloned;
    }
}
