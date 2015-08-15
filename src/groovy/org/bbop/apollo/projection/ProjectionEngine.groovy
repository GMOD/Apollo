package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 */
class ProjectionEngine {


    Projection generateForwardProjection(Track trackA,Track trackB){
       Projection projection= new Projection()

        return projection
    }

    def sameTrack(Track track1, Track track2) {

        return true
    }


    Track projectToTrack(Track track,List<Projection> projections) {
        Track returnTrack = track

        for(Projection projection : projections){
            returnTrack = projection.project(returnTrack)
        }

        return returnTrack
    }
}
