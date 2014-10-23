package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureLocationPublication {

    static constraints = {
    }

     Integer featureLocationPublicationId;
     Publication publication;
     FeatureLocation featureLocation;

    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureLocationPublication) ) return false;
        FeatureLocationPublication castOther = ( FeatureLocationPublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getSingleFeatureLocation()==castOther.getSingleFeatureLocation()) || ( this.getSingleFeatureLocation()!=null && castOther.getSingleFeatureLocation()!=null && this.getSingleFeatureLocation().equals(castOther.getSingleFeatureLocation()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getSingleFeatureLocation() == null ? 0 : this.getSingleFeatureLocation().hashCode() );
        return result;
    }

    public FeatureLocationPublication generateClone() {
        FeatureLocationPublication cloned = new FeatureLocationPublication();
        cloned.publication = this.publication;
        cloned.featureLocation = this.featureLocation;
        return cloned;
    }
}
