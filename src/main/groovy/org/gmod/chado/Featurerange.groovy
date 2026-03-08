package org.gmod.chado

class Featurerange {

    String rangestr
    Feature feature
    Featuremap featuremap
    Feature rightendf
    Feature leftstartf
    Feature leftendf
    Feature rightstartf

    static belongsTo = [Feature, Featuremap]

    static mapping = {
        datasource "chado"
        id column: "featurerange_id", generator: "increment"
        version false
        optimisticLock 'none'
    }

    static constraints = {
        rangestr nullable: true
        leftendf nullable: true
        rightstartf nullable: true
    }
}
