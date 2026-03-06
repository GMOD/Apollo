package org.bbop.apollo


class FeatureCVTerm {

    static constraints = {
    }

    Publication publication;
    Feature feature;
    CVTerm cvterm;
    boolean isNot;
    int rank;

    static hasMany = [
            featureCVTermPublications: Publication
            ,featureCVTermDBXrefs: DBXref
    ]



    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureCVTerm castOther = ( FeatureCVTerm ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && ( (this.getCvterm()==castOther.getCvterm()) || ( this.getCvterm()!=null && castOther.getCvterm()!=null && this.getCvterm().equals(castOther.getCvterm()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );
        result = 37 * result + ( getCvterm() == null ? 0 : this.getCvterm().hashCode() );
        result = 37 * result + this.getRank();
        return result;
    }

    public FeatureCVTerm generateClone() {
        FeatureCVTerm cloned = new FeatureCVTerm();
        cloned.publication = this.publication;
        cloned.feature = this.feature;
        cloned.cvterm = this.cvterm;
        cloned.isNot = this.isNot;
        cloned.rank = this.rank;
        cloned.featureCVTermPublications = this.featureCVTermPublications;
        cloned.featureCVTermDBXrefs = this.featureCVTermDBXrefs;
        return cloned;
    }
}
