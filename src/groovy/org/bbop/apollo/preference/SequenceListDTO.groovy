package org.bbop.apollo.preference

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class SequenceListDTO extends ArrayList<SequenceDTO>{

    SequenceListDTO(String inputString){
        JSONArray inputArray = JSON.parse(inputString) as JSONArray
        for(obj in inputArray){
            SequenceDTO sequenceDTO = new SequenceDTO(
                    id: obj.id
                    ,name: obj.name
                    ,organism: obj.organism
                    ,start: obj.start
                    ,end: obj.end
                    ,length: obj.length
            )
            add(sequenceDTO)
        }
    }
}
