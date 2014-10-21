package org.bbop.apollo

/**
 * NOTE: superclass is NOT region . . .
 */
class Intron extends TranscriptRegion{

    static constraints = {
    }


    String ontologyId = "SO:0000188"// XX:NNNNNNN
//    String cvTerm = "Match"// may have a link
    String cvTerm = "Intron"
}
