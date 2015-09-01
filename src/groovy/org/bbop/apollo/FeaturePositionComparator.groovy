package org.bbop.apollo
/**
 * Created by Nathan Dunn on 10/29/14.
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
        if (feature1.getFeatureLocation().getFmin() < feature2.getFeatureLocation().getFmin()) {
            retVal = -1;
        }
        else if (feature1.getFeatureLocation().getFmin() > feature2.getFeatureLocation().getFmin()) {
            retVal = 1;
        }
        else if (feature1.getFeatureLocation().getFmax() < feature2.getFeatureLocation().getFmax()) {
            retVal = -1;
        }
        else if (feature1.getFeatureLocation().getFmax() > feature2.getFeatureLocation().getFmax()) {
            retVal = 1;
        }
        else if (feature1.getLength() != feature2.getLength()) {
            retVal = feature1.getLength() < feature2.getLength() ? -1 : 1;
        }
        if (sortByStrand && feature1.getFeatureLocation().getStrand() == -1) {
            retVal *= -1;
        }
        return retVal;
    }
}
