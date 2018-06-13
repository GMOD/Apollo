package org.bbop.apollo

class AlleleInfo {

    String tag;
    String value;
    Allele allele;

    static constraints = {
    }

    static mapping = {
        value type: 'text'
    }

    static belongsTo = [
            Allele
    ]
}
