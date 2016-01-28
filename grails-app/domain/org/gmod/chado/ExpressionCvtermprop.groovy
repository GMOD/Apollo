package org.gmod.chado

class ExpressionCvtermprop {

    String value
    Integer rank
    ExpressionCvterm expressionCvterm
    Cvterm cvterm

    static belongsTo = [Cvterm, ExpressionCvterm]

    static mapping = {
        datasource "chado"
        id column: "expression_cvtermprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "expression_cvterm_id"]
        rank unique: ["cvterm", "expressionCvterm"]
    }
}
