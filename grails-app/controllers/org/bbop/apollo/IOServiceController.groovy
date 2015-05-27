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
    def preferenceService
    def grailsResourceLocator
    
    def index() { }
    
    def handleOperation(String track, String operation) {
        log.debug "Requested parameterMap: ${request.parameterMap.keySet()}"
        log.debug "upstream params: ${params}"
        //JSONObject postObject = findPost()
        //operation = postObject.get(REST_OPERATION)
        //TODO: Currently not using the findPost()
        def mappedAction = underscoreToCamelCase(operation)
        forward action: "${mappedAction}", params: params
    }
    
    def write() {
        log.debug("params to IOService::write(): ${params}")
        String typeOfExport = params.type
        String sequenceName = params.tracks.substring("Annotations-".size())
        String fileName
        String sequenceType
        List<String> ontologyIdList = [Gene.class.name,Pseudogene.class.name,RepeatRegion.class.name,TransposableElement.class.name]
        Organism organism = preferenceService.currentOrganismForCurrentUser
        def listOfFeatures = FeatureLocation.executeQuery("select distinct f from FeatureLocation fl join fl.sequence s join fl.feature f where s.organism = :organism and s.name in (:sequenceName) and fl.feature.class in (:ontologyIdList) order by f.name asc", [sequenceName: sequenceName, ontologyIdList: ontologyIdList,organism:organism])
        File outputFile = File.createTempFile ("Annotations-" + sequenceName + "-", "." + typeOfExport.toLowerCase())
        
        if (typeOfExport == "GFF3") {
            // call gff3HandlerService
            fileName = "Annotations-" + sequenceName + "." + typeOfExport.toLowerCase()
            gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String)
        } else if (typeOfExport == "FASTA") {
            // call fastaHandlerService
            sequenceType = params.seqType
            fileName = "Annotations-" + sequenceName + "." + sequenceType + "." + typeOfExport.toLowerCase()
            fastaHandlerService.writeFeatures(listOfFeatures, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
        }

        //generating a html fragment with the link for download that can be rendered on client side
        String htmlResponseString = "<html><head></head><body><iframe name='hidden_iframe' style='display:none'></iframe><a href='@DOWNLOAD_LINK_URL@' target='hidden_iframe'>@DOWNLOAD_LINK@</a></body></html>"
        String downloadLinkUrl = 'IOService/download/?filePath=' + URLEncoder.encode(outputFile.path) + "&fileType=" + typeOfExport + "&fileName=" + URLEncoder.encode(fileName)
        htmlResponseString = htmlResponseString.replace("@DOWNLOAD_LINK_URL@", downloadLinkUrl)
        htmlResponseString = htmlResponseString.replace("@DOWNLOAD_LINK@", fileName)
        
        render text: htmlResponseString, contentType: "text/html", encoding: "UTF-8"
    }
    
    def download() {
        def file = new File(params.filePath)
        if (!file.exists()) { return }
        response.contentType = "txt"
        //TODO: Support for gzipped output
        String fileName = params.fileName
        response.setHeader("Content-disposition", "attachment; filename=${fileName}")
        def outputStream = response.outputStream
        outputStream << file.text
        outputStream.flush()
        outputStream.close()
        file.delete()
    }
}
