package org.bbop.apollo

import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.http.HttpStatus

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Principal
import java.text.DateFormat

import static grails.async.Promises.*


//import grails.compiler.GrailsCompileStatic
import grails.converters.JSON

//import org.bbop.apollo.editor.AnnotationEditor
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

/**
 * From the AnnotationEditorService
 */
//@GrailsCompileStatic
class AnnotationEditorController extends AbstractApolloController implements AnnotationListener {


    def featureService
    def sequenceService
    def configWrapperService
    def featureRelationshipService
    def featurePropertyService
    def requestHandlingService
    def transcriptService
    def exonService
    def permissionService
    def preferenceService
    def sequenceSearchService


    def index() {
        log.debug "bang "
    }

    // Map the operation specified in the URL to a controller
    def handleOperation(String track, String operation) {
        JSONObject postObject = findPost()
        operation = postObject.get(REST_OPERATION)
        def mappedAction = underscoreToCamelCase(operation)
        log.debug "${operation} -> ${mappedAction}"
        log.debug "Controller: " + params.controller
        forward action: "${mappedAction}", params: [data: postObject]
    }

    /**
     * @return
     */
    def getUserPermission() {
        log.debug "getting user permission !! ${params.data}"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        log.debug "principal: " + SecurityUtils.subject.principal
        String username = SecurityUtils.subject.principal
        int permission = PermissionEnum.NONE.value
        if (username) {

            log.debug "input username ${username}"
            User user = User.findByUsername(username)
            log.debug "found a user ${user} for username: ${username}"

            Organism organism = preferenceService.getCurrentOrganism(user)
            if (!organism) {
                log.error "somehow no organism shown, getting for all"
            }
            Map<String, Integer> permissions
            List<PermissionEnum> permissionEnumList = permissionService.getOrganismPermissionsForUser(organism, user)
            permission = permissionService.findHighestEnumValue(permissionEnumList)
            permissions = new HashMap<>()
            permissions.put(username, permission)
            permissions = permissionService.getPermissionsForUser(user)
            if (permissions) {
                session.setAttribute("permissions", permissions);
            }
            if (permissions.values().size() > 0) {
                permission = permissions.values().iterator().next();
            }
        }

        returnObject.put(REST_PERMISSION, permission)
        returnObject.put(REST_USERNAME, username)

        render returnObject
    }


    def getDataAdapters() {
        // temporary workaround
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        String jsonString = "[{\"permission\":1,\"key\":\"GFF3\",\"options\":\"output=file&format=gzip&type=GFF3\"},{\"permission\":1,\"key\":\"FASTA\",\"data_adapters\":[{\"permission\":1,\"key\":\"peptide\",\"options\":\"output=file&format=gzip&type=FASTA&seqType=peptide\"},{\"permission\":1,\"key\":\"cDNA\",\"options\":\"output=file&format=gzip&type=FASTA&seqType=cdna\"},{\"permission\":1,\"key\":\"CDS\",\"options\":\"output=file&format=gzip&type=FASTA&seqType=cds\"}]}]"
        JSONArray dataAdaptersArray = new JSONArray()

        if (!permissionService.checkPermissions(PermissionEnum.EXPORT)) {
            returnObject.put(REST_DATA_ADAPTERS, dataAdaptersArray)
            render returnObject
            return
        }
        dataAdaptersArray = JSON.parse(jsonString) as JSONArray
        returnObject.put(REST_DATA_ADAPTERS, dataAdaptersArray)
        log.debug "return object from getDataAdapters: ${returnObject.toString()}"
        render returnObject
    }

