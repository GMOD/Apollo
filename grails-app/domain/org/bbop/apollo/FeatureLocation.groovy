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
    Integer fmax
    boolean isFmaxPartial
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


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        FeatureLocation castOther = (FeatureLocation) other;

        return ((this.getFeature() == castOther.getFeature()) || (this.getFeature() != null && castOther.getFeature() != null && this.getFeature().equals(castOther.getFeature()))) && (this.getLocgroup() == castOther.getLocgroup()) && (this.getRank() == castOther.getRank());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + (getFeature() == null ? 0 : this.getFeature().hashCode());

        result = 37 * result + this.getLocgroup();
        result = 37 * result + this.getRank();

        return result;
    }

    /**
     * We use this as an artificial accessor in case the property has not been calculatd
     * @return
     */
    public Integer calculateLength(){
        return fmax-fmin
    }
    public FeatureLocation generateClone() {
        FeatureLocation cloned = new FeatureLocation();
        cloned.sequence = this.sequence;
        cloned.feature = this.feature;
        cloned.fmin = this.fmin;
        cloned.isFminPartial = this.isFminPartial;
        cloned.fmax = this.fmax;
        cloned.isFmaxPartial = this.isFmaxPartial;
        cloned.strand = this.strand;
        cloned.phase = this.phase;
        cloned.residueInfo = this.residueInfo;
        cloned.locgroup = this.locgroup;
        cloned.rank = this.rank;
        cloned.featureLocationPublications = this.featureLocationPublications;
        return cloned;
    }

}
