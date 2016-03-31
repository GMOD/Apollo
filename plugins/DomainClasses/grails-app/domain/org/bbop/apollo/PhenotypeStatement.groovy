package org.bbop.apollo

class PhenotypeStatement {

    static constraints = {
    }


    Environment environment;
    CVTerm type;
    Publication publication;
    Genotype genotype;
    Phenotype phenotype;



    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        PhenotypeStatement castOther = ( PhenotypeStatement ) other;

        return ( (this.getEnvironment()==castOther.getEnvironment()) || ( this.getEnvironment()!=null && castOther.getEnvironment()!=null && this.getEnvironment().equals(castOther.getEnvironment()) ) ) && ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getGenotype()==castOther.getGenotype()) || ( this.getGenotype()!=null && castOther.getGenotype()!=null && this.getGenotype().equals(castOther.getGenotype()) ) ) && ( (this.getPhenotype()==castOther.getPhenotype()) || ( this.getPhenotype()!=null && castOther.getPhenotype()!=null && this.getPhenotype().equals(castOther.getPhenotype()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getEnvironment() == null ? 0 : this.getEnvironment().hashCode() );
        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getGenotype() == null ? 0 : this.getGenotype().hashCode() );
        result = 37 * result + ( getPhenotype() == null ? 0 : this.getPhenotype().hashCode() );
        return result;
    }

    public PhenotypeStatement generateClone() {
        PhenotypeStatement cloned = new PhenotypeStatement();
        cloned.environment = this.environment;
        cloned.type = this.type;
        cloned.publication = this.publication;
        cloned.genotype = this.genotype;
        cloned.phenotype = this.phenotype;
        return cloned;
    }
}
