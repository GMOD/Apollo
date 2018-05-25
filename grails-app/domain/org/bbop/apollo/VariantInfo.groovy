package org.bbop.apollo

class VariantInfo {

    String tag;
    String value;
    SequenceAlteration variant;

    static constraints = {
    }

    static belongsTo = [
            SequenceAlteration
    ]
}
