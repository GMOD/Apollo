package org.bbop.apollo

class AnalysisProperty {

    static constraints = {
    }

    Analysis analysis;
    CVTerm type;
    String value;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        AnalysisProperty castOther = ( AnalysisProperty ) other;

        return ( (this.getAnalysis()==castOther.getAnalysis()) || ( this.getAnalysis()!=null && castOther.getAnalysis()!=null && this.getAnalysis().equals(castOther.getAnalysis()) ) ) && ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getValue()==castOther.getValue()) || ( this.getValue()!=null && castOther.getValue()!=null && this.getValue().equals(castOther.getValue()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getAnalysis() == null ? 0 : this.getAnalysis().hashCode() );
        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );
        result = 37 * result + ( getValue() == null ? 0 : this.getValue().hashCode() );
        return result;
    }

    public AnalysisProperty generateClone() {
        AnalysisProperty cloned = new AnalysisProperty();
        cloned.analysis = this.analysis;
        cloned.type = this.type;
        cloned.value = this.value;
        return cloned;
    }
}
