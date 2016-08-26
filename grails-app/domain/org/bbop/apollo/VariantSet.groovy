package org.bbop.apollo

class VariantSet {

    String name;

    static constraints = {
    }

    static HasMany = [
            metadata: VariantSetMetadata
    ]
}
