package org.bbop.apollo

import org.bbop.apollo.sequence.Strand

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



    public FeatureLocation getFirstFeatureLocation() {
        List<FeatureLocation> locs = getFeatureLocations().sort(){ a,b ->
            a.rank <=> b.rank ?:  a.fmin  <=> b.fmin ?: a.length ?: b.length
        };
        return locs ? locs.first() : null
    }

    public FeatureLocation getLastFeatureLocation() {
        List<FeatureLocation> locs = getFeatureLocations().sort(){  a,b ->
            a.rank <=> b.rank ?:  a.fmin  <=> b.fmin ?: a.length ?: b.length
        };
        return locs ? locs.last() : null
    }

    public String getSequenceNames(){

        if(!featureLocations) {
            return "None"
        }

        String returnName = ""

        featureLocations.eachWithIndex { FeatureLocation entry, int i ->
            returnName += entry.sequence.name
            if(i < featureLocations.size()-1){
                returnName += "::"
            }
        }

        return returnName
    }

    /** Get the length of this feature.
     *
     * @return Length of feature
     */
    public int getLength() {
        getFmax()-getFmin()
//        return getFeatureLocation().calculateLength()
    }

    /**
     * Returns the calculated fmin in the given sequence?!? so first rank
     * @return
     */
    public Integer getFmin(){
        if(!featureLocations){
            throw new Exception("No feature locations exist for feature")
        }
        featureLocations.sort(){ it.rank }.first().fmin
    }

    public Organism getOrganism(){
        if(!featureLocations){
            return null
        }
        else{
            return getFirstFeatureLocation().sequence.organism
        }
    }

    /**
     * Returns the calculated fmax if part of multiple scaffolds
     *
     * This is the fmax of the last featureLength, plus the some of all previous featureLoctaion sequences
     * @return
     */
    public Integer getFmax(){
//        featureLocation.fmax
        Integer calculatedMax = 0
        int maxRank = getMaxRank()
        featureLocations.sort(){ it.rank }.each {
            if(it.rank<maxRank){
                calculatedMax += it.sequence.length
            }
            else{
                calculatedMax += it.fmax
            }
        }
        return calculatedMax
//        featureLocations.sort(){ it.rank }.last().fmax
    }

    /**
     * Rteturn the rank of the last feature location
     * @return
     */
    public Integer getMaxRank(){
        featureLocations.sort(){ it.rank }.last().rank
    }

    /**
     * This will always be 0, but good to include it
     * @return
     */
    public Integer getMinRank(){
        featureLocations.sort(){ it.rank }.first().rank
    }

    /**
     * TODO: we should validate the strands of all of the feature locations here?
     * @return
     */
    public Integer getStrand(){
        featureLocations.first().strand
    }

    public Boolean isNegativeStrand(){
        for(fl in featureLocations){
            if(fl.strand!=Strand.NEGATIVE.value){
                return false
            }
        }
        return true
    }

    public Boolean isPositiveStrand(){
        for(fl in featureLocations){
            if(fl.strand!=Strand.POSITIVE.value){
                return false
            }
        }
        return true
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

    Sequence getFirstSequence() {
        if(featureLocations){
            return featureLocations.sort(){ it.rank }.first().sequence
        }
        else{
            return null
        }
    }


    Sequence getLastSequence() {
        if(featureLocations){
            return featureLocations.sort(){ it.rank }.last().sequence
        }
        else{
            return null
        }
    }

    FeatureLocation getFeatureLocationForPosition(int position) {
        int currentPosition = 0
        for(FeatureLocation featureLocation in featureLocations.sort(){it.rank}){
            if(position>=currentPosition && position < featureLocation.sequence.end){
                return featureLocation
            }
        }
        return null
    }
}
