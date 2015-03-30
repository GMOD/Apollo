package org.bbop.apollo.filter

import org.bbop.apollo.Feature

/**
 * Created by ndunn on 2/3/15.
 * @E is E-type
 * @T is T-type
 */
class StopCodonFilter implements FeatureFilter<List<String>, Feature> {

    Random random = new Random()

    List<String> filterFeature(Feature feature) {

        List<String> errorList = new ArrayList<>()

//        if(random.nextFloat()<0.2){
        // TODO: do an actual search at some point
        if (false) {
            errorList.add("Stop Codon")
        }

        return errorList
    }
//    List<E> filterFeatures(List<T> objects)
}