package org.bbop.apollo


class Assemblage {


    String projection
    Integer padding
    String payload
    String sequenceList // JSON array of sequence list
//    String referenceTrack  // JSON array of reference tracks or single string, TODO: remove
    Organism organism
    Integer start
    Integer end
    String name // given namve of the assemblage

    static constraints = {
        projection nullable: true
//        sequenceList nullable: false,unique: ['organism','user'],minSize: 50 // [{'name':'A','id':1,'start':0,'end':0,'length':0}]
//        sequenceList nullable: false,unique: ['organism','user'],minSize: 5 // [{'name':'A','id':1,'start':0,'end':0,'length':0}]
        sequenceList nullable: false,unique: ['organism'],minSize: 5 // [{'name':'A','id':1,'start':0,'end':0,'length':0}]
//        sequenceList nullable: false,unique: ['organism'],minSize: 50 // [{'name':'A','id':1,'start':0,'end':0,'length':0}]
//        referenceTrack nullable: true
        padding nullable: true
        payload nullable: true
        organism nullable: false
        start nullable: false
        end nullable: false
//        name nullable: true, blank: false, unique: ['organism','user']
        name nullable: true, blank: false, unique: ['organism']
    }

    static mapping = {
        sequenceList type: "text"
//        referenceTrack type: "text"
        payload type: "text"
        start column: "startbp"
        end column: "endbp"
    }

}
