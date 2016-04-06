package org.gmod.chado

class ExpressionPub {

    Pub pub
    Expression expression

    static belongsTo = [Expression, Pub]

    static mapping = {
        datasource "chado"
        id column: "expression_pub_id", generator: "increment"
        version false
    }
}