    def getHistoryForFeatures() {
        log.debug "getting history !! ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        inputObject.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        permissionService.checkPermissions(inputObject, PermissionEnum.READ)

        JSONObject historyContainer = createJSONFeatureContainer();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            JSONArray history = new JSONArray();
            jsonFeature.put(FeatureStringEnum.HISTORY.value, history);
            List<FeatureEvent> transactionList = FeatureEvent.findAllByUniqueName(feature.uniqueName, [sort: "dateCreated", order: "asc"])
            for (int j = 0; j < transactionList.size(); ++j) {
                FeatureEvent transaction = transactionList.get(j);
                JSONObject historyItem = new JSONObject();
                historyItem.put(REST_OPERATION, transaction.operation.name());
                historyItem.put(FeatureStringEnum.EDITOR.value, transaction.getEditor().username);
                historyItem.put(FeatureStringEnum.DATE.value, dateFormat.format(transaction.dateCreated));
                if (transaction.current) {
                    historyItem.put(FeatureStringEnum.CURRENT.value, true);
                } else {
                    historyItem.put(FeatureStringEnum.CURRENT.value, false);
                }
                JSONArray historyFeatures = new JSONArray();
                historyItem.put(FeatureStringEnum.FEATURES.value, historyFeatures);

                if (transaction.newFeaturesJsonArray) {
                    JSONArray newFeaturesJsonArray = (JSONArray) JSON.parse(transaction.newFeaturesJsonArray)
                    for (int featureIndex = 0; featureIndex < newFeaturesJsonArray.size(); featureIndex++) {
                        JSONObject featureJsonObject = newFeaturesJsonArray.getJSONObject(featureIndex)
                        // TODO: this needs to be functional
                        if (false && transaction.getOperation().equals(FeatureOperation.SPLIT_TRANSCRIPT)) {
////                        if (gbolFeature.overlaps(f)) {
//                            if (overlapperService.overlaps(feature.featureLocation,f.featureLocation,true)) {
////                                if (f.getUniqueName().equals(jsonFeature.getString("uniquename"))) {
//                            historyFeatures.put(featureService.convertFeatureToJSON(f));
                            throw new RuntimeException("split transcript operations not supported yet")
                        }
//                    } else {
//                        historyFeatures.put(featureService.convertFeatureToJSON(f));
                        historyFeatures.put(featureJsonObject);
//                    }
                    }
                    history.put(historyItem);
                }
            }
            historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature);
        }

        render historyContainer as JSON
    }


    def getTranslationTable() {
        log.debug "get translation table!! ${params}"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        TranslationTable translationTable = SequenceTranslationHandler.getDefaultTranslationTable()
        JSONObject ttable = new JSONObject();
        for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
            ttable.put(t.getKey(), t.getValue());
        }
        returnObject.put(REST_TRANSLATION_TABLE, ttable);
        render returnObject
    }


    def addFeature() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setExonBoundaries() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setExonBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    def addExon() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    /**
     * // input
     *{"operation":"add_transcript","track":"Annotations-Group1.2","features":[{"location":{"fmin":247892,"strand":1,"fmax":305356},"name":"geneid_mRNA_CM000054.5_150","children":[{"location":{"fmin":305327,"strand":1,"fmax":305356},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":258308,"strand":1,"fmax":258471},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":247892,"strand":1,"fmax":247976},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":247892,"strand":1,"fmax":305356},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}},{"location":{"fmin":247892,"strand":1,"fmax":305356},"name":"5e5c32e6-ca4a-4b53-85c8-b0f70c76acbd","children":[{"location":{"fmin":247892,"strand":1,"fmax":247976},"name":"00540e13-de64-4fa2-868a-e168e584f55d","uniquename":"00540e13-de64-4fa2-868a-e168e584f55d","type":"exon","date_last_modified":new Date(1415391635593)},{"location":{"fmin":258308,"strand":1,"fmax":258471},"name":"de44177e-ce76-4a9a-8313-1c654d1174aa","uniquename":"de44177e-ce76-4a9a-8313-1c654d1174aa","type":"exon","date_last_modified":new Date(1415391635586)},{"location":{"fmin":305327,"strand":1,"fmax":305356},"name":"fa49095f-cdb9-4734-8659-3286a7c727d5","uniquename":"fa49095f-cdb9-4734-8659-3286a7c727d5","type":"exon","date_last_modified":new Date(1415391635578)},{"location":{"fmin":247892,"strand":1,"fmax":305356},"name":"29b83822-d5a0-4795-b0a9-71b1651ff915","uniquename":"29b83822-d5a0-4795-b0a9-71b1651ff915","type":"cds","date_last_modified":new Date(1415391635600)}],"uniquename":"df08b046-ed1b-4feb-93fc-53adea139df8","type":"mrna","date_last_modified":new Date(1415391635771)}]}*
     * // returned form the fir method
     *{"operation":"ADD","sequenceAlterationEvent":false,"features":[{"location":{"fmin":670576,"strand":1,"fmax":691185},"parent_type":{"name":"gene","cv":{"name":"sequence"}},"name":"geneid_mRNA_CM000054.5_38","children":[{"location":{"fmin":670576,"strand":1,"fmax":670658},"parent_type":{"name":"mRNA","cv":{"name":"sequence"}},"properties":[{"value":"demo","type":{"name":"owner","cv":{"name":"feature_property"}}}],"uniquename":"60072F8198F38EB896FB218D2862FFE4","type":{"name":"exon","cv":{"name":"sequence"}},"date_last_modified":1415391541148,"parent_id":"D1D1E04521E6FFA95FD056D527A94730"},{"location":{"fmin":690970,"strand":1,"fmax":691185},"parent_type":{"name":"mRNA","cv":{"name":"sequence"}},"properties":[{"value":"demo","type":{"name":"owner","cv":{"name":"feature_property"}}}],"uniquename":"CC6058CFA17BD6DB8861CC3B6FA1E4B1","type":{"name":"exon","cv":{"name":"sequence"}},"date_last_modified":1415391541148,"parent_id":"D1D1E04521E6FFA95FD056D527A94730"},{"location":{"fmin":670576,"strand":1,"fmax":691185},"parent_type":{"name":"mRNA","cv":{"name":"sequence"}},"properties":[{"value":"demo","type":{"name":"owner","cv":{"name":"feature_property"}}}],"uniquename":"6D85D94970DE82168B499C75D886FB89","type":{"name":"CDS","cv":{"name":"sequence"}},"date_last_modified":1415391541148,"parent_id":"D1D1E04521E6FFA95FD056D527A94730"}],"properties":[{"value":"demo","type":{"name":"owner","cv":{"name":"feature_property"}}}],"uniquename":"D1D1E04521E6FFA95FD056D527A94730","type":{"name":"mRNA","cv":{"name":"sequence"}},"date_last_modified":1415391541169,"parent_id":"8E2895FDD74F4F9DF9F6785B72E04A50"}]}* @return
     */
    def addTranscript() {
        log.debug "AEC::adding transcript ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def duplicateTranscript() {
        log.debug "AEC::set translation start ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.duplicateTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setTranslationStart() {
        log.debug "AEC::set translation start ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationStart(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setTranslationEnd() {
        log.debug "AEC::set translation end ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationEnd(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setBoundaries() {
        log.debug "AEC::set boundaries ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

/**
 *
 * Should return of form:
 *{"features": [{"location": {"fmin": 511,"strand": - 1,"fmax": 656},
 * parent_type": {"name": "gene","cv": {"name": "sequence"}},"name": "feat"}]}* @return
 */
    def getFeatures() {

        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        returnObject.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
        render requestHandlingService.getFeatures(returnObject)
    }

    def getInformation() {
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (!permissionService.checkPermissions(PermissionEnum.WRITE)) {
            render new JSONObject() as JSON
            return
        }
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value);
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            JSONObject info = new JSONObject();
            info.put(FeatureStringEnum.UNIQUENAME.value, uniqueName);
            info.put("time_accessioned", gbolFeature.lastUpdated)
            info.put("owner", "some username");
            String parentIds = "";
            featureRelationshipService.getParentForFeature(gbolFeature).each {
                if (parentIds.length() > 0) {
                    parentIds += ", ";
                }
                parentIds += it.getUniqueName();
            }
            if (parentIds.length() > 0) {
                info.put("parent_ids", parentIds);
            }

            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(info);
        }

        render featureContainer
    }

