package org.bbop.apollo

class DataAdapter {


    static constraints = {
    }

    static hasMany = [
            dataAdapters: DataAdapter
    ]

    String key
    String implementationClass
    String permission
    String options

    String tempDirectory

    // gff3 stuff
    // value to use in the source column
    String source
    Boolean exportSourceGenomicSequence

    // feature stuff
    // [{featureType:"sequence:mRNA"},{featureType:"sequence:transcript"}]
    String featureTypeString

    // chado //
//    String hibernateConfig
}
