package org.bbop.apollo

class Feature implements Ontological{

    static auditable = true

    static constraints = {
        name nullable: false
        uniqueName nullable: false
        dbxref nullable: true
        sequenceLength nullable: true
        md5checksum nullable: true
        isAnalysis nullable: true
        isObsolete nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
        symbol nullable: true // TODO: should be false and unique per organism
        description nullable: true
        status nullable: true
    }

    String symbol
    String description
    DBXref dbxref;
    String name;
    String uniqueName;
    Integer sequenceLength;
    String md5checksum;
    Status status
    boolean isAnalysis;
    boolean isObsolete;
    Date dateCreated;
    Date lastUpdated ;

    static hasMany = [
            featureLocations: FeatureLocation
            ,featureGenotypes: FeatureGenotype
            ,parentFeatureRelationships: FeatureRelationship  // relationships where I am the parent feature relationship
            ,childFeatureRelationships: FeatureRelationship // relationships where I am the child feature relationship
            ,featureCVTerms: FeatureCVTerm
            ,featureSynonyms: FeatureSynonym
            ,featureDBXrefs: DBXref
            ,featurePublications: Publication
            ,featurePhenotypes: Phenotype
            ,featureProperties: FeatureProperty
            ,synonyms: Synonym
            ,owners:User
    ]

    static mappedBy = [
            parentFeatureRelationships: "parentFeature",
            childFeatureRelationships: "childFeature",
            featureGenotypes: "feature",
            featureLocations: "feature"
    ]
    
    static mapping = {
            childFeatureRelationships cascade: 'all-delete-orphan'
            parentFeatureRelationships cascade: 'all-delete-orphan'
            featureLocations cascade: 'all-delete-orphan' // lazy: false  since most / all feature locations have a single element join is more efficient
            name type: 'text'
            description type: 'text'
    }


    static belongsTo = [
            User
    ]
    
    public User getOwner(){
        if(owners?.size()>0){
            return owners.iterator().next()
        }
        return null
    }


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Feature castOther = ( Feature ) other;

        return  (this?.ontologyId==castOther?.ontologyId) \
                   &&  (this?.getUniqueName()==castOther?.getUniqueName())
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + ( ontologyId == null ? 0 : this.ontologyId.hashCode() );
        result = 37 * result + ( getUniqueName() == null ? 0 : this.getUniqueName().hashCode() );
        return result;
    }

    public Feature generateClone() {
        Feature cloned = this.getClass().newInstance()
        cloned.dbxref = this.dbxref;
        cloned.name = this.name;
        cloned.uniqueName = this.uniqueName;
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



    /** Convenience method for retrieving the location.  Assumes that it only contains a single
     *  location so it returns the first (and hopefully only) location from the collection of
     *  locations.  Returns <code>null</code> if none are found.
     *
     * @return FeatureLocation of this object
     */
    public FeatureLocation getFeatureLocation() {
        Collection<FeatureLocation> locs = getFeatureLocations();
        return locs ? locs.first() : null
    }


    /** Get the length of this feature.
     *
     * @return Length of feature
     */
    public int getLength() {
        return getFeatureLocation().calculateLength()
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


    @Override
    public String toString() {
        return "Feature{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", uniqueName='" + uniqueName + '\'' +
                ", sequenceLength=" + sequenceLength +
                ", status=" + status +
                ", dateCreated=" + dateCreated +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
