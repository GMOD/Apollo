package org.gmod.chado

class FeatureExpressionprop {

    String value
    Integer rank
    FeatureExpression featureExpression
    Cvterm type

    static belongsTo = [Cvterm, FeatureExpression]

    static mapping = {
        datasource "chado"
        id column: "feature_expressionprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "feature_expression_id"]
        rank unique: ["type", "featureExpression"]
    }
}
