package org.bbop.apollo

class FeaturePhenotype {

    static constraints = {
    }

    Integer featurePhenotypeId;
    Feature feature;
    Phenotype phenotype;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof FeaturePhenotype) ) return false;
        FeaturePhenotype castOther = ( FeaturePhenotype ) other;

        return ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) ) && ( (this.getPhenotype()==castOther.getPhenotype()) || ( this.getPhenotype()!=null && castOther.getPhenotype()!=null && this.getPhenotype().equals(castOther.getPhenotype()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );
        result = 37 * result + ( getPhenotype() == null ? 0 : this.getPhenotype().hashCode() );
        return result;
    }

    public FeaturePhenotype generateClone() {
        FeaturePhenotype cloned = new FeaturePhenotype();
        cloned.feature = this.feature;
        cloned.phenotype = this.phenotype;
        return cloned;
    }

}
