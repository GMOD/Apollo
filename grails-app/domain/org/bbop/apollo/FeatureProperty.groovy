package org.bbop.apollo

class FeatureProperty {

    static constraints = {
    }

    Integer featurePropertyId;
    CVTerm type;
    Feature feature;
    String value;
    int rank;
//    Set<FeaturePropertyPublication> featurePropertyPublications = new HashSet<FeaturePropertyPublication>(0);

    static hasMany = [
            featurePropertyPublications :  FeaturePropertyPublication
    ]
}
