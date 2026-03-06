package org.bbop.apollo

class EnvironmentCVTerm {

    static constraints = {
    }

    Environment environment;
    CVTerm cvterm;

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        EnvironmentCVTerm castOther = ( EnvironmentCVTerm ) other;

        return ( (this.getEnvironment()==castOther.getEnvironment()) || ( this.getEnvironment()!=null && castOther.getEnvironment()!=null && this.getEnvironment().equals(castOther.getEnvironment()) ) ) && ( (this.getCvterm()==castOther.getCvterm()) || ( this.getCvterm()!=null && castOther.getCvterm()!=null && this.getCvterm().equals(castOther.getCvterm()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getEnvironment() == null ? 0 : this.getEnvironment().hashCode() );
        result = 37 * result + ( getCvterm() == null ? 0 : this.getCvterm().hashCode() );
        return result;
    }

    public EnvironmentCVTerm generateClone() {
        EnvironmentCVTerm cloned = new EnvironmentCVTerm();
        cloned.environment = this.environment;
        cloned.cvterm = this.cvterm;
        return cloned;
    }

}
