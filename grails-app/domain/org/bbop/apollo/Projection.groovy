package org.bbop.apollo

class Projection {

    static constraints = {
    }

    String track
    Sequence sequence // iomplies organism

    // this will transferrable to a ProjectionInterface object . . . for optimization
    // TODO: replace the JBrowse hashmap
    String projectionJsonString

    static mapping = {
        projectionJsonString type: "text"
    }
}
