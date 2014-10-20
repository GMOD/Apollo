package org.bbop.apollo

class Feature {

    static constraints = {
    }

    Integer featureId;
    CVTerm type;
    DBXref dbxref;
    Organism organism;
    String name;
    String uniqueName;
    String residues;
    Integer sequenceLength;
    String md5checksum;
    boolean isAnalysis;
    boolean isObsolete;
    Date timeAccessioned;
    Date timeLastModified;
//    Set<FeatureLocation> featureLocations = new HashSet<FeatureLocation>(0);
//    Set<FeatureGenotype> featureGenotypes = new HashSet<FeatureGenotype>(0);
//    Set<FeatureRelationship> parentFeatureRelationships = new HashSet<FeatureRelationship>(0);
//    Set<FeatureRelationship> childFeatureRelationships = new HashSet<FeatureRelationship>(0);
//    Set<FeatureCVTerm> featureCVTerms = new HashSet<FeatureCVTerm>(0);
//    Set<FeatureSynonym> featureSynonyms = new HashSet<FeatureSynonym>(0);
//    Set<FeatureDBXref> featureDBXrefs = new HashSet<FeatureDBXref>(0);
//    Set<FeaturePublication> featurePublications = new HashSet<FeaturePublication>(0);
//    Set<FeaturePhenotype> featurePhenotypes = new HashSet<FeaturePhenotype>(0);
//    Set<FeatureProperty> featureProperties = new HashSet<FeatureProperty>(0);

    static hasMany = [
            featureLocations: FeatureLocation
            ,featureGenotypes: FeatureGenotype
            ,parentFeatureRelationships: FeatureRelationship
            ,childFeatureRelationships: FeatureRelationship
            ,featureCVTerms: FeatureCVTerm
            ,featureSynonyms: FeatureSynonym
            ,featureDBXrefs: FeatureDBXref
            ,featurePublications:FeaturePublication
            ,featurePhenotypes: FeaturePhenotype
            ,featureProperties: FeatureProperty
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof Feature) ) return false;
        Feature castOther = ( Feature ) other;

        return ( (this.getType()==castOther.getType()) || ( this.getType()!=null && castOther.getType()!=null && this.getType().equals(castOther.getType()) ) ) && ( (this.getOrganism()==castOther.getOrganism()) || ( this.getOrganism()!=null && castOther.getOrganism()!=null && this.getOrganism().equals(castOther.getOrganism()) ) ) && ( (this.getUniqueName()==castOther.getUniqueName()) || ( this.getUniqueName()!=null && castOther.getUniqueName()!=null && this.getUniqueName().equals(castOther.getUniqueName()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getType() == null ? 0 : this.getType().hashCode() );

        result = 37 * result + ( getOrganism() == null ? 0 : this.getOrganism().hashCode() );

        result = 37 * result + ( getUniqueName() == null ? 0 : this.getUniqueName().hashCode() );



        return result;
    }

    public Feature generateClone() {
        Feature cloned = new Feature();
        cloned.type = this.type;
        cloned.dbxref = this.dbxref;
        cloned.organism = this.organism;
        cloned.name = this.name;
        cloned.uniqueName = this.uniqueName;
        cloned.residues = this.residues;
        cloned.sequenceLength = this.sequenceLength;
        cloned.md5checksum = this.md5checksum;
        cloned.isAnalysis = this.isAnalysis;
        cloned.isObsolete = this.isObsolete;
        cloned.timeAccessioned = this.timeAccessioned;
        cloned.timeLastModified = this.timeLastModified;
        cloned.featureLocations = this.featureLocations;
        cloned.featureGenotypes = this.featureGenotypes;
        cloned.parentFeatureRelationships = this.parentFeatureRelationships;
        cloned.childFeatureRelationships = this.childFeatureRelationships;
        cloned.featureCVTerms = this.featureCVTerms;
        cloned.featureSynonyms = this.featureSynonyms;
        cloned.featureDBXrefs = this.featureDBXrefs;
        cloned.featurePublications = this.featurePublications;
        cloned.featurePhenotypes = this.featurePhenotypes;
        cloned.featureProperties = this.featureProperties;
        return cloned;
    }



    @Override
    public String toString() {
        return String.format("%s (%s)", getUniqueName(), getType());
    }

    public String getResidues(int fmin, int fmax) {
        if (getResidues() != null) {
            return getResidues().substring(fmin, fmax);
        }
        return null;
    }
}
