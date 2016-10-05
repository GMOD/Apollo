package org.bbop.apollo
/**
 * Created by ndunn on 10/29/14.
 */
class FeaturePositionComparator<T extends Feature> implements  Comparator<T>{

    private boolean sortByStrand;

    public FeaturePositionComparator() {
        this(true);
    }

    public FeaturePositionComparator(boolean sortByStrand) {
        this.sortByStrand = sortByStrand;
    }

    public int compare(T feature1, T feature2) {

        if (feature1 == null || feature2 == null) {
//            log.info("both features null");
        }

        int retVal = 0;
        FeatureLocation featureLocation1 = feature1.featureLocation
        FeatureLocation featureLocation2  = feature2.featureLocation
        if (featureLocation1.fmin < featureLocation2.fmin) {
            retVal = -1;
        }
        else if (featureLocation1.getFmin() > featureLocation2.getFmin()) {
            retVal = 1;
        }
        else if (featureLocation1.getFmax() < featureLocation2.getFmax()) {
            retVal = -1;
        }
        else if (featureLocation1.getFmax() > featureLocation2.getFmax()) {
            retVal = 1;
        }
        else if (featureLocation1.calculateLength() != featureLocation2.calculateLength()) {
            retVal = featureLocation1.calculateLength() < featureLocation2.calculateLength() ? -1 : 1;
        }
        if (sortByStrand && featureLocation1.getStrand() == -1) {
            retVal *= -1;
        }
        return retVal;
    }
}
