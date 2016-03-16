package org.gmod.chado

class Cv {

    String name
    String definition

    static hasMany = [cvprops    : Cvprop,
                      cvtermpaths: Cvtermpath,
                      cvterms    : Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cv_id", generator: "increment"
        version false
    }

    static constraints = {
        name unique: true
        definition nullable: true
    }
}
