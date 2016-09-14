package org.bbop.apollo

import grails.converters.JSON

/**
 * Created by Nathan Dunn on 10/29/14.
 *
 * Updated to include multiple feature locations and sort by rank.
 * This makes the assumption that they are on the same sequence, which is incorrect.
 *
 * We want to include the one that starts first, first.
 * And then the one that ends last last.
 *
 * First we find the common sequence.
 *
 * TODO:  We could reconstruct a list of sequences from the to FeatureLocations and compare
 *
 * In the interim, this will fix 99% of all likely cases
 */
class FeaturePositionComparator<T extends Feature> implements  Comparator<T>{

    private boolean sortByStrand;
    private List<Sequence> orderedSequenceList

    public FeaturePositionComparator() {
        this(true);
    }

    public FeaturePositionComparator(boolean sortByStrand) {
        this.sortByStrand = sortByStrand;
    }

    public FeaturePositionComparator(boolean sortByStrand,List<Sequence> orderedSequenceList) {
        this.sortByStrand = sortByStrand;
        this.orderedSequenceList = orderedSequenceList
    }

    public int compare(T feature1, T feature2) {

        Sequence sequence = findCommonSequence(feature1,feature2)
        int rankFeature1
        int rankFeature2
        if(sequence==null){
            if(!orderedSequenceList){
                throw new RuntimeException("Can not compare features with no common sequences.")
            }
            else{
                rankFeature1 = orderedSequenceList.indexOf(feature1.getFirstSequence())
                rankFeature2 = orderedSequenceList.indexOf(feature2.getFirstSequence())
            }
        }
        else{
            rankFeature1 = getRankForFeature(feature1,sequence)
            rankFeature2 = getRankForFeature(feature2,sequence)
        }

        int retVal = 0;

        // if the rank of feature for this sequence is less, then it is the prefixing
        // part of another feature, crossing a boundary after this one
        if(rankFeature1 < rankFeature2){
            return -1 ;
        }
        // if the rank of feature for this sequence is greater, then it is the tailing
        // part of another exon, crossing a previous boundary
        if(rankFeature1 > rankFeature2){
            return 1 ;
        }
        if (feature1.getFmin() < feature2.getFmin()) {
            retVal = -1;
        }
        else if (feature1.getFmin() > feature2.getFmin()) {
            retVal = 1;
        }
        else if (feature1.getFmax() < feature2.getFmax()) {
            retVal = -1;
        }
        else if (feature1.getFmax() > feature2.getFmax()) {
            retVal = 1;
        }
        else if (feature1.getLength() != feature2.getLength()) {
            retVal = feature1.getLength() < feature2.getLength() ? -1 : 1;
        }
        if (sortByStrand && feature1.getStrand() == -1) {
            retVal *= -1;
        }
        return retVal;
    }

    static int getRankForFeature(T feature, Sequence sequence) {
        for(fl in feature.featureLocations){
            if(fl.sequence==sequence){
                return fl.rank
            }
        }
        return -1
    }

    static Sequence findCommonSequence(T feature1, T feature2) {
        for(fl1 in feature1.featureLocations){
            for(fl2 in feature2.featureLocations){
                if(fl1.sequence==fl2.sequence){
                    return fl1.sequence
                }
            }
        }

        return null
    }
}
