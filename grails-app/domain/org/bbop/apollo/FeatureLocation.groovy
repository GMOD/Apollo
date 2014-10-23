package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class FeatureLocation {

    static constraints = {
        feature nullable: false
        fmin nullable: false
        fmax nullable: false


        isFminPartial nullable: true
        featureLocationId nullable: true
        sourceFeature nullable: true
        isFmaxPartial nullable: true
        strand nullable: true
        phase nullable: true
        residueInfo nullable: true
        locgroup nullable: true
        rank nullable: true

    }

    Integer featureLocationId;
    Feature sourceFeature;
    Feature feature;
    Integer fmin;
    boolean isFminPartial;
    Integer fmax;
    boolean isFmaxPartial;
    Integer strand;
    Integer phase;
    String residueInfo;
    int locgroup;
    int rank;

//    Set<FeatureLocationPublication> featureLocationPublications = new HashSet<FeatureLocationPublication>(0);

    static hasMany = [
            featureLocationPublications: FeatureLocationPublication
    ]


    public boolean equals(Object other) {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof FeatureLocation)) return false;
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

    public FeatureLocation generateClone() {
        FeatureLocation cloned = new FeatureLocation();
        cloned.sourceFeature = this.sourceFeature;
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
