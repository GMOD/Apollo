package org.bbop.apollo.preference

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class SequenceListDTO extends ArrayList<SequenceDTO>{

    SequenceListDTO(String inputString){
        JSONArray inputArray = JSON.parse(inputString) as JSONArray
        for(obj in inputArray){
            OrganismDTO organismDTO = new OrganismDTO(
                    commonName: obj.organism
            )
            SequenceDTO sequenceDTO = new SequenceDTO(
                    id: obj.id
                    ,name: obj.name
                    ,organism: organismDTO
                    ,start: obj.start
                    ,end: obj.end
                    ,length: obj.length
            )
            add(sequenceDTO)
        }
    }
}
