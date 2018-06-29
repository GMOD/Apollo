package org.bbop.apollo

class VariantInfo {

    String tag;
    String value;
    SequenceAlteration variant;

    static constraints = {
    }

    static mapping = {
        value type: 'text'
    }

    static belongsTo = [
            SequenceAlteration
    ]
}
