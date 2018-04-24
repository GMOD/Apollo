package org.bbop.apollo

class AlleleInfo {

    String tag;
    String value;

    static constraints = {
    }

    static belongsTo = [
            Allele
    ]
}
