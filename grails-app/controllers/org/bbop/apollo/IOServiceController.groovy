package org.bbop.apollo

import org.codehaus.groovy.grails.web.json.JSONObject

class IOServiceController extends AbstractApolloController {
    
    def sequenceService
    def featureService
    def gff3HandlerService
    def fastaHandlerService
    def preferenceService

    def index() { }
    
    def handleOperation(String track, String operation) {
        JSONObject postObject = findPost()
        log.info "handleOperation: ${request.parameterMap.keySet()}"
        log.info "handleOperation request: ${request.parameterMap}"
        log.info "handleOperation params: ${params}"
        def mappedAction = underscoreToCamelCase(operation)
        forward action: "${mappedAction}", params: [data: postObject]
    }
    
    def write() {
        log.info("params to IOService::write(): ${params}")

        String typeOfExport = params.type
        String sequenceName = params.tracks.substring("Annotations-".size())
        
        String fileName
        String sequenceType
        List<String> ontologyIdList = [Gene.class.name,Pseudogene.class.name,RepeatRegion.class.name,TransposableElement.class.name]
        Organism organism = params.organism?Organism.findByCommonName(params.organism):preferenceService.currentOrganismForCurrentUser
        def listOfFeatures = FeatureLocation.executeQuery("select distinct f from FeatureLocation fl join fl.sequence s join fl.feature f where s.organism = :organism and s.name in (:sequenceName) and fl.feature.class in (:ontologyIdList)",
                [sequenceName: sequenceName, ontologyIdList: ontologyIdList, organism:organism])
        Sequence sequence = Sequence.executeQuery("select distinct s from Sequence s where s.name = :sequenceName", [sequenceName: sequenceName])[0]
        def sequenceTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]
        def listOfSequenceAlterations = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes", [sequence: sequence, sequenceTypes: sequenceTypes])
        def featuresToExport = listOfFeatures + listOfSequenceAlterations
        File outputFile = File.createTempFile ("Annotations-" + sequenceName + "-", "." + typeOfExport.toLowerCase())
        if (typeOfExport == "GFF3") {
            // call gff3HandlerService
            fileName = "Annotations-" + sequenceName + "." + typeOfExport.toLowerCase()
            if (params.exportSequence == "true") {
                gff3HandlerService.writeFeaturesToText(outputFile.path, featuresToExport, grailsApplication.config.apollo.gff3.source as String, true, [sequence])
            }
            else {
                gff3HandlerService.writeFeaturesToText(outputFile.path, featuresToExport, grailsApplication.config.apollo.gff3.source as String)
            }
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
        if (!file.exists())
            return
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
