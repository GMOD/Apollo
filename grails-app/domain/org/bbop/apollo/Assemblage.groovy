package org.bbop.apollo


class Assemblage {


    String projection // TODO: remove?
    Integer padding // TODO: remove?
    String payload // metadata
    String sequenceList // JSON array of sequence list
    Organism organism
    Integer start
    Integer end
    String name // given name of the assemblage

    static constraints = {
        projection nullable: true
        sequenceList nullable: false,unique: ['organism'],minSize: 5 // [{'name':'A','id':1,'start':0,'end':0,'length':0}]
        padding nullable: true
        payload nullable: true
        organism nullable: false
        start nullable: false
        end nullable: false
        name nullable: true, blank: false
    }

    static mapping = {
        sequenceList type: "text"
        name type: "text"
        payload type: "text"
        start column: "startbp"
        end column: "endbp"
    }

}
