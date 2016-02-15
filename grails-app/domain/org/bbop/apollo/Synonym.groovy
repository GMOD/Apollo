package org.bbop.apollo

class Synonym {

    static constraints = {
    }


    CVTerm type;
    String name;
    String synonymSGML;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Synonym castOther = ( Synonym ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getName()==castOther.getName()) || ( this.getName()!=null && castOther.getName()!=null && this.getName().equals(castOther.getName()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getName() == null ? 0 : this.getName().hashCode() );

        return result;
    }

    public Synonym generateClone() {
        Synonym cloned = new Synonym();
        cloned.type = this.type;
        cloned.name = this.name;
        cloned.synonymSGML = this.synonymSGML;
        return cloned;
    }
}
