package org.bbop.apollo

class CallSet {

    String name

    static constraints = {
    }

    static belongsTo = [
            variantSet: VariantSet
    ]
}
