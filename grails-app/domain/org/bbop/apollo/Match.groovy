package org.bbop.apollo

/**
 * In the ontology, this is a "is_a" relationship ... not sure if it makes sense to keep it that way, though
 */
class Match extends Region{

    static constraints = {
    }

    // added
    AnalysisFeature analysisFeature;

    static String cvTerm = "match"
    static String ontologyId = "SO:0000343"// XX:NNNNNNN
    static String alternateCvTerm = "Match"

    // add convenience methods

    def setQueryFeatureLocation(int fmin, int fmax, int strand, Feature source) {
        FeatureLocation loc = analysisFeature.getQueryFeatureLocation();
        boolean needToAdd = false;
        if (loc == null) {
            loc = new FeatureLocation();
            needToAdd = true;
        }
        loc.setRank(0);
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(strand);
        if (needToAdd) {
            analysisFeature.getFeatureLocations().add(loc);
        }
    }

    def setSubjectFeatureLocation(int fmin, int fmax, int strand, Feature source) {
        FeatureLocation loc = getSubjectFeatureLocation();
        boolean needToAdd = false;
        if (loc == null) {
            loc = new FeatureLocation();
            needToAdd = true;
        }
        loc.setRank(1);
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(strand);
        if (needToAdd) {
            analysisFeature.getFeatureLocations().add(loc);
            getFeatureLocations().add(loc);
        }
    }
    def setIdentity(double identity) {
        if (analysisFeature == null) {
            return;
        }
        analysisFeature.setIdentity(identity);
    }


    def getQueryFeatureLocation() {
        for (FeatureLocation loc : getFeatureLocations()) {
            if (loc.getRank() == 0) {
                return loc;
            }
        }
        return null;
    }

    def getSubjectFeatureLocation() {
        for (FeatureLocation loc : getFeatureLocations()) {
            if (loc.getRank() == 1) {
                return loc;
            }
        }
        return null;
    }
}
