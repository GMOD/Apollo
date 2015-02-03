package org.bbop.apollo.filter

import org.bbop.apollo.CDS
import org.bbop.apollo.Feature

/**
 * Created by ndunn on 2/3/15.
 * @E is E-type
 * @T is T-type
 *
 * If any CDS is not exactly divisible 3, put error on feature
 */
class Cds3Filter implements FeatureFilter<List<String>,Feature>{


    // TODO: need a unit test for this
    List<String> filterFeature(Feature feature){
        List<String> errorList = new ArrayList<>()
        if(feature.ontologyId == CDS.ontologyId){
            println "evaluating CDS: " + feature.name
            if(feature.getLength()%3!=0){
                errorList.add("Error::CDS/3")
            }
        }
        return errorList
    }
//    List<E> filterFeatures(List<T> objects)
}