package org.bbop.apollo

class VariantSet {

    String name;

    static constraints = {
    }

    static hasMany = [
            metadata: VariantSetMetadata
    ]
}
