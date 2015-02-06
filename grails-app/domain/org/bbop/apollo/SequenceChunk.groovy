package org.bbop.apollo

class SequenceChunk {

    static constraints = {
    }

    static mapping = {
        residue type: "text"
    }

    Sequence sequence
    int chunkNumber
    String residue
}
