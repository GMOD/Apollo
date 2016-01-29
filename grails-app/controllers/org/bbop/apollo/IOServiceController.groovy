package org.bbop.apollo

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
    def requestHandlingService
    def preferenceService
    def permissionService
    def configWrapperService

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

            ,@RestApiParam(name="type", type="string", paramType = RestApiParamType.QUERY,description = "Type of export 'FASTA','GFF3'")


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
            String output = dataObject.output
            String format = dataObject.format
            def sequences = dataObject.sequences // can be array or string
            Organism organism = dataObject.organism?Organism.findByCommonName(dataObject.organism):preferenceService.getCurrentOrganismForCurrentUser()


            //def results = Gene.executeQuery("select distinct f, child, childLocation, subChild from Gene f left outer join fetch f.featureDBXrefs left outer join fetch f.featureSynonyms left outer join fetch f.owners left outer join fetch f.featureProperties join fetch f.featureLocations fl join fetch f.parentFeatureRelationships pr join pr.childFeature child join child.featureLocations childLocation join child.parentFeatureRelationships cpr join cpr.childFeature subchild where fl.sequence.name in (:sequences) and f.class in (:viewableAnnotationList)", [sequences: sequences, viewableAnnotationList: requestHandlingService.viewableAnnotationList])
            def st=System.currentTimeMillis()
            
            //def features = Gene.executeQuery("from Gene f left join fetch f.synonyms left join fetch f.featureDBXrefs left join fetch f.featureProperties left join fetch f.owners join fetch f.featureLocations fl join fetch f.parentFeatureRelationships pr join fetch pr.childFeature child left join fetch child.synonyms left join fetch child.featureDBXrefs left join fetch child.featureProperties left join fetch child.owners join fetch child.featureLocations join fetch child.childFeatureRelationships join fetch child.parentFeatureRelationships cpr join fetch cpr.childFeature subchild join fetch subchild.featureLocations join fetch subchild.childFeatureRelationships left join fetch subchild.parentFeatureRelationships where fl.sequence.name in (:sequences) and f.class in (:viewableAnnotationList)", [sequences: sequences, viewableAnnotationList: requestHandlingService.viewableAnnotationList])
            def queryParams = [viewableAnnotationList: requestHandlingService.viewableAnnotationList]
            if(sequences) queryParams.sequences = sequences
            def features = Gene.executeQuery("select distinct f from Gene f join fetch f.featureLocations fl join fetch f.parentFeatureRelationships pr join fetch pr.childFeature child join fetch child.featureLocations join fetch child.childFeatureRelationships join fetch child.parentFeatureRelationships cpr join fetch cpr.childFeature subchild join fetch subchild.featureLocations join fetch subchild.childFeatureRelationships left join fetch subchild.parentFeatureRelationships where f.class in (:viewableAnnotationList)" + (sequences? " and where fl.sequence.name in (:sequences)":""), queryParams)

            log.debug "TOTALSUB ${System.currentTimeMillis()-st} ${features.size}"
           
            def sequenceList = Sequence.createCriteria().list() {
                eq('organism',organism)
                if(sequences) {
                    'in'('name',sequences)
                }
            }
            File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())
            String fileName

            if (typeOfExport == "GFF3") {
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
            } else if (typeOfExport == "FASTA") {
                if(exportAllSequences!="true"&&sequences!=null&&!(sequences.class == JSONArray.class)) {
                    fileName = "Annotations-" + sequences + "." + sequenceType + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }
                else {
                    fileName = "Annotations" + "." + sequenceType + "." + typeOfExport.toLowerCase() + (format=="gzip"?".gz":"")
                }

                // call fastaHandlerService
                fastaHandlerService.writeFeatures(features, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
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
            log.debug "TOTAL ${System.currentTimeMillis()-current}"
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
}