/**
 * Provided if not coming thorugh a websocket
 * @param jsonObject
 * @return
 */
    private def fixUserName(JSONObject jsonObject) {
        if (jsonObject.containsKey(FeatureStringEnum.USERNAME.value)) return

        String username = SecurityUtils.subject.principal
        jsonObject.put(FeatureStringEnum.USERNAME.value, username)
    }

    def getSequenceAlterations() {
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        fixUserName(returnObject)

        Sequence sequence = permissionService.checkPermissions(returnObject, PermissionEnum.READ)

        JSONArray jsonFeatures = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, jsonFeatures)
        def sequenceTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]

        // TODO: get alterations from session
        List<SequenceAlteration> sequenceAlterationList = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes"
                , [sequence: sequence, sequenceTypes: sequenceTypes])
        for (SequenceAlteration alteration : sequenceAlterationList) {
            jsonFeatures.put(featureService.convertFeatureToJSON(alteration, true));
        }

        render returnObject
    }


    def getOrganism() {
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        if (organism) {
            render organism as JSON
        } else {
            render new JSONObject()
        }
    }

/**
 * TODO: link to the database for real config values
 * @return
 */
    def getAnnotationInfoEditorConfiguration() {
        log.debug "getting the config "
        JSONObject annotationInfoEditorConfigContainer = new JSONObject();
        JSONArray annotationInfoEditorConfigs = new JSONArray();
        annotationInfoEditorConfigContainer.put(FeatureStringEnum.ANNOTATION_INFO_EDITOR_CONFIGS.value, annotationInfoEditorConfigs);
        JSONObject annotationInfoEditorConfig = new JSONObject();
        annotationInfoEditorConfigs.put(annotationInfoEditorConfig);
        if (configWrapperService.hasStatus()) {
            JSONArray statusArray = new JSONArray()
            annotationInfoEditorConfig.put(FeatureStringEnum.STATUS.value, statusArray);
            Status.all.each { status ->
                statusArray.add(status.value)
            }
        }
        annotationInfoEditorConfig.put(FeatureStringEnum.HASDBXREFS.value, true);
        annotationInfoEditorConfig.put(FeatureStringEnum.HASATTRIBUTES.value, true);
        annotationInfoEditorConfig.put(FeatureStringEnum.HASPUBMEDIDS.value, true);
        annotationInfoEditorConfig.put(FeatureStringEnum.HASGOIDS.value, true);
        annotationInfoEditorConfig.put(FeatureStringEnum.HASCOMMENTS.value, true);
        JSONArray supportedTypes = new JSONArray();
        supportedTypes.add(FeatureStringEnum.DEFAULT.value)
        annotationInfoEditorConfig.put(FeatureStringEnum.SUPPORTED_TYPES.value, supportedTypes);
        log.debug "return config ${annotationInfoEditorConfigContainer}"
        render annotationInfoEditorConfigContainer
    }


    def setDescription() {
//        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
//        return requestHandlingService.setDescription(inputObject)
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setDescription(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setSymbol() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setSymbol(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def setReadthroughStopCodon() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setReadthroughStopCodon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def addSequenceAlteration() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def deleteSequenceAlteration() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def flipStrand() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.flipStrand(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def mergeExons() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeExons(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def splitExon() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    def deleteFeature() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def deleteExon() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def makeIntron() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.makeIntron(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def splitTranscript() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def mergeTranscripts() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeTranscripts(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    def getSequence() {
        log.debug "getSequence ${params.data}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.EXPORT)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        JSONObject featureContainer = createJSONFeatureContainer()
        JSONObject sequenceObject = sequenceService.getSequenceForFeatures(inputObject)
        featureContainer.getJSONArray("features").put(sequenceObject)
        render featureContainer
    }

    def getSequenceSearchTools() {
        log.debug "getSequenceSearchTools ${params.data}"
        render sequenceSearchService.getSequenceSearchTools()
    }

    def getCannedComments() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        render CannedComment.listOrderByComment() as JSON
    }

    def searchSequence() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        println "Organism to string:  ${organism as JSON}"
        render sequenceSearchService.searchSequence(inputObject, organism.getBlatdb())
    }


    def getGff3() {
        log.debug "getGff3 ${params.data}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.EXPORT)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        try {
            File outputFile = File.createTempFile("feature", ".gff3");
            sequenceService.getGff3ForFeature(inputObject, outputFile)
            Charset encoding = Charset.defaultCharset()
            byte[] encoded = Files.readAllBytes(Paths.get(outputFile.getAbsolutePath()))
            String gff3String = new String(encoded, encoding)
            outputFile.delete() // deleting temp file
            render gff3String

        } catch (IOException e) {
            log.debug("Cannot create a temp file for 'get GFF3' operation")
            e.printStackTrace()
        }
    }

    def getAnnotationInfoEditorData() {
        Sequence sequence
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        try {
            sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        } catch (e) {
            log.error(e)
            render new JSONObject() as JSON
            return
        }



        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)


        JSONObject returnObject = createJSONFeatureContainer()

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            log.debug "input json feature ${jsonFeature}"
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            log.debug "feature converted? ${feature}"
            log.debug "retrieved feature ${feature.name} ${feature.uniqueName}"
            JSONObject newFeature = featureService.convertFeatureToJSON(feature, false)

            if (feature.symbol) newFeature.put(FeatureStringEnum.SYMBOL.value, feature.symbol)
            if (feature.description) newFeature.put(FeatureStringEnum.DESCRIPTION.value, feature.description)

            jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, feature.dateCreated.time);
            jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, feature.lastUpdated.time);

            // TODO: add the rest of the attributes
            if (configWrapperService.hasAttributes()) {
                JSONArray properties = new JSONArray();
                newFeature.put(FeatureStringEnum.NON_RESERVED_PROPERTIES.value, properties);
                for (FeatureProperty property : feature.featureProperties) {
                    JSONObject jsonProperty = new JSONObject();
                    jsonProperty.put(FeatureStringEnum.TAG.value, property.getTag());
                    jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                    properties.put(jsonProperty);
                }
            }
            if (configWrapperService.hasDbxrefs() || configWrapperService.hasPubmedIds() || configWrapperService.hasGoIds()) {
                JSONArray dbxrefs = new JSONArray();
                newFeature.put(FeatureStringEnum.DBXREFS.value, dbxrefs);
                for (DBXref dbxref : feature.featureDBXrefs) {
                    JSONObject jsonDbxref = new JSONObject();
                    jsonDbxref.put(FeatureStringEnum.DB.value, dbxref.getDb().getName());
                    jsonDbxref.put(FeatureStringEnum.ACCESSION.value, dbxref.getAccession());
                    dbxrefs.put(jsonDbxref);
                }
            }
            if (configWrapperService.hasComments()) {
                JSONArray comments = new JSONArray();
                newFeature.put(FeatureStringEnum.COMMENTS.value, comments);
                for (Comment comment : featurePropertyService.getComments(feature)) {
                    comments.put(comment.value);
                }
                JSONArray cannedComments = new JSONArray();

                newFeature.put(FeatureStringEnum.CANNED_COMMENTS.value, cannedComments);

                List<FeatureType> featureTypeList = FeatureType.findAllByOntologyId(feature.ontologyId)
                List<String> cannedCommentStrings = new ArrayList<>()
                cannedCommentStrings.addAll(CannedComment.executeQuery("select cc from CannedComment cc join cc.featureTypes ft where ft in (:featureTypeList)",[featureTypeList:featureTypeList]).comment)
                cannedCommentStrings.addAll(CannedComment.executeQuery("select cc from CannedComment cc where cc.featureTypes is empty" ).comment)
                if (cannedCommentStrings != null) {
                    for (String comment : cannedCommentStrings) {
                        cannedComments.put(comment);
                    }
                }
            }
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeature);
        }


        render returnObject
    }


    @MessageMapping("/AnnotationNotification")
    @SendTo("/topic/AnnotationNotification")
    protected String annotationEditor(String inputString, Principal principal) {
        log.debug "Input String:  annotation editor service ${inputString}"
        JSONObject rootElement = (JSONObject) JSON.parse(inputString)
        rootElement.put(FeatureStringEnum.USERNAME.value, principal.name)

        log.debug "AEC::root element: ${rootElement as JSON}"
        String operation = ((JSONObject) rootElement).get(REST_OPERATION)

        String operationName = underscoreToCamelCase(operation)
        log.debug "operationName: ${operationName}"
        def p = task {
            switch (operationName) {
                case "setToDownstreamDonor": requestHandlingService.setDonor(rootElement, false)
                    break
                case "setToUpstreamDonor": requestHandlingService.setDonor(rootElement, true)
                    break
                case "setToDownstreamAcceptor": requestHandlingService.setAcceptor(rootElement, false)
                    break
                case "setToUpstreamAcceptor": requestHandlingService.setAcceptor(rootElement, true)
                    break
                default:
                    boolean foundMethod = false
                    String returnString = null
                    requestHandlingService.getClass().getMethods().each { method ->
                        if (method.name == operationName) {
                            foundMethod = true
                            log.debug "found the method ${operationName}"
                            Feature.withNewSession {
                                returnString = method.invoke(requestHandlingService, rootElement)
                            }
                            return returnString
                        }
                    }
                    if (foundMethod) {
                        return returnString
                    } else {
                        log.error "METHOD NOT found ${operationName}"
                        throw new AnnotationException("Operation ${operationName} not found")
                    }
                    break
            }
        }
        try {
            def results = p.get()
            return results
        } catch (AnnotationException ae) {
            // TODO: should be returning nothing, but then broadcasting specifically to this user
            return sendError(ae, principal.name)
        }

    }

