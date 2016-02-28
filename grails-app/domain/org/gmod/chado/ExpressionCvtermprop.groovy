package org.gmod.chado

class ExpressionCvtermprop {

    String value
    Integer rank
    ExpressionCvterm expressionCvterm
    Cvterm type

    static belongsTo = [Cvterm, ExpressionCvterm]

    static mapping = {
        datasource "chado"
        id column: "expression_cvtermprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "expression_cvterm_id"]
        rank unique: ["type", "expressionCvterm"]
    }
}
