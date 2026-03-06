package org.gmod.chado

class ExpressionImage {

    Expression expression
    Eimage eimage

    static belongsTo = [Eimage, Expression]

    static mapping = {
        datasource "chado"
        id column: "expression_image_id", generator: "increment"
        version false
    }
}
