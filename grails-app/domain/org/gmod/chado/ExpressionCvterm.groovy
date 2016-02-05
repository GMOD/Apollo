package org.gmod.chado

class ExpressionCvterm {

    Integer rank
    Cvterm cvtermByCvtermId
    Expression expression
    Cvterm cvtermByCvtermTypeId

    static hasMany = [expressionCvtermprops: ExpressionCvtermprop]
    static belongsTo = [Cvterm, Expression]

    static mapping = {
        datasource "chado"
        id column: "expression_cvterm_id", generator: "assigned"
        version false
    }
}
