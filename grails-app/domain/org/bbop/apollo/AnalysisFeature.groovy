package org.bbop.apollo

class AnalysisFeature {

    static constraints = {
    }

    Analysis analysis;
    Feature feature;
    Double rawScore;
    Double normalizedScore;
    Double significance;
    Double identity;

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        AnalysisFeature castOther = ( AnalysisFeature ) other;

        return ( (this.getAnalysis()==castOther.getAnalysis()) || ( this.getAnalysis()!=null && castOther.getAnalysis()!=null && this.getAnalysis().equals(castOther.getAnalysis()) ) ) && ( (this.getFeature()==castOther.getFeature()) || ( this.getFeature()!=null && castOther.getFeature()!=null && this.getFeature().equals(castOther.getFeature()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getAnalysis() == null ? 0 : this.getAnalysis().hashCode() );
        result = 37 * result + ( getFeature() == null ? 0 : this.getFeature().hashCode() );




        return result;
    }

    public AnalysisFeature generateClone() {
        AnalysisFeature cloned = new AnalysisFeature();
        cloned.analysis = this.analysis;
        cloned.feature = this.feature;
        cloned.rawScore = this.rawScore;
        cloned.normalizedScore = this.normalizedScore;
        cloned.significance = this.significance;
        cloned.identity = this.identity;
        return cloned;
    }
}
