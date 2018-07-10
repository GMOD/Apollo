package org.bbop.apollo

import com.google.common.base.Splitter
import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.DownloadFile
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus

import java.util.zip.GZIPOutputStream

@RestApi(name = "IO Services", description = "Methods for bulk importing and exporting sequence data")
class IOServiceController extends AbstractApolloController {

    def sequenceService
    def featureService
    def gff3HandlerService
    def fastaHandlerService
    def chadoHandlerService
    def preferenceService
    def permissionService
    def configWrapperService
    def requestHandlingService
    def vcfHandlerService

    // fileMap of uuid / filename
    // see #464
    private Map<String, DownloadFile> fileMap = new HashMap<>()

    def index() {}

    def handleOperation(String track, String operation) {
        log.debug "Requested parameterMap: ${request.parameterMap.keySet()}"
        log.debug "upstream params: ${params}"
        JSONObject postObject = findPost()
        def mappedAction = underscoreToCamelCase(operation)
        forward action: "${mappedAction}", params: params
    }

    @RestApiMethod(description = "Write out genomic data.  An example script is used in the https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/get_gff3.groovy"
            , path = "/IOService/write", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)

            , @RestApiParam(name = "type", type = "string", paramType = RestApiParamType.QUERY, description = "Type of annotated genomic features to export 'FASTA','GFF3','CHADO'.")

