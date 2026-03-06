package org.gmod.chado

class Eimage {

    String eimageData
    String eimageType
    String imageUri

    static hasMany = [expressionImages: ExpressionImage]

    static mapping = {
        datasource "chado"
        id column: "eimage_id", generator: "increment"
        version false
    }

    static constraints = {
        eimageData nullable: true
        imageUri nullable: true
    }
}
