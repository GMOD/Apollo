package org.gmod.chado

class FeatureExpressionprop {

    String value
    Integer rank
    FeatureExpression featureExpression
    Cvterm cvterm

    static belongsTo = [Cvterm, FeatureExpression]

    static mapping = {
        datasource "chado"
        id column: "feature_expressionprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "feature_expression_id"]
        rank unique: ["cvterm", "featureExpression"]
    }
}
