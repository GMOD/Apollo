package org.bbop.apollo.filter

import org.bbop.apollo.Feature

/**
 * Created by ndunn on 2/3/15.
 * @E is E-type
 * @T is T-type
 */
class StopCodonFilter implements FeatureFilter<List<String>,Feature>{

    Random random = new Random()

    List<String> filterFeature(Feature feature){

        List<String> errorList = new ArrayList<>()

        if(random.nextFloat()<0.2){
            errorList.add("Error::Stop Codon")
        }

        return errorList
    }
//    List<E> filterFeatures(List<T> objects)
}