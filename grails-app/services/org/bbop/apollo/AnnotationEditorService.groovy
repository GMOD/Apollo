package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import static java.util.Calendar.YEAR
import groovy.json.JsonBuilder

@Transactional
class AnnotationEditorService {


    @NotTransactional
    String cleanJSONString(String inputString){
        String outputString = new String(inputString)
        // remove leading string
        outputString = outputString.indexOf("\"")==0 ? outputString.substring(1) : outputString
        outputString = outputString.lastIndexOf("\"")==outputString.length()-1 ? outputString.substring(0,outputString.length()-1) : outputString
//        outputString = outputString.replaceAll("/\\\\/","")
        outputString = outputString.replaceAll("\\\\\"","\"")
        return outputString
    }

    JsonBuilder todaysAnnotation(){
    	Date today = new Date()
    	Date yesterday = today -1 // -24 hour
    	List updatedGenes = Gene.findAllByLastUpdatedGreaterThan(yesterday)
    	
    	Map geneToSpecies = [ : ]    	
    	updatedGenes.each { gene ->
    		geneToSpecies[gene.uniqueName] = gene.getFeatureLocation().sequence.organism.commonName
    	}
    	
    	JsonBuilder toJson = new JsonBuilder()
    	toJson(geneToSpecies)
    	return toJson
    }        
}
