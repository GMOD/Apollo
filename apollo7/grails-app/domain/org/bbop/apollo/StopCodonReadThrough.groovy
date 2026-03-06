package org.bbop.apollo

/**
 * Note: top-level in the sequence ontology
 * Maps to: ReadthroughStopCodon
 */

// its unclear how this should be handled properly as its closer to a CDS
//class StopCodonReadThrough extends CDS{
class StopCodonReadThrough extends Feature implements Ontological {

    static constraints = {
    }

    static String cvTerm = "stop_codon_read_through"
    static String ontologyId = "SO:0000883"// XX:NNNNNNN
    static String alternateCvTerm = "StopCodonReadThrough"

    // add convenience methods
}
