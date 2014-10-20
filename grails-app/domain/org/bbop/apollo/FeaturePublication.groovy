package org.bbop.apollo

class FeaturePublication {

    static constraints = {
    }

    Integer featurePublicationId;
    Publication publication;
    Feature feature;
//    Set<FeaturePublicationProperty> featurePublicationProperties = new HashSet<FeaturePublicationProperty>(0);

    static hasMany = [
            featurePublicationProperties : FeaturePublicationProperty
    ]
}
