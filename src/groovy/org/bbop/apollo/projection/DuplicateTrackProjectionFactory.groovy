package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 */
class DuplicateTrackProjectionFactory implements ProjectionFactory{


    @Override
    Projection generateProjection(Track trackA,Track trackB){
       Projection projection= new DuplicateTrackProjection()
       return projection
    }


    @Override
    Track projectToTrack(Track track,List<Projection> projections) {
        Track returnTrack = track

        for(Projection projection : projections){
            returnTrack = projection.project(returnTrack)
        }

        return returnTrack
    }
}
