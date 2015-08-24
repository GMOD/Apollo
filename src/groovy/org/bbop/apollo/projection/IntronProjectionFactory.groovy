package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 */
class IntronProjectionFactory {


    Projection generateProjection(Track trackA,Track trackB){
       IntronProjection projection= new IntronProjection()




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
