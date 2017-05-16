package org.bbop.apollo

class FeatureLocation {
    static mapping = {
        length formula: 'FMAX-FMIN'
    }
    static constraints = {
        feature nullable: false
        fmin nullable: false
        fmax nullable: false
        sequence nullable: false
        length nullable: true
        isFminPartial nullable: true
        isFmaxPartial nullable: true
        fminData nullable: true
        fmaxData nullable: true
        strand nullable: true
        phase nullable: true
        residueInfo nullable: true
        locgroup nullable: true
        rank nullable: true
    }

    Feature feature
    Integer fmin
    Integer length
    boolean isFminPartial
    String fminData
    Integer fmax
    boolean isFmaxPartial
    String fmaxData
    Integer strand
    Integer phase
    String residueInfo
    int locgroup
    int rank
    Sequence sequence

    static belongsTo = [Feature]


    static hasMany = [
            featureLocationPublications: Publication
    ]


    boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureLocation castOther = (FeatureLocation) other;

//        return ((this.getFeature() == castOther.getFeature()) || (this.getFeature() != null && castOther.getFeature() != null && this.getFeature().equals(castOther.getFeature()))) && (this.getLocgroup() == castOther.getLocgroup()) && (this.getRank() == castOther.getRank());
//        return
//        ( (this.getFeature() == castOther.getFeature()) || (this.getFeature() != null && castOther.getFeature() != null && this.getFeature().equals(castOther.getFeature())))
//        &&
//        return (this.sequence == castOther.sequence) && (this.getRank() == castOther.getRank()) && this.getLocgroup() == castOther.getLocgroup()) ;
        return  (this.getRank() == castOther.getRank())
//        && this.getLocgroup() == castOther.getLocgroup() ;
    }

    int hashCode() {
        int result = 17;

        result = 37 * result + (getFeature() == null ? 0 : this.getFeature().hashCode());

        result = 37 * result + this.getLocgroup();
        result = 37 * result + this.getRank();

        return result;
    }

    int getRank(){
        rank ?: 0
    }

    /**
     * We use this as an artificial accessor in case the property has not been calculatd
     * @return
     */
    Integer calculateLength(){
        return fmax-fmin
    }
    FeatureLocation generateClone() {
        FeatureLocation cloned = new FeatureLocation();
        cloned.sequence = this.sequence;
        cloned.feature = this.feature;
        cloned.fmin = this.fmin;
        cloned.isFminPartial = this.isFminPartial;
        cloned.fmax = this.fmax;
        cloned.isFmaxPartial = this.isFmaxPartial;
        cloned.fmaxData = this.fmaxData;
        cloned.fminData = this.fminData;
        cloned.strand = this.strand;
        cloned.phase = this.phase;
        cloned.residueInfo = this.residueInfo;
        cloned.locgroup = this.locgroup;
        cloned.rank = this.rank;
        cloned.featureLocationPublications = this.featureLocationPublications;
        return cloned;
    }

    FeatureLocation getPreviousFeatureLocation() {
        for(fl in feature.featureLocations){
            if(fl.rank == (rank-1)){
                return fl
            }
        }
        return null
    }

    FeatureLocation getNextFeatureLocation() {
        for (fl in feature.featureLocations) {
            if (fl.rank == (rank + 1)) {
                return fl
            }
        }
        return null
    }

}
