package org.bbop.apollo

class VariantInfo {

    String tag;
    String value;

    static constraints = {
    }

    static belongsTo = [
            SequenceAlteration
    ]
}
