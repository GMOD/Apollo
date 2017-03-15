package org.bbop.apollo

class Individual {

    String name
    String description
    String sex
    Organism organism

    static constraints = {
        name nullable: false
        description nullable: true
        sex nullable: true
        organism nullable: false
    }

    static hasMany = [
            individualInfo: IndividualInfo
    ]

}
