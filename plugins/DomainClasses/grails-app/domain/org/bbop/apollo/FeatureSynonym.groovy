package org.bbop.apollo


class FeatureSynonym {

    static constraints = {
    }

    Publication publication;
    Feature feature;
    Synonym synonym;
    boolean isCurrent;
    boolean isInternal;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureSynonym castOther = ( FeatureSynonym ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && ( (this.getSynonym()==castOther.getSynonym()) || ( this.getSynonym()!=null && castOther.getSynonym()!=null && this.getSynonym().equals(castOther.getSynonym()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );
        result = 37 * result + ( getSynonym() == null ? 0 : this.getSynonym().hashCode() );


        return result;
    }

    public FeatureSynonym generateClone() {
        FeatureSynonym cloned = new FeatureSynonym();
        cloned.publication = this.publication;
        cloned.feature = this.feature;
        cloned.synonym = this.synonym;
        cloned.isCurrent = this.isCurrent;
        cloned.isInternal = this.isInternal;
        return cloned;
    }
}
