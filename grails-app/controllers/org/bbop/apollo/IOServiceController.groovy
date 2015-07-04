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
        List<String> viewableAnnotationList = [
                MRNA.class.name,
                Pseudogene.class.name,
                RepeatRegion.class.name,
                TransposableElement.class.name
        ]

        String fileName="Annotations-" + sequenceName + "." + typeOfExport.toLowerCase()
        String sequenceType
        Organism organism = params.organism?Organism.findByCommonName(params.organism):preferenceService.currentOrganismForCurrentUser
        File outputFile = File.createTempFile ("Annotations-" + sequenceName + "-", "." + typeOfExport.toLowerCase())
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)))
        List<Feature> exons=new ArrayList<Feature>();
        List<Feature> children=new ArrayList<Feature>();
        List<GFF3Entry> entries=new ArrayList<GFF3Entry>();
        String source="webapollo"
        if (typeOfExport == "GFF3") {
            // call gff3HandlerService
            long start = System.currentTimeMillis();
            List queryResults = Feature.executeQuery("select f,fr.childFeature,cfr.parentFeature from Feature f join f.featureLocations fl join f.parentFeatureRelationships fr join f.childFeatureRelationships cfr where fl.sequence.name = :sequence and f.class in (:viewableAnnotationList)",
                    [sequence: sequenceName, viewableAnnotationList: viewableAnnotationList])
            long durationInMilliseconds = System.currentTimeMillis()-start;
            log.debug "selecting top-level features ${durationInMilliseconds}"
            start = System.currentTimeMillis();
            def last_parent_id
            def last_transcript_id
            queryResults.each { result ->
                Feature transcript=result[0]
                Feature parent=result[2]
                Feature child=result[1]
                if (parent && parent.id != last_parent_id) {
                    last_parent_id = parent.id
                    log.debug "parent ${parent.id}"
                    gff3HandlerService.convertToEntry(parent,null,source,entries)
                }
                if(transcript&&last_transcript_id==null) {
                    log.debug "first transcript ${transcript.id}"
                    last_transcript_id=transcript.id
                }
                else if(transcript&&transcript.id != last_transcript_id) {
                    log.debug "transcript ${transcript.id}"
                    last_transcript_id = transcript.id
                    gff3HandlerService.convertToEntry(transcript,exons,children,parent, source,entries)
                }


                if(child.cvTerm=="exon") {
                    exons.add(child)
                }
                else {
                    children.add(child)
                }
            }
            for(def entry : entries) {
                out.println entry
            }
            durationInMilliseconds = System.currentTimeMillis()-start;
            log.debug "convert to gff3 ${durationInMilliseconds}"
            out.close()

        }



        //generating a html fragment with the link for download that can be rendered on client side
        String htmlResponseString = "<html><head></head><body><iframe name='hidden_iframe' style='display:none'></iframe>"+
           "<a href='IOService/download/?filePath=${outputFile.path}&fileType=${typeOfExport}&fileName=${fileName}' target='hidden_iframe'>${fileName}</a></body></html>"

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
