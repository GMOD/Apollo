package org.bbop.apollo

class FeatureRelationship {

    static constraints = {
    }

    private Integer featureRelationshipId;
    private CVTerm type;
    private Feature objectFeature;
    private Feature subjectFeature;
    private String value;
    private int rank;
//    private Set<FeatureRelationshipProperty> featureRelationshipProperties = new HashSet<FeatureRelationshipProperty>(0);
//    private Set<FeatureRelationshipPublication> featureRelationshipPublications = new HashSet<FeatureRelationshipPublication>(0);

    static hasMany = [
            featureRelationshipProperties : FeatureRelationshipProperty
            ,featureRelationshipPublications: FeatureRelationshipPublication
    ]
}
