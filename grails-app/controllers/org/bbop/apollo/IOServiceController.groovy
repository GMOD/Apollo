package org.bbop.apollo

import grails.web.JSONBuilder
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.springframework.core.io.Resource
import grails.converters.JSON
import org.json.JSONString

class IOServiceController extends AbstractApolloController {
    
    def sequenceService
    def featureService
    def gff3HandlerService
    def fastaHandlerService
    def grailsResourceLocator
    
    def index() { }
    
    def handleOperation(String track, String operation) {
        println "Requested parameterMap: ${request.parameterMap.keySet()}"
        println "upstream params: ${params}"
        println "OPERATION: ${operation}"
        println "TYPE: ${params.adapter}"
        
        //JSONObject postObject = findPost()
        //operation = postObject.get(REST_OPERATION)
        //TODO: Currently not using the findPost() because the input params is not of the proper format
        
        def mappedAction = underscoreToCamelCase(operation)
        forward action: "${mappedAction}", params: params
    }
    
    def write() {
        String typeOfExport = params.type
        String sequenceType
        String sequenceName = params.tracks.substring("Annotations-".size())
        List<String> ontologyIdList = [Gene.class.name]
        def listOfFeatures = FeatureLocation.executeQuery("select distinct f from FeatureLocation fl join fl.sequence s join fl.feature f where s.name in (:sequenceName) and fl.feature.class in (:ontologyIdList) order by f.name asc", [sequenceName: sequenceName, ontologyIdList: ontologyIdList])
        File outputFile = File.createTempFile ("Annotations-" + sequenceName, "." + typeOfExport.toLowerCase())

        if (typeOfExport == "GFF3") {
            // call gff3HandlerService
            gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String)
        } else if (typeOfExport == "FASTA") {
            // call fastaHandlerService
            sequenceType = params.seqType
            fastaHandlerService.writeFeatures(listOfFeatures, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
        }
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("filePath", outputFile.path)
        jsonObject.put("exportType", typeOfExport)
        jsonObject.put("sequenceType", sequenceType)
        redirect(controller: 'sequence', action: 'exportHandler', params: jsonObject)
//        println "TEST RESOURCE LOCATOR: ${grailsResourceLocator.findResourceForURI(outputFile.toURI().toString())}"
//        render {
//            div(id: "downloadDiv", outputFile.path)
//            
//        }
    }
}
