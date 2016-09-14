package org.bbop.apollo.projection

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * To be serialized by a projection
 * Both the min/max should be treated as inclusive coordinates
 * Created by nathandunn on 9/24/15.
 * TODO: merge with coordinate
 */
@EqualsAndHashCode
@ToString
class Location {

    Integer min
    Integer max
    ProjectionSequence sequence
}
