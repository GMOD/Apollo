package org.gmod.chado

class Expressionprop {

    String value
    Integer rank
    Expression expression
    Cvterm type

    static belongsTo = [Cvterm, Expression]

    static mapping = {
        datasource "chado"
        id column: "expressionprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "expression_id"]
        rank unique: ["type", "expression"]
    }
}
