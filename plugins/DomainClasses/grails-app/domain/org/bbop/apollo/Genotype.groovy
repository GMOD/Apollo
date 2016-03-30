package org.bbop.apollo


class Genotype {

    static constraints = {
    }

    Integer genotypeId;
    String name;
    String uniqueName;
    String description;

    static hasMany = [
           phenotypeDescriptions  : PhenotypeDescription
            ,featureGenotypes: FeatureGenotype
            ,phenotypeStatements : PhenotypeStatement
    ]

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Genotype castOther = ( Genotype ) other;

        return ( (this.getUniqueName()==castOther.getUniqueName()) || ( this.getUniqueName()!=null && castOther.getUniqueName()!=null && this.getUniqueName().equals(castOther.getUniqueName()) ) );
    }

    public int hashCode() {
        int result = 17;



        result = 37 * result + ( getUniqueName() == null ? 0 : this.getUniqueName().hashCode() );




        return result;
    }

    public Genotype generateClone() {
        Genotype cloned = new Genotype();
        cloned.name = this.name;
        cloned.uniqueName = this.uniqueName;
        cloned.description = this.description;
        cloned.phenotypeDescriptions = this.phenotypeDescriptions;
        cloned.featureGenotypes = this.featureGenotypes;
        cloned.phenotypeStatements = this.phenotypeStatements;
        return cloned;
    }
}
