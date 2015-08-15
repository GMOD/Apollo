package org.bbop.apollo

import org.bbop.apollo.projection.Projection
import org.bbop.apollo.projection.ProjectionEngine
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

    void "generate-spec"() {

        given:
        Track track1 = new Track()
        Track track2 = new Track()
        ProjectionEngine projectionEngine = new ProjectionEngine()

        when: "we generate a projection"
        Projection projectionTrack1To2  = projectionEngine.generateForwardProjection(track1,track2)
        List<Projection> projectionList = [projectionTrack1To2]
        Track track3 = projectionEngine.projectToTrack(track1,projectionList)

        then: "it should generate forward "
        assert projectionEngine.sameTrack(track2,track3)

        when: "we project backwards"
        Projection projectionTrack2To1  = projectionEngine.generateForwardProjection(track2,track1)
        Track track0 = projectionEngine.projectToTrack(track1,[projectionTrack2To1])

        then: "it should generate backward"
        assert projectionEngine.sameTrack(track0,track1)

    }
}
