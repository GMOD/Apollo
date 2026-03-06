package org.gmod.chado

class EnvironmentCvterm {

    Environment environment
    Cvterm cvterm

    static belongsTo = [Cvterm, Environment]

    static mapping = {
        datasource "chado"
        id column: "environment_cvterm_id", generator: "increment"
        version false
    }
}
