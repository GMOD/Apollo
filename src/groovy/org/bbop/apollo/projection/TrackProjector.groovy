package org.bbop.apollo.projection

import org.bbop.apollo.Organism
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * Created by nathandunn on 8/11/15.
 */
interface TrackProjector {

//    Track projectTrack(Track trackA,Track trackB,ProjectionInterface projection)
    String projectTrack(JSONArray refSeqJsonObject, MultiSequenceProjection projection,Organism currentOrganism,String refererLoc)
}
