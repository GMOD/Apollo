package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
abstract class BiologicalRegion extends Region{

    static constraints = {
    }


    static String ontologyId = "SO:0001411"// XX:NNNNNNN


    /** Convenience method to get the fmin for the location of this feature.
     *
     * @return fmin of this feature.  Returns null if no FeatureLocation is set for this feature
     */
    public Integer getFmin() {
        if (getFeatureLocation() == null) {
            return null;
        }
        return getFeatureLocation().getFmin();
    }

    /** Convenience method to get the fmax for the location of this feature.
     *
     * @return fmax of this feature.  Returns null if no FeatureLocation is set for this feature
     */
    public Integer getFmax() {
        if (getFeatureLocation() == null) {
            return null;
        }
        return getFeatureLocation().getFmax();
    }


}
