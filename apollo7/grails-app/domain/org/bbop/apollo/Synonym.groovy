package org.bbop.apollo

class Synonym {

    static constraints = {
    }

    String name;

    boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Synonym castOther = ( Synonym ) other;

        return (this.getName()==castOther.getName()) || ( this.getName()!=null && castOther.getName()!=null && this.getName().equals(castOther.getName()) )
    }

    int hashCode() {
        int result = 17;
        result = 37 * result + ( getName() == null ? 0 : this.getName().hashCode() );
        return result;
    }

    Synonym generateClone() {
        Synonym cloned = new Synonym();
        cloned.name = this.name;
        return cloned;
    }
}