// TODO: handle errors without broadcasting
    protected def sendError(AnnotationException exception, String username) {
        log.debug "excrption ${exception}"
        log.debug "excrption message ${exception.message}"
        log.debug "username ${username}"

        JSONObject errorObject = new JSONObject()
        errorObject.put(REST_OPERATION, FeatureStringEnum.ERROR.name())
        errorObject.put(FeatureStringEnum.ERROR_MESSAGE.value, exception.message)
        errorObject.put(FeatureStringEnum.USERNAME.value, username)

        return errorObject.toString()
    }


    @SendTo("/topic/AnnotationNotification")
    protected String sendAnnotationEvent(String returnString) {
        log.debug "AEC::return operations sent . . ${returnString?.size()}"
        return returnString
    }

    synchronized void handleChangeEvent(AnnotationEvent... events) {
        log.debug "handingling event ${events.length}"
        if (events.length == 0) {
            return;
        }
//        sendAnnotationEvent(events)
        // TODO: this is more than a bit of a hack
//        String sequenceName = "Annotations-${events[0].sequence.name}"
        JSONArray operations = new JSONArray();
        for (AnnotationEvent event : events) {
            JSONObject features = event.getFeatures();
            try {
                features.put("operation", event.getOperation().name());
                features.put("sequenceAlterationEvent", event.isSequenceAlterationEvent());
                operations.put(features);
            }
            catch (JSONException e) {
                log.error("error handling change event ${event}: ${e}")
            }
        }

        sendAnnotationEvent(operations.toString())

    }


    def web_services(){
        render view: "/web_services"
    }

}
