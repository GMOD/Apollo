package org.bbop.apollo

class FeatureGenotype {

    static constraints = {
    }

    Integer featureGenotypeId;
    Genotype genotype;
    Feature feature;
    CVTerm cvterm;
    Feature chromosomeFeature;
    int rank;
    int cgroup;



    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureGenotype castOther = ( FeatureGenotype ) other;

        return ( (this.getGenotype()==castOther.getGenotype()) || ( this.getGenotype()!=null && castOther.getGenotype()!=null && this.getGenotype().equals(castOther.getGenotype()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && ( (this.getCvterm()==castOther.getCvterm()) || ( this.getCvterm()!=null && castOther.getCvterm()!=null && this.getCvterm().equals(castOther.getCvterm()) ) ) && ( (this.getChromosomeFeature()==castOther.getChromosomeFeature()) || ( this.getChromosomeFeature()!=null && castOther.getChromosomeFeature()!=null && this.getChromosomeFeature().equals(castOther.getChromosomeFeature()) ) ) && (this.getRank()==castOther.getRank()) && (this.getCgroup()==castOther.getCgroup());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getGenotype() == null ? 0 : this.getGenotype().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );
        result = 37 * result + ( getCvterm() == null ? 0 : this.getCvterm().hashCode() );
        result = 37 * result + ( getChromosomeFeature() == null ? 0 : this.getChromosomeFeature().hashCode() );
        result = 37 * result + this.getRank();
        result = 37 * result + this.getCgroup();
        return result;
    }

    public FeatureGenotype generateClone() {
        FeatureGenotype cloned = new FeatureGenotype();
        cloned.genotype = this.genotype;
        cloned.feature = this.feature;
        cloned.cvterm = this.cvterm;
        cloned.chromosomeFeature = this.chromosomeFeature;
        cloned.rank = this.rank;
        cloned.cgroup = this.cgroup;
        return cloned;
    }
}
