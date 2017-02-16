package org.bbop.apollo.alteration

import groovy.json.JsonBuilder
import org.bbop.apollo.CDS
import org.bbop.apollo.Feature

/**
 * Created by deepak.unni3 on 2/8/17.
 */
class OverlapInfo {
    String uniquename
    int strand
    Boolean overlaps
    Boolean isUpstream
    Boolean isDownstream
    Boolean isModified
    LocationInfo location
    LocationInfo localLocation
    LocationInfo modLocation
    LocationInfo modLocalLocation
    String locationSeq
    String modLocationSeq
    ArrayList<String> inference
    ArrayList<OverlapInfo> children

    @Override
    public String toString() {
        return new JsonBuilder( this ).toPrettyString()
    }

    public OverlapInfo generateClone() {
        OverlapInfo clone = new OverlapInfo()
        clone.uniquename = this.uniquename
        clone.strand = this.strand
        clone.location = this.modLocation
        clone.modLocation = null
        clone.localLocation = this.modLocalLocation
        clone.modLocalLocation = null
        clone.locationSeq = this.modLocationSeq
        clone.modLocationSeq = null
        return clone
    }
}
