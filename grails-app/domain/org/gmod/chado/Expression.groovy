package org.gmod.chado

class Expression {

    String uniquename
    String md5checksum
    String description

    static hasMany = [expressionCvterms : ExpressionCvterm,
                      expressionImages  : ExpressionImage,
                      expressionPubs    : ExpressionPub,
                      expressionprops   : Expressionprop,
                      featureExpressions: FeatureExpression]

    static mapping = {
        datasource "chado"
        id column: "expression_id", generator: "increment"
        version false
    }

    static constraints = {
        uniquename unique: true
        md5checksum nullable: true, maxSize: 32
        description nullable: true
    }
}
