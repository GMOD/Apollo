package org.bbop.apollo

class AlleleInfo {

    Allele allele;
    String tag;
    String value;

    static constraints = {
    }

    static belongsTo = [
            Allele
    ]
}
