package org.bbop.apollo

class OrganismProperty {

    static constraints = {
    }

    Integer organismId;
    String abbreviation;
    String genus;
    String species;
    String commonName;
    String comment;

    static hasMany = [
        organismProperties : OrganismProperty
        ,organismDBXrefs : OrganismDBXref
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        OrganismProperty castOther = ( OrganismProperty ) other;

        return ( (this.getGenus()==castOther.getGenus()) || ( this.getGenus()!=null && castOther.getGenus()!=null && this.getGenus().equals(castOther.getGenus()) ) ) && ( (this.getSpecies()==castOther.getSpecies()) || ( this.getSpecies()!=null && castOther.getSpecies()!=null && this.getSpecies().equals(castOther.getSpecies()) ) );
    }

    public int hashCode() {
        int result = 17;



        result = 37 * result + ( getGenus() == null ? 0 : this.getGenus().hashCode() );
        result = 37 * result + ( getSpecies() == null ? 0 : this.getSpecies().hashCode() );




        return result;
    }

    public Organism generateClone() {
        Organism cloned = new Organism();
        cloned.abbreviation = this.abbreviation;
        cloned.genus = this.genus;
        cloned.species = this.species;
        cloned.commonName = this.commonName;
        cloned.comment = this.comment;
        cloned.organismProperties = this.organismProperties;
        cloned.organismDBXrefs = this.organismDBXrefs;
        return cloned;
    }
}
