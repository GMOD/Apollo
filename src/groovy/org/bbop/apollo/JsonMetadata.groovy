package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

trait JsonMetadata {

    abstract String getMetadata()
    abstract void setMetadata(String newMetadata)

    private void validateMetaData(){
        // resets bad JSON
        if(!getMetadata() || !getMetadata().startsWith("{") || !getMetadata().endsWith("}")){
            setMetadata("{}")
        }
    }

    JSONObject addMetaData(String key, String value){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(getMetadata()) as JSONObject
        jsonObject.put(key,value)
        setMetadata(jsonObject.toString())
        return jsonObject
    }

    def getMetaData(String key){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(getMetadata()) as JSONObject
        return jsonObject.containsKey(key) ? jsonObject.get(key) : null
    }

    JSONObject getMetaDataObject(){
        validateMetaData()
        return JSON.parse(getMetadata()) as JSONObject
    }

    def removeMetaData(String key){
        validateMetaData()
        JSONObject jsonObject = JSON.parse(getMetadata()) as JSONObject
        String value = jsonObject.remove(key)
        setMetadata(jsonObject.toString())
        return value
    }
}