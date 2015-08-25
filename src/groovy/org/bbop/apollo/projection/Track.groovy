package org.bbop.apollo.projection


/**
 * Created by nathandunn on 8/14/15.
 *
 * This is essentially the same thing as a scaffold.
 */
class Track {

    int length
    String name

    List<Coordinate> coordinateList = new ArrayList<>()

    def addCoordinate(int min, int max) {
        coordinateList.add(new Coordinate(min: min, max: max))
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof Track)) return false

        Track track = (Track) o

        if(track.length!=length) {
            println "different trac lengths ${track.length} vs ${length}"
            return false
        }

        // coordinates must be in the same order as well
        for(int i = 0 ; i < coordinateList.size() ;i++){
            Coordinate coordinateA = coordinateList.get(i)
            Coordinate coordinateB = track.coordinateList.get(i)
            if(!coordinateA.equals(coordinateB)) return false
        }

        return true
    }

    int hashCode() {
        return (coordinateList != null ? coordinateList.hashCode() : 0)
    }
}
