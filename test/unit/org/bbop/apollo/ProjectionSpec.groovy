package org.bbop.apollo

import org.bbop.apollo.projection.DuplicateTrackProjection
import org.bbop.apollo.projection.DuplicateTrackProjectionFactory
import org.bbop.apollo.projection.Projection
import org.bbop.apollo.projection.IntronProjectionFactory
import org.bbop.apollo.projection.Track
import spock.lang.Specification

/**
 * Created by nathandunn on 8/14/15.
 */
class ProjectionSpec extends Specification{

    def setup() {
    }

    def cleanup() {
    }

    void "confirm that we can generate a duplicate projection"() {

        given:
        Track track1 = new Track()

        when: "we generate a duplicate projection"
        track1.addCoordinate(4,12)
        track1.addCoordinate(70,80)
        Projection projectionTrack1To2 = new DuplicateTrackProjection()
        Track track2 = projectionTrack1To2.projectTrack(track1)

        then: "it should generate forward "
        assert track1.equals(track2)


    }
}
