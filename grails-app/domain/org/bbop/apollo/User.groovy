package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Maps to CVTerm Owner, no Ontology term
 */
class User implements Ontological {


    static auditable = true

    // TODO: username should be mapped to "value" of FeatureProperty
    String username
    String passwordHash
    String firstName
    String lastName
    String metadata // this is JSON metadata

    static String cvTerm = "Owner"
    static String ontologyId = "Owner"

    static hasMany = [roles: Role, userGroups: UserGroup, assemblages: Assemblage]

    static belongsTo = [
            UserGroup
    ]


    static constraints = {
        username(nullable: false, blank: false, unique: true)
        passwordHash(display: false, blank: false, null: false,minSize: 5)
        metadata(display: false, blank: true,nullable: true)
    }

    static mapping = {
        table "grails_user"
//        password column: "grails_password"
    }

    private void validateMetaData(){
        // resets bad JSON
        if(!metadata || !metadata.startsWith("{") || !metadata.endsWith("}")){
            metadata = "{}"
        }
    }

    JSONObject addMetaData(String key,String value){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(metadata) as JSONObject
        jsonObject.put(key,value)
        metadata = jsonObject.toString()
        return jsonObject
    }

    def getMetaData(String key){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(metadata) as JSONObject
        return jsonObject.containsKey(key) ? jsonObject.get(key) : null
    }

    JSONObject getMetaDataObject(){
        validateMetaData()
        return JSON.parse(metadata) as JSONObject
    }

    def removeMetaData(String key){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(metadata) as JSONObject
        String value = jsonObject.remove(key)
        metadata = jsonObject.toString()
        return value
    }
}
