package org.gmod.chado

class Expressionprop {

    String value
    Integer rank
    Expression expression
    Cvterm cvterm

    static belongsTo = [Cvterm, Expression]

    static mapping = {
        datasource "chado"
        id column: "expressionprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "expression_id"]
        rank unique: ["cvterm", "expression"]
    }
}
