package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class Feature implements Ontological{

    static auditable = true

    static constraints = {

//        featureId nullable: true

        name nullable: false

        type nullable: true

        uniqueName nullable: true
        dbxref nullable: true
        organism nullable: true
        residues nullable: true
        sequenceLength nullable: true
        md5checksum nullable: true
        isAnalysis nullable: true
        isObsolete nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
        featureLocation nullable: true
    }

//    Integer featureId;
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

    Date dateCreated;
    Date lastUpdated ;

//    Date timeAccessioned;
//    Date timeLastModified;

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

    static mappedBy = [
            parentFeatureRelationships: "objectFeature"
            ,childFeatureRelationships: "subjectFeature"
            ,featureGenotypes: "feature"
            ,featureLocations: "feature"
    ]

    static belongsTo = [
            User
    ]


    public boolean equals(Object other) {
//        if ( (this == other ) ) return true;
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
        cloned.dateCreated = this.dateCreated;
        cloned.lastUpdated = this.lastUpdated;
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


    /** Convenience method for retrieving the location.  Assumes that it only contains a single
     *  location so it returns the first (and hopefully only) location from the collection of
     *  locations.  Returns <code>null</code> if none are found.
     *
     * @return FeatureLocation of this object
     */
    public FeatureLocation getSingleFeatureLocation() {
        Collection<FeatureLocation> locs = getFeatureLocations();
        if (locs != null) {
            Iterator<FeatureLocation> i = locs.iterator();
            if (i.hasNext()) {
                return i.next();
            }
        }
        return null;
    }


    /** Convenience method for setting the location.  Assumes that it only contains a single
     *  location so the previous location will be removed.
     *
     *  @param featureLocation - new FeatureLocation to set this gene to
     */
    public void setFeatureLocation(FeatureLocation featureLocation) {
        Collection<FeatureLocation> locs = getFeatureLocations();
        if (locs != null) {
            locs.clear();
        }
        featureLocations.add(featureLocation)
//        feature.addFeatureLocation(featureLocation);
    }


    /** Convenience method for retrieving the location.  Assumes that it only contains a single
     *  location so it returns the first (and hopefully only) location from the collection of
     *  locations.  Returns <code>null</code> if none are found.
     *
     * @return FeatureLocation of this object
     */
    public FeatureLocation getFeatureLocation() {
        Collection<FeatureLocation> locs = getFeatureLocations();
        if (locs != null) {
            Iterator<FeatureLocation> i = locs.iterator();
            if (i.hasNext()) {
                return i.next();
            }
        }
        return null;
    }


    /** Get the length of this feature.
     *
     * @return Length of feature
     */
    public int getLength() {
        return getFeatureLocation().getFmax() - getFeatureLocation().getFmin();
    }

    public Integer getFmin(){
        featureLocation.fmin
    }

    public Integer getFmax(){
        featureLocation.fmax
    }

    public Integer getStrand(){
        featureLocation.strand
    }


}
