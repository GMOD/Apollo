package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 */
class DiscontinuousProjectionFactory {


    Projection generateProjection(Track trackA,Track trackB){
       DiscontinuousProjection projection= new DiscontinuousProjection()




        return projection
    }


    Track projectToTrack(Track track,List<Projection> projections) {
        Track returnTrack = track

        for(Projection projection : projections){
            returnTrack = projection.project(returnTrack)
        }

        return returnTrack
    }
}
