package org.bbop.apollo

/**
 * In the ontology, this is a "is_a" relationship . .. not sure if it makes sense to keep it that way, though
 */
class Match extends Region{

    static constraints = {
    }

    // added
    AnalysisFeature analysisFeature;

    static String ontologyId = "SO:0000343"// XX:NNNNNNN
    static String cvTerm = "Match"// may have a link

    // add convenience methods

    public void setQueryFeatureLocation(int fmin, int fmax, int strand, Feature source) {
        FeatureLocation loc = getQueryFeatureLocation();
        boolean needToAdd = false;
        if (loc == null) {
            loc = new FeatureLocation();
            needToAdd = true;
        }
        if (source != null) {
            loc.setSourceFeature(source.getFeature());
        }
        loc.setRank(0);
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(strand);
        if (needToAdd) {
            getFeatureLocations().add(loc);
        }
    }

    public void setSubjectFeatureLocation(int fmin, int fmax, int strand, Feature source) {
        FeatureLocation loc = getSubjectFeatureLocation();
        boolean needToAdd = false;
        if (loc == null) {
            loc = new FeatureLocation();
            needToAdd = true;
        }
        if (source != null) {
            loc.setSourceFeature(source.getFeature());
        }
        loc.setRank(1);
        loc.setFmin(fmin);
        loc.setFmax(fmax);
        loc.setStrand(strand);
        if (needToAdd) {
            getFeatureLocations().add(loc);
        }
    }
    public void setIdentity(double identity) {
        if (analysisFeature == null) {
            return;
        }
        analysisFeature.setIdentity(identity);
    }
}
