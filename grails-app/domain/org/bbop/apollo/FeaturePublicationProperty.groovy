package org.bbop.apollo

class FeaturePublicationProperty {

    static constraints = {
    }

    Integer featurePublicationPropertyId;
    CVTerm type;
    FeaturePublication featurePublication;
    String value;
    int rank;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeaturePublicationProperty) ) return false;
        FeaturePublicationProperty castOther = ( FeaturePublicationProperty ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getFeaturePublication()==castOther.getFeaturePublication()) || ( this.getFeaturePublication()!=null && castOther.getFeaturePublication()!=null && this.getFeaturePublication().equals(castOther.getFeaturePublication()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getFeaturePublication() == null ? 0 : this.getFeaturePublication().hashCode() );

        result = 37 * result + this.getRank();
        return result;
    }

    public FeaturePublicationProperty generateClone() {
        FeaturePublicationProperty cloned = new FeaturePublicationProperty();
        cloned.type = this.type;
        cloned.featurePublication = this.featurePublication;
        cloned.value = this.value;
        cloned.rank = this.rank;
        return cloned;
    }

}
