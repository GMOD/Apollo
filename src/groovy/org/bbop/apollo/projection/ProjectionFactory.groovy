package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
interface ProjectionFactory {

    Projection generateProjection(Track trackA,Track trackB)
    Track projectToTrack(Track track,List<Projection> projections)
}
