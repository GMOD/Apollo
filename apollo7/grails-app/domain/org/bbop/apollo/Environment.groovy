package org.bbop.apollo

class Environment {

    static constraints = {
    }

    String uniquename;
    String description;

    static hasMany = [
           environmentCVTerms  : EnvironmentCVTerm
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Environment castOther = ( Environment ) other;

        return ( (this.getUniquename()==castOther.getUniquename()) || ( this.getUniquename()!=null && castOther.getUniquename()!=null && this.getUniquename().equals(castOther.getUniquename()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getUniquename() == null ? 0 : this.getUniquename().hashCode() );


        return result;
    }

    public Environment generateClone() {
        Environment cloned = new Environment();
        cloned.uniquename = this.uniquename;
        cloned.description = this.description;
        cloned.environmentCVTerms = this.environmentCVTerms;
        return cloned;
    }

}
