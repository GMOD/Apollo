package org.bbop.apollo

class Analysis {

    static constraints = {
    }

    String name;
    String description;
    String program;
    String programVersion;
    String algorithm;
    String sourceName;
    String sourceVersion;
    String sourceURI;
    Date timeExecuted;

    static hasMany = [
            analysisFeatures : AnalysisFeature
            ,analysisProperties : AnalysisProperty
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Analysis castOther = ( Analysis ) other;

        return ( (this.getProgram()==castOther.getProgram()) || ( this.getProgram()!=null && castOther.getProgram()!=null && this.getProgram().equals(castOther.getProgram()) ) ) && ( (this.getProgramVersion()==castOther.getProgramVersion()) || ( this.getProgramVersion()!=null && castOther.getProgramVersion()!=null && this.getProgramVersion().equals(castOther.getProgramVersion()) ) ) && ( (this.getSourceName()==castOther.getSourceName()) || ( this.getSourceName()!=null && castOther.getSourceName()!=null && this.getSourceName().equals(castOther.getSourceName()) ) );
    }

    public int hashCode() {
        int result = 17;




        result = 37 * result + ( getProgram() == null ? 0 : this.getProgram().hashCode() );
        result = 37 * result + ( getProgramVersion() == null ? 0 : this.getProgramVersion().hashCode() );

        result = 37 * result + ( getSourceName() == null ? 0 : this.getSourceName().hashCode() );





        return result;
    }

    public Analysis generateClone() {
        Analysis cloned = new Analysis();
        cloned.name = this.name;
        cloned.description = this.description;
        cloned.program = this.program;
        cloned.programVersion = this.programVersion;
        cloned.algorithm = this.algorithm;
        cloned.sourceName = this.sourceName;
        cloned.sourceVersion = this.sourceVersion;
        cloned.sourceURI = this.sourceURI;
        cloned.timeExecuted = this.timeExecuted;
        cloned.analysisFeatures = this.analysisFeatures;
        cloned.analysisProperties = this.analysisProperties;
        return cloned;
    }
}
