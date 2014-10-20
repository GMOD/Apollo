package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureCVTermPublication {

    static constraints = {
    }

    Integer featureCVTermPublicationId;
    Publication publication;
    FeatureCVTerm featureCVTerm;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeatureCVTermPublication) ) return false;
        FeatureCVTermPublication castOther = ( FeatureCVTermPublication ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeatureCVTerm()==castOther.getFeatureCVTerm()) || ( this.getFeatureCVTerm()!=null && castOther.getFeatureCVTerm()!=null && this.getFeatureCVTerm().equals(castOther.getFeatureCVTerm()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeatureCVTerm() == null ? 0 : this.getFeatureCVTerm().hashCode() );
        return result;
    }

    public FeatureCVTermPublication generateClone() {
        FeatureCVTermPublication cloned = new FeatureCVTermPublication();
        cloned.publication = this.publication;
        cloned.featureCVTerm = this.featureCVTerm;
        return cloned;
    }
}
