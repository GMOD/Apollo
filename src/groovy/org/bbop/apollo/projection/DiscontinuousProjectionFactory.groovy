package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 * Used only for testing
 */
@Singleton
class DiscontinuousProjectionFactory {

    /**
     * For each track,
     * @param inputTrack
     * @param padding
     * @return
     */
    DiscontinuousProjection createProjection(Track inputTrack,Integer padding=0){
       DiscontinuousProjection projection= new DiscontinuousProjection()

        inputTrack.coordinateList.each {
            projection.addInterval(it.min,it.max,padding)
        }

        return projection
    }


//    Track projectToTrack(Track track,List<Projection> projections) {
//        Track returnTrack = track
//
//        for(Projection projection : projections){
//            returnTrack = projection.project(returnTrack)
//        }
//
//        return returnTrack
//    }
}
