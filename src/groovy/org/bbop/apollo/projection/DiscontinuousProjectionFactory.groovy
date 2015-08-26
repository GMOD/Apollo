package org.bbop.apollo.projection

/**
 * Created by nathandunn on 8/14/15.
 */
@Singleton
class DiscontinuousProjectionFactory {

    /**
     * For each track,
     * @param inputTrack
     * @param padding
     * @return
     */
    Projection createProjection(Track inputTrack,Integer padding=0){
       DiscontinuousProjection projection= new DiscontinuousProjection()

        inputTrack.coordinateList.each {
//            projection.addInterval(Math.max(it.min-padding,0),Math.min(it.max+padding,inputTrack.length))
            projection.addInterval(it.min-padding,it.max+padding)
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
