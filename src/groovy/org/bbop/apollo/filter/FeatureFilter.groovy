package org.bbop.apollo.filter

/**
 * Created by ndunn on 2/3/15.
 * @E is E-type
 * @T is T-type
 */
interface FeatureFilter<E,T> {


    E filterFeature(T object)
//    List<E> filterFeatures(List<T> objects)
}