package org.bbop.apollo

class SequenceChunk {

    static constraints = {
    }

    static mapping = {
        residue type: "text"
        cache usage: 'read-only'
    }

    Sequence sequence
    int chunkNumber
    String residue
}
