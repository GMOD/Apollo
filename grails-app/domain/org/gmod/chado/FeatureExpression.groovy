package org.gmod.chado

class FeatureExpression {

    Pub pub
    Feature feature
    Expression expression

    static hasMany = [featureExpressionprops: FeatureExpressionprop]
    static belongsTo = [Expression, Feature, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_expression_id", generator: "increment"
        version false
    }
}
