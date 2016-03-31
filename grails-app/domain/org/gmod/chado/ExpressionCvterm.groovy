package org.gmod.chado

class ExpressionCvterm {

    Integer rank
    Cvterm cvterm
    Expression expression
    Cvterm cvtermType

    static hasMany = [expressionCvtermprops: ExpressionCvtermprop]
    static belongsTo = [Cvterm, Expression]

    static mapping = {
        datasource "chado"
        id column: "expression_cvterm_id", generator: "increment"
        version false
    }
}
