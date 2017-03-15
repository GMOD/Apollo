package org.bbop.apollo

class VariantSetMetadata {

    String key;
    String value;
    String number;
    String type;
    String description;

    static constraints = {
    }

    static belongsTo = [
            variantSet: VariantSet
    ]
}
