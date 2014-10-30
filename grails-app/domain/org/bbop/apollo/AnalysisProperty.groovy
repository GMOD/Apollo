package org.bbop.apollo

class AnalysisProperty {

    static constraints = {
    }

//    Integer analysisPropertyId;
    Analysis analysis;
    CVTerm type;
    String value;


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof AnalysisProperty) ) return false;
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
