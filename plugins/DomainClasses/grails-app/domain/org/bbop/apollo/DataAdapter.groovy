package org.bbop.apollo

class DataAdapter {


    static constraints = {
        key nullable: false
        permission nullable: false
        options nullable: true
        implementationClass nullable: true
        tempDirectory nullable: true
        exportSourceGenomicSequence nullable: true
        source nullable: true
        featureTypeString nullable: true
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

    static mapping = {
        key column: "data_adapter_key"
    }
}
