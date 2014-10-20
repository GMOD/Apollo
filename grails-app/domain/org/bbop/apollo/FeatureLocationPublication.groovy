package org.bbop.apollo

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

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeatureLocation()==castOther.getFeatureLocation()) || ( this.getFeatureLocation()!=null && castOther.getFeatureLocation()!=null && this.getFeatureLocation().equals(castOther.getFeatureLocation()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeatureLocation() == null ? 0 : this.getFeatureLocation().hashCode() );
        return result;
    }

    public FeatureLocationPublication generateClone() {
        FeatureLocationPublication cloned = new FeatureLocationPublication();
        cloned.publication = this.publication;
        cloned.featureLocation = this.featureLocation;
        return cloned;
    }
}
