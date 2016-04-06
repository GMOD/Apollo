package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.DownloadFile
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import java.util.zip.GZIPOutputStream
import org.springframework.http.HttpStatus
import org.bbop.apollo.gwt.shared.PermissionEnum

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

    // fileMap of uuid / filename
    // see #464
    private Map<String,DownloadFile> fileMap = new HashMap<>()

    def index() { }
    
    def handleOperation(String track, String operation) {
        log.debug "Requested parameterMap: ${request.parameterMap.keySet()}"
        log.debug "upstream params: ${params}"
        JSONObject postObject = findPost()
        def mappedAction = underscoreToCamelCase(operation)
        forward action: "${mappedAction}", params: params
    }

    @RestApiMethod(description="Write out genomic data.  An example script is used in the https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/get_gff3.groovy"
            ,path="/ioService/write",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)

            ,@RestApiParam(name="type", type="string", paramType = RestApiParamType.QUERY,description = "Type of export 'FASTA','GFF3','CHADO'")


            ,@RestApiParam(name="seqType", type="string", paramType = RestApiParamType.QUERY,description = "Type of output sequence 'peptide','cds','cdna','genomic'")
            ,@RestApiParam(name="format", type="string", paramType = RestApiParamType.QUERY,description = "'gzip' or 'text'")
            ,@RestApiParam(name="sequences", type="string", paramType = RestApiParamType.QUERY,description = "Names of references sequences to add.")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "Name of organism that sequences belong to.")
            ,@RestApiParam(name="output", type="string", paramType = RestApiParamType.QUERY,description = "Output method 'file','text'")
            ,@RestApiParam(name="exportAllSequences", type="boolean", paramType = RestApiParamType.QUERY,description = "Export all sequences for an organism (over-rides 'sequences')")
            ,@RestApiParam(name="exportGff3Fasta", type="boolean", paramType = RestApiParamType.QUERY,description = "Export sequences when exporting gff3")
    ]
    )
    @Timed
    def write() {
        try {
            long current = System.currentTimeMillis()
            JSONObject dataObject = (request.JSON ?: params) as JSONObject
            if(params.data) dataObject=JSON.parse(params.data)
            if(!permissionService.hasPermissions(dataObject, PermissionEnum.READ)){
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            String typeOfExport = dataObject.type
            String sequenceType = dataObject.seqType
            String exportAllSequences = dataObject.exportAllSequences
            String exportGff3Fasta = dataObject.exportGff3Fasta
            String chadoExportType = dataObject.chadoExportType
            String output = dataObject.output
            String format = dataObject.format
            def sequences = dataObject.sequences // can be array or string
            Organism organism = dataObject.organism?Organism.findByCommonName(dataObject.organism):preferenceService.getCurrentOrganismForCurrentUser()


            def st=System.currentTimeMillis()
            def queryParams = [viewableAnnotationList: requestHandlingService.viewableAnnotationList, organism: organism]
            if(sequences) queryParams.sequences = sequences
            // caputures 3 level indirection, joins feature locations only. joining other things slows it down
            def genes = Gene.executeQuery("select distinct f from Gene f join fetch f.featureLocations fl join fetch f.parentFeatureRelationships pr join fetch pr.childFeature child join fetch child.featureLocations join fetch child.childFeatureRelationships join fetch child.parentFeatureRelationships cpr join fetch cpr.childFeature subchild join fetch subchild.featureLocations join fetch subchild.childFeatureRelationships left join fetch subchild.parentFeatureRelationships where fl.sequence.organism = :organism and f.class in (:viewableAnnotationList)" + (sequences? " and fl.sequence.name in (:sequences)":""), queryParams)
            // captures rest of feats
            def otherFeats=Feature.createCriteria().list() {
                featureLocations {
                    sequence {
                        eq('organism',organism)
                        if(sequences) {
                            'in'('name',sequences)
                        }
                    }
                }
                'in'('class',requestHandlingService.viewableAlterations+requestHandlingService.viewableAnnotationFeatureList)
            }
            log.debug "${otherFeats}"
            def features = genes+otherFeats

            log.debug "IOService query: ${System.currentTimeMillis()-st}ms"
           
            def sequenceList = Sequence.createCriteria().list() {
                eq('organism',organism)
                if(sequences) {
                    'in'('name',sequences)
                }
            }
            File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())
            String fileName

            if (typeOfExport == FeatureStringEnum.TYPE_GFF3.getValue()) {
                // adding sequence alterations to list of features to export
                if(exportAllSequences!="true"&&sequences!=null&&!(sequences.class == JSONArray.class)) {
                    fileName = "Annotations-" + sequences + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }
                else {
                    fileName = "Annotations" + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }
                // call gff3HandlerService
                if (exportGff3Fasta == "true") {
                    gff3HandlerService.writeFeaturesToText(outputFile.path, features, grailsApplication.config.apollo.gff3.source as String, true, sequenceList)
                } else {
                    gff3HandlerService.writeFeaturesToText(outputFile.path, features, grailsApplication.config.apollo.gff3.source as String)
                }
            } else if (typeOfExport == FeatureStringEnum.TYPE_FASTA.getValue()) {
                if(exportAllSequences!="true"&&sequences!=null&&!(sequences.class == JSONArray.class)) {
                    fileName = "Annotations-" + sequences + "." + sequenceType + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }
                else {
                    fileName = "Annotations" + "." + sequenceType + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }

                // call fastaHandlerService
                fastaHandlerService.writeFeatures(features, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
            }
            else if (typeOfExport == FeatureStringEnum.TYPE_CHADO.getValue()){
                JSONObject returnObject = new JSONObject()
                if (sequences) {
                    returnObject = chadoHandlerService.writeFeatures(organism, sequenceList, features)
                }
                else {
                    returnObject = chadoHandlerService.writeFeatures(organism, [], features, exportAllSequences.equals("true"))
                }

                render returnObject
            }

            //generating a html fragment with the link for download that can be rendered on client side
            String uuidString = UUID.randomUUID().toString()
            DownloadFile downloadFile = new DownloadFile(
                    uuid: uuidString
                    ,path: outputFile.path
                    ,fileName: fileName
            )
            log.debug "${uuidString}"
            fileMap.put(uuidString,downloadFile)

            if(output=="file") {

                def jsonObject = [
                    "uuid":uuidString,
                    "exportType": typeOfExport,
                    "seqType": sequenceType,
                    "format": format,
                    "filename": fileName
                ]
                render jsonObject as JSON
            }
            else {
                render text: outputFile.text
            }
            log.debug "Total IOService export time ${System.currentTimeMillis()-current}ms"
        }
        catch(Exception e) {
            def error=[error: e.message]
            e.printStackTrace()
            render error as JSON
        }
    }

    @RestApiMethod(description="This is used to retrieve the a download link once the write operation was initialized using output: file."
            ,path="/ioService/download",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="uuid", type="string", paramType = RestApiParamType.QUERY,description = "UUID that holds the key to the stored download.")
            ,@RestApiParam(name="format", type="string", paramType = RestApiParamType.QUERY,description = "'gzip' or 'text'")
    ]
    )
    @Timed
    def download() {
        String uuid = params.uuid
        DownloadFile downloadFile = fileMap.remove(uuid)
        def file
        if(downloadFile) {
            file = new File(downloadFile.path)
            if (!file.exists()) {
                render text: "Error: file does not exist"
                return
            }
        }
        else {
            render text: "Error: uuid did not map to file. Please try to re-download"
            return
        }

        response.setHeader("Content-disposition", "attachment; filename=${downloadFile.fileName}")
        if(params.format=="gzip") {
            new GZIPOutputStream(response.outputStream).withWriter{ it << file.text }
//            def output = new BufferedOutputStream(new GZIPOutputStream(response.outputStream))
//            output << file.text
        }
        else {
            def outputStream = response.outputStream
            outputStream << file.text
            outputStream.flush()
            outputStream.close()
        }

        file.delete()
    }

    def chadoExportStatus() {
        boolean exportStatus = false
        JSONObject returnObject = new JSONObject()
        returnObject.export_status = configWrapperService.hasChadoDataSource().toString()
        render returnObject
    }
}
