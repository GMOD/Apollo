package org.bbop.apollo
/**
 * Created by ndunn on 10/29/14.
 */
class FeaturePositionComparator<T extends Feature> implements  Comparator<T>{

    private boolean sortByStrand;

    FeaturePositionComparator() {
        this(true)
    }

    FeaturePositionComparator(boolean sortByStrand) {
        this.sortByStrand = sortByStrand
    }

    int compare(T feature1, T feature2) {

        if (feature1 == null || feature2 == null) {
//            log.info("both features null");
        }

        int retVal = 0;
        FeatureLocation featureLocation1 = feature1.featureLocation
        FeatureLocation featureLocation2  = feature2.featureLocation
        if (featureLocation1.fmin < featureLocation2.fmin) {
            retVal = -1;
        }
        else if (featureLocation1.fmin > featureLocation2.fmin) {
            retVal = 1;
        }
        else if (featureLocation1.fmax < featureLocation2.fmax) {
            retVal = -1;
        }
        else if (featureLocation1.fmax > featureLocation2.fmax) {
            retVal = 1;
        }
        else if (featureLocation1.calculateLength() != featureLocation2.calculateLength()) {
            retVal = featureLocation1.calculateLength() < featureLocation2.calculateLength() ? -1 : 1;
        }
            // overlapping perfectly, use strand to force consistent results
        else{
            retVal = featureLocation1.strand - featureLocation2.strand
        }

        if (sortByStrand && featureLocation1.strand == -1) {
            retVal *= -1;
        }
        return retVal;
    }
}