            , @RestApiParam(name = "seqType", type = "string", paramType = RestApiParamType.QUERY, description = "Type of output sequence 'peptide','cds','cdna','genomic'.")
            , @RestApiParam(name = "format", type = "string", paramType = RestApiParamType.QUERY, description = "'gzip' or 'text'")
            , @RestApiParam(name = "sequences", type = "string", paramType = RestApiParamType.QUERY, description = "Names of references sequences to add (default is all).")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Name of organism that sequences belong to (will default to last organism).")
            , @RestApiParam(name = "output", type = "string", paramType = RestApiParamType.QUERY, description = "Output method 'file','text'")
            , @RestApiParam(name = "exportAllSequences", type = "boolean", paramType = RestApiParamType.QUERY, description = "Export all reference sequences for an organism (over-rides 'sequences')")
            , @RestApiParam(name = "exportGff3Fasta", type = "boolean", paramType = RestApiParamType.QUERY, description = "Export reference sequence when exporting GFF3 annotations.")
            , @RestApiParam(name = "region", type = "String", paramType = RestApiParamType.QUERY, description = "Highlighted genomic region to export in form sequence:min..max  e.g., chr3:1001..1034")
    ]
    )
    @Timed
    def write() {
        try {
            long current = System.currentTimeMillis()
            JSONObject dataObject = permissionService.handleInput(request,params)
            if (!permissionService.hasPermissions(dataObject, PermissionEnum.READ)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            String typeOfExport = dataObject.type
            String sequenceType = dataObject.seqType
            Boolean exportAllSequences = dataObject.exportAllSequences ? Boolean.valueOf(dataObject.exportAllSequences) : false
            Boolean exportGff3Fasta = dataObject.exportGff3Fasta ? Boolean.valueOf(dataObject.exportGff3Fasta) : false
            String output = dataObject.output
            String adapter = dataObject.adapter
            String format = dataObject.format
            String region = dataObject.region
            def sequences = dataObject.sequences // can be array or string
            Organism organism = dataObject.organism ? preferenceService.getOrganismForTokenInDB(dataObject.organism) : preferenceService.getCurrentOrganismForCurrentUser(dataObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))

            def st = System.currentTimeMillis()
            def queryParams = [organism: organism]
            def features = []

            if(exportAllSequences){
                sequences = []
            }
            if (sequences) {
                queryParams.sequences = sequences
            }

            if (typeOfExport == FeatureStringEnum.TYPE_VCF.value) {
                queryParams['viewableAnnotationList'] = requestHandlingService.viewableSequenceAlterationList
                features = SequenceAlteration.createCriteria().list() {
                    featureLocations {
                        sequence {
                            eq('organism', organism)
                            if (sequences) {
                                'in'('name', sequences)
                            }
                        }
                    }
                    'in'('class', requestHandlingService.viewableSequenceAlterationList)
                }

                log.debug "IOService query: ${System.currentTimeMillis() - st}ms"
            }
            else {
                queryParams['viewableAnnotationList'] = requestHandlingService.viewableAnnotationList

                // caputures 3 level indirection, joins feature locations only. joining other things slows it down
                def genes = Gene.executeQuery("select distinct f from Gene f join fetch f.featureLocations fl join fetch f.parentFeatureRelationships pr join fetch pr.childFeature child join fetch child.featureLocations join fetch child.childFeatureRelationships join fetch child.parentFeatureRelationships cpr join fetch cpr.childFeature subchild join fetch subchild.featureLocations join fetch subchild.childFeatureRelationships left join fetch subchild.parentFeatureRelationships where fl.sequence.organism = :organism and f.class in (:viewableAnnotationList)" + (sequences ? " and fl.sequence.name in (:sequences)" : ""), queryParams)
                // captures rest of feats
                def otherFeats = Feature.createCriteria().list() {
                    featureLocations {
                        sequence {
                            eq('organism', organism)
                            if (sequences) {
                                'in'('name', sequences)
                            }
                        }
                    }
                    'in'('class', requestHandlingService.viewableAlterations + requestHandlingService.viewableAnnotationFeatureList)
                }
                log.debug "${otherFeats}"
                features = genes + otherFeats

                log.debug "IOService query: ${System.currentTimeMillis() - st}ms"
            }

            def sequenceList = Sequence.createCriteria().list() {
                eq('organism', organism)
                if (sequences) {
                    'in'('name', sequences)
                }
            }

            File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())
            String fileName

            if (typeOfExport == FeatureStringEnum.TYPE_GFF3.getValue()) {
                // adding sequence alterations to list of features to export
                if (!exportAllSequences  && sequences != null && !(sequences.class == JSONArray.class)) {
                    fileName = "Annotations-" + sequences + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                } else {
                    fileName = "Annotations" + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                }
                // call gff3HandlerService
                if (exportGff3Fasta) {
                    gff3HandlerService.writeFeaturesToText(outputFile.path, features, grailsApplication.config.apollo.gff3.source as String, true, sequenceList)
                } else {
                    gff3HandlerService.writeFeaturesToText(outputFile.path, features, grailsApplication.config.apollo.gff3.source as String)
                }
            } else if (typeOfExport == FeatureStringEnum.TYPE_VCF.value) {
                if (!exportAllSequences  && sequences != null && !(sequences.class == JSONArray.class)) {
                    fileName = "Annotations-" + sequences + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                } else {
                    fileName = "Annotations" + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                }
                // call vcfHandlerService
                vcfHandlerService.writeVariantsToText(organism, features, outputFile.path, grailsApplication.config.apollo.gff3.source as String)
            } else if (typeOfExport == FeatureStringEnum.TYPE_FASTA.getValue()) {
                if (!exportAllSequences  && sequences != null && !(sequences.class == JSONArray.class)) {
                    String regionString = (region && adapter == FeatureStringEnum.HIGHLIGHTED_REGION.value) ? region : ""
                    fileName = "Annotations-" + sequences + "${regionString}." + sequenceType + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                } else {
                    fileName = "Annotations" + "." + sequenceType + "." + typeOfExport.toLowerCase() + (format == "gzip" ? ".gz" : "")
                }

                // call fastaHandlerService
                if(region && adapter == FeatureStringEnum.HIGHLIGHTED_REGION.value){
                    String track = region.split(":")[0]
                    String locationString = region.split(":")[1]
                    Integer min = locationString.split("\\.\\.")[0] as Integer
                    Integer max = locationString.split("\\.\\.")[1] as Integer
                    // its an exclusive fmin, so must subtract one
                    --min
                    Sequence sequence = Sequence.findByOrganismAndName(organism,track)

                    String defline = String.format(">Genomic region %s - %s\n",region,sequence.organism.commonName);
                    String genomicSequence = defline
                    genomicSequence += Splitter.fixedLength(FastaHandlerService.NUM_RESIDUES_PER_LINE).split(sequenceService.getGenomicResiduesFromSequenceWithAlterations(sequence,min,max, Strand.POSITIVE)).join("\n")
                    outputFile.text = genomicSequence
                }
                else{
                    fastaHandlerService.writeFeatures(features, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT,region)
                }
            } else if (typeOfExport == FeatureStringEnum.TYPE_CHADO.getValue()) {
                if (sequences) {
                    render chadoHandlerService.writeFeatures(organism, sequenceList, features)
                } else {
                    render chadoHandlerService.writeFeatures(organism, [], features, exportAllSequences)
                }
                 return // ??
            }

            //generating a html fragment with the link for download that can be rendered on client side
            String uuidString = UUID.randomUUID().toString()
            DownloadFile downloadFile = new DownloadFile(
                    uuid: uuidString
                    , path: outputFile.path
                    , fileName: fileName
            )
            log.debug "${uuidString}"
            fileMap.put(uuidString, downloadFile)

            if (output == "file") {

                def jsonObject = [
                        "uuid"      : uuidString,
                        "exportType": typeOfExport,
                        "seqType"   : sequenceType,
                        "format"    : format,
                        "filename"  : fileName
                ]
                render jsonObject as JSON
            } else {
                render text: outputFile.text
            }
            log.debug "Total IOService export time ${System.currentTimeMillis() - current}ms"
        }
        catch (Exception e) {
            def error = [error: e.message]
            e.printStackTrace()
            render error as JSON
        }
    }

    @RestApiMethod(description = "This is used to retrieve the a download link once the write operation was initialized using output: file."
            , path = "/IOService/download", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "uuid", type = "string", paramType = RestApiParamType.QUERY, description = "UUID that holds the key to the stored download.")
            , @RestApiParam(name = "format", type = "string", paramType = RestApiParamType.QUERY, description = "'gzip' or 'text'")
    ]
    )
    @Timed
    def download() {
        String uuid = params.uuid
        DownloadFile downloadFile = fileMap.remove(uuid)
        def file
        if (downloadFile) {
            file = new File(downloadFile.path)
            if (!file.exists()) {
                render text: "Error: file does not exist"
                return
            }
        } else {
            render text: "Error: uuid did not map to file. Please try to re-download"
            return
        }

        response.setHeader("Content-disposition", "attachment; filename=${downloadFile.fileName}")
        if (params.format == "gzip") {
            new GZIPOutputStream(response.outputStream).withWriter { it << file.text }
//            def output = new BufferedOutputStream(new GZIPOutputStream(response.outputStream))
//            output << file.text
        } else {
            def outputStream = response.outputStream
            outputStream << file.text
            outputStream.flush()
            outputStream.close()
        }

        file.delete()
    }

    def chadoExportStatus() {
        JSONObject returnObject = new JSONObject()
        returnObject.export_status = configWrapperService.hasChadoDataSource().toString()
        render returnObject
    }
}
