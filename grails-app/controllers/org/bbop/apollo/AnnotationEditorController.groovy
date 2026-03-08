package org.bbop.apollo

import grails.converters.JSON
import groovy.json.JsonBuilder
import org.bbop.apollo.security.ApolloSecurityUtils
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.TranslationTable
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate

import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Principal


/**
 * From the WA1 AnnotationEditorService class.
 *
 * This code primarily provides integration with genomic editing functionality visible in the JBrowse window.
 */
class AnnotationEditorController extends AbstractApolloController implements AnnotationListener {


    FeatureService featureService
    SequenceService sequenceService
    ConfigWrapperService configWrapperService
    FeatureRelationshipService featureRelationshipService
    FeaturePropertyService featurePropertyService
    RequestHandlingService requestHandlingService
    PreferenceService preferenceService
    SequenceSearchService sequenceSearchService
    FeatureEventService featureEventService
    AnnotationEditorService annotationEditorService
    OrganismService organismService
    JsonWebUtilityService jsonWebUtilityService
    CannedCommentService cannedCommentService
    CannedAttributeService cannedAttributeService
    AvailableStatusService availableStatusService
    SimpMessagingTemplate brokerMessagingTemplate


    def index() {
        log.debug "bang "
    }


    // Map the operation specified in the URL to a controller
    def handleOperation(String track, String operation) {
        JSONObject postObject = findPost()
        operation = postObject.get(REST_OPERATION)
        def mappedAction = underscoreToCamelCase(operation)
        log.debug "handleOperation ${params.controller} ${operation} -> ${mappedAction}"
        forward action: "${mappedAction}", params: [data: postObject]
    }

    /**
     * @return
     */
    def getUserPermission() {
        log.debug "getUserPermission ${params.data}"
        JSONObject returnObject = permissionService.handleInput(request, params)

        String username = ApolloSecurityUtils.currentUsername
        if (username) {
            int permission = PermissionEnum.NONE.value

            User user = User.findByUsername(username)
            log.debug "getting user permission for ${user}, returnObject"
//            Organism organism = preferenceService.getOrganismFromPreferences(user,null,returnObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(returnObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
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
            returnObject.put(REST_PERMISSION, permission)
            returnObject.put(REST_USERNAME, username)
            render returnObject as JSON
        } else {
            def errorMessage = [message: "You must first login before editing"]
            response.status = HttpStatus.UNAUTHORIZED.value()
            render errorMessage as JSON
        }
    }

    //TODO: parse permissions
    def getDataAdapters() {
        log.debug "getDataAdapters"
        JSONObject returnObject = permissionService.handleInput(request, params)
        def set = configWrapperService.getDataAdapterTools()

        def obj = new JsonBuilder(set)
        def jre = ["data_adapters": obj.content]
        render jre as JSON
    }

    def getHistoryForFeatures() {
        withPermission(PermissionEnum.READ) { inputObject ->
            if (!inputObject.track && inputObject.sequence) {
                inputObject.track = inputObject.sequence
            }
            JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
            JSONObject historyContainer = jsonWebUtilityService.createJSONFeatureContainer()
            featureEventService.generateHistory(historyContainer, featuresArray)
        }
    }


    def getTranslationTable() {
        withPermission(PermissionEnum.READ) { returnObject ->
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(returnObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            TranslationTable translationTable = organismService.getTranslationTable(organism)

            JSONObject ttable = new JSONObject()
            for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
                ttable.put(t.getKey(), t.getValue())
            }

            JSONArray startProteins = new JSONArray()
            JSONArray stopProteins = new JSONArray()
            for (String startCodon in translationTable.getStartCodons()) {
                startProteins.add(translationTable.getTranslationTable().get(startCodon))
            }
            for (String stopCodon in translationTable.getStopCodons()) {
                stopProteins.add(translationTable.getTranslationTable().get(stopCodon))
            }

            returnObject.put(REST_TRANSLATION_TABLE, ttable)
            returnObject.put(REST_START_PROTEINS, startProteins.unique())
            returnObject.put(REST_STOP_PROTEINS, stopProteins.unique())
            returnObject
        }
    }


    def addFeature() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addFeature(it) }
    }

    def setExonBoundaries() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setExonBoundaries(it) }
    }

    def setShineDalgarnoBoundaries() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setShineDalgarnoBoundaries(it) }
    }

    def addExon() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addExon(it) }
    }

    def addComments() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addComments(it) }
    }

    def deleteComments() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteComments(it) }
    }

    def updateComments() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.updateComments(it) }
    }

    def getComments() {
        withPermission(PermissionEnum.READ) { requestHandlingService.getComments(it) }
    }

    def addTranscript() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addTranscript(it) }
    }

    def duplicateTranscript() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.duplicateTranscript(it) }
    }

    def setTranslationStart() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setTranslationStart(it) }
    }

    def setTranslationEnd() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setTranslationEnd(it) }
    }

    def setLongestOrf() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setLongestOrf(it) }
    }

    def setBoundaries() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setBoundaries(it) }
    }

    def getFeatures() {
        withPermission(PermissionEnum.READ) { requestHandlingService.getFeatures(it) }
    }


    def getSequenceAlterations() {
        withPermission(PermissionEnum.READ) { inputObject ->
            Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.READ)
            JSONArray jsonFeatures = new JSONArray()
            inputObject.put(FeatureStringEnum.FEATURES.value, jsonFeatures)
            List<SequenceAlterationArtifact> sequenceAlterationList = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes"
                    , [sequence: sequence, sequenceTypes: requestHandlingService.viewableAlterations])
            for (SequenceAlterationArtifact alteration : sequenceAlterationList) {
                jsonFeatures.put(featureService.convertFeatureToJSON(alteration, true))
            }
            inputObject
        }
    }


    def getAnnotationInfoEditorConfiguration() {
        JSONObject annotationInfoEditorConfigContainer = new JSONObject();
        JSONArray annotationInfoEditorConfigs = new JSONArray();
        annotationInfoEditorConfigContainer.put(FeatureStringEnum.ANNOTATION_INFO_EDITOR_CONFIGS.value, annotationInfoEditorConfigs);
        JSONObject annotationInfoEditorConfig = new JSONObject();
        annotationInfoEditorConfigs.put(annotationInfoEditorConfig);

        annotationInfoEditorConfig.put(FeatureStringEnum.HASDBXREFS.value, configWrapperService.hasDbxrefs());
        annotationInfoEditorConfig.put(FeatureStringEnum.HASATTRIBUTES.value, configWrapperService.hasAttributes());
        annotationInfoEditorConfig.put(FeatureStringEnum.HASPUBMEDIDS.value, configWrapperService.hasPubmedIds());
        annotationInfoEditorConfig.put(FeatureStringEnum.HASGOIDS.value, configWrapperService.hasGoIds());
        annotationInfoEditorConfig.put(FeatureStringEnum.HASCOMMENTS.value, configWrapperService.hasComments());
        JSONArray supportedTypes = new JSONArray();
        supportedTypes.add(FeatureStringEnum.DEFAULT.value)
        annotationInfoEditorConfig.put(FeatureStringEnum.SUPPORTED_TYPES.value, supportedTypes);
        log.debug "return config ${annotationInfoEditorConfigContainer}"
        render annotationInfoEditorConfigContainer as JSON
    }

    def setName() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setName(it) }
    }

    def setDescription() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setDescription(it) }
    }

    def setSymbol() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setSymbol(it) }
    }

    def setStatus() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setStatus(it) }
    }

    def addAttribute() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addNonReservedProperties(it) }
    }

    def deleteAttribute() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteNonReservedProperties(it) }
    }

    def updateAttribute() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.updateNonReservedProperties(it) }
    }

    def addDbxref() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addNonPrimaryDbxrefs(it) }
    }

    def updateDbxref() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.updateNonPrimaryDbxrefs(it) }
    }

    def deleteDbxref() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteNonPrimaryDbxrefs(it) }
    }

    def getInformation() {
        JSONObject featureContainer = jsonWebUtilityService.createJSONFeatureContainer();
        JSONObject inputObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(inputObject,PermissionEnum.READ)
        } catch (Exception e) {
            def error = [error: e.message]
            render error as JSON
            return
        }
        if (!permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)) {
            render new JSONObject() as JSON
            return
        }
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value);
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            JSONObject info = new JSONObject();
            info.put(FeatureStringEnum.UNIQUENAME.value, uniqueName)
            info.put("time_accessioned", gbolFeature.lastUpdated)
            info.put("owner", gbolFeature.owner ? gbolFeature.owner.username : "N/A")
            info.put("location", gbolFeature.featureLocation.fmin)
            if(gbolFeature instanceof SequenceAlterationArtifact){
                info.put("length", gbolFeature.offset)
            }
            if(gbolFeature instanceof SequenceAlteration && gbolFeature.alterationResidue){
                info.put("length", gbolFeature?.alterationResidue?.size())
            }
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
            def featureProperties = featurePropertyService.getNonReservedProperties(gbolFeature);
            featureProperties.each {
                info.put(it.tag, it.value);
            }
            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(info);
        }

        render featureContainer as JSON
    }

    def getAttributes() {
        withPermission(PermissionEnum.READ) { inputObject ->
            String uniqueName = inputObject.getString(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            JSONArray attributes = new JSONArray()
            feature.featureProperties.each {
                if (it.ontologyId != Comment.ontologyId && it.tag != null) {
                    JSONObject attributeObject = new JSONObject()
                    attributeObject.put(FeatureStringEnum.TAG.value, it.tag)
                    attributeObject.put(FeatureStringEnum.VALUE.value, it.value)
                    attributes.add(attributeObject)
                }
            }
            JSONObject returnObject = new JSONObject()
            returnObject.put(FeatureStringEnum.ATTRIBUTES.value, attributes)
            returnObject
        }
    }

    def getDbxrefs() {
        withPermission(PermissionEnum.READ) { inputObject ->
            String uniqueName = inputObject.getString(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            JSONArray annotations = new JSONArray()
            feature.featureDBXrefs.each {
                JSONObject dbxrefObject = new JSONObject()
                dbxrefObject.put(FeatureStringEnum.TAG.value, it.db.name)
                dbxrefObject.put(FeatureStringEnum.VALUE.value, it.accession)
                annotations.add(dbxrefObject)
            }
            JSONObject returnObject = new JSONObject()
            returnObject.put("annotations", annotations)
            returnObject
        }
    }

    def setReadthroughStopCodon() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.setReadthroughStopCodon(it) }
    }

    def addSequenceAlteration() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.addSequenceAlteration(it) }
    }

    def deleteSequenceAlteration() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteSequenceAlteration(it) }
    }

    def flipStrand() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.flipStrand(it) }
    }

    def mergeExons() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.mergeExons(it) }
    }

    def splitExon() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.splitExon(it) }
    }

    def deleteFeature() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteFeature(it) }
    }


    def deleteVariantEffectsForSequences() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.removeVariantEffect(it) }
    }

    def deleteFeaturesForSequences() {
        withPermission(PermissionEnum.WRITE) { inputObject ->
            JSONArray features = new JSONArray()
            inputObject.features = features
            List<Long> sequenceList = inputObject.sequence.collect {
                return Long.valueOf(it.id)
            }
            List<String> featureUniqueNames = Feature.executeQuery("select f.uniqueName from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s   where f.childFeatureRelationships is empty and s.id in (:sequenceList) and f.class in (:viewableTypes) ", [sequenceList: sequenceList, viewableTypes: requestHandlingService.viewableAnnotationList])
            featureUniqueNames.each {
                def jsonObject = new JSONObject()
                jsonObject.put(FeatureStringEnum.UNIQUENAME.value, it)
                features.add(jsonObject)
            }
            inputObject.remove("sequence")
            requestHandlingService.deleteFeature(inputObject)
        }
    }

    def deleteExon() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.deleteExon(it) }
    }

    def makeIntron() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.makeIntron(it) }
    }

    def splitTranscript() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.splitTranscript(it) }
    }

    def mergeTranscripts() {
        withPermission(PermissionEnum.WRITE) { requestHandlingService.mergeTranscripts(it) }
    }

    def getSequence() {
        withPermission(PermissionEnum.EXPORT) { inputObject ->
            JSONObject featureContainer = jsonWebUtilityService.createJSONFeatureContainer()
            JSONObject sequenceObject = sequenceService.getSequenceForFeatures(inputObject)
            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(sequenceObject)
            featureContainer
        }
    }

    def getSequenceSearchTools() {
        log.debug "getSequenceSearchTools ${params.data}"
        def set = configWrapperService.getSequenceSearchTools()
        def obj = new JsonBuilder(set)
        def jre = ["sequence_search_tools": obj.content]
        render jre as JSON
    }

    private String getOntologyIdForType(String type) {
        JSONObject cvTerm = new JSONObject()
        if (type.toUpperCase() == Gene.cvTerm.toUpperCase()) {
            JSONObject cvTermName = new JSONObject()
            cvTermName.put(FeatureStringEnum.NAME.value, FeatureStringEnum.CV.value)
            cvTerm.put(FeatureStringEnum.CV.value, cvTermName)
            cvTerm.put(FeatureStringEnum.NAME.value, type)
        } else {
            JSONObject cvTermName = new JSONObject()
            cvTermName.put(FeatureStringEnum.NAME.value, FeatureStringEnum.SEQUENCE.value)
            cvTerm.put(FeatureStringEnum.CV.value, cvTermName)
            cvTerm.put(FeatureStringEnum.NAME.value, type)
        }
        return featureService.convertJSONToOntologyId(cvTerm)
    }

    private List<FeatureType> getFeatureTypeListForType(String type) {
        String ontologyId = getOntologyIdForType(type)
        return FeatureType.findAllByOntologyId(ontologyId)
    }

    def getCannedComments() {
        withPermission(PermissionEnum.READ) { inputObject ->
            Organism organism = Organism.findById(inputObject.getLong(FeatureStringEnum.ORGANISM_ID.value))
            String type = inputObject.getString(FeatureStringEnum.TYPE.value)
            List<FeatureType> featureTypeList = getFeatureTypeListForType(type)
            cannedCommentService.getCannedComments(organism, featureTypeList)
        }
    }

    def getCannedKeys() {
        withPermission(PermissionEnum.READ) { inputObject ->
            Organism organism = Organism.findById(inputObject.getLong(FeatureStringEnum.ORGANISM_ID.value))
            String type = inputObject.getString(FeatureStringEnum.TYPE.value)
            List<FeatureType> featureTypeList = getFeatureTypeListForType(type)
            cannedAttributeService.getCannedKeys(organism, featureTypeList)
        }
    }

    def getCannedValues() {
        withPermission(PermissionEnum.READ) { inputObject ->
            Organism organism = Organism.findById(inputObject.getLong(FeatureStringEnum.ORGANISM_ID.value))
            String type = inputObject.getString(FeatureStringEnum.TYPE.value)
            List<FeatureType> featureTypeList = getFeatureTypeListForType(type)
            cannedAttributeService.getCannedValues(organism, featureTypeList)
        }
    }

    def getAvailableStatuses() {
        withPermission(PermissionEnum.READ) { inputObject ->
            Organism organism = Organism.findById(inputObject.getLong(FeatureStringEnum.ORGANISM_ID.value))
            String type = inputObject.containsKey(FeatureStringEnum.TYPE.value) ? inputObject.getString(FeatureStringEnum.TYPE.value) : null
            List<FeatureType> featureTypeList = type ? getFeatureTypeListForType(type) : []
            availableStatusService.getAvailableStatuses(organism, featureTypeList)
        }
    }

    def searchSequence() {
        log.debug "sequenceSearch data ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        try{
            permissionService.hasPermissions(inputObject, PermissionEnum.READ)
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            log.debug "Organism to string:  ${organism as JSON}"
            render sequenceSearchService.searchSequence(inputObject, organism.getBlatdb())
        }
        catch (Exception ae) {
            def error = [error: ae.message]
            render error as JSON
        }
    }


    def getGff3() {
        log.debug "getGff3 ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(inputObject, PermissionEnum.EXPORT)
            File outputFile = File.createTempFile("feature", ".gff3")
            sequenceService.getGff3ForFeature(inputObject, outputFile)
            Charset encoding = Charset.defaultCharset()
            byte[] encoded = Files.readAllBytes(Paths.get(outputFile.getAbsolutePath()))
            String gff3String = new String(encoded, encoding)
            outputFile.delete() // deleting temp file
            render gff3String

        }
        catch (IOException e) {
            log.debug("Cannot create a temp file for 'get GFF3' operation", e)
        }
        catch (Exception ae) {
            def error = [error: ae.message]
            render error as JSON
        }
    }


    def getRecentAnnotations() {
        withPermission(PermissionEnum.EXPORT) { inputObject ->
            if (inputObject.get('days') instanceof Integer) {
                String filterString = inputObject.containsKey(FeatureStringEnum.STATUS.value) ? inputObject.getString(FeatureStringEnum.STATUS.value) : null
                annotationEditorService.recentAnnotations(inputObject.getInt('days'), filterString)
            } else {
                throw new AnnotationException(inputObject.get('days') + ' Param days must be an Integer')
            }
        }
    }

    def getAttributions() {
        withPermission(PermissionEnum.EXPORT) { inputObject ->
            int max = inputObject.max ?: 1000
            featureEventService.generateAttributions(max)
        }
    }




    @MessageMapping("/AnnotationNotification")
    @SendTo("/topic/AnnotationNotification")
    protected String annotationEditor(String inputString, Principal principal) {
        log.debug("Web socket connected: ${inputString}")
        inputString = annotationEditorService.cleanJSONString(inputString)
        JSONObject rootElement = (JSONObject) JSON.parse(inputString)
        rootElement.put(FeatureStringEnum.USERNAME.value, principal.name)

        String operation = ((JSONObject) rootElement).get(REST_OPERATION)
        log.debug "prinicial name ${principal?.name}"

        String operationName = underscoreToCamelCase(operation)
        log.debug "operationName: ${operationName}"
        def p = task {
            switch (operationName) {
                case "currentUser":
                    User user = permissionService.getCurrentUser(rootElement)
                    return user as JSON
            // test case
                case "ping":
                    return "pong"
            // test case
                case "broadcast":
                    broadcastMessage("pong",principal?.name)
                    break
            // test case
                case "logout":
                    try {
                        ApolloSecurityUtils.logout()
                    } catch (Exception e) {
                        log.warn "No thread, so sending through websocket instead ${e}"
                    }
                    finally {
                        if(principal?.name){
                            JSONObject jsonObject = new JSONObject()
                            jsonObject.put(FeatureStringEnum.USERNAME.value,principal.name)
                            jsonObject.put(REST_OPERATION,"logout")
                            brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/user/" + principal.name, jsonObject.toString()
                        }
                    }
                    break
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
                                try {
                                    returnString = method.invoke(requestHandlingService, rootElement)
                                } catch (Exception e) {
                                    log.error("CAUGHT ERROR through websocket call: " + e)
                                    if (e instanceof InvocationTargetException || !e.message) {
                                        log.error("THROWING PARENT ERROR instead through reflection: " + e.getCause())
                                        return sendError(e.getCause(), principal?.name)
                                    } else {
                                        return sendError(e, principal?.name)
                                    }
                                }
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
        } catch (Exception ae) {
            // TODO: should be returning nothing, but then broadcasting specifically to this user
            log.error("Error for user ${principal?.name} when exexecting ${inputString}" + ae?.message)
            return sendError(ae, principal.name)
        }

    }

    /**
     * Note: this is a test websocket method
     * @param message
     * @param username
     * @return
     */
    protected def broadcastMessage(String message,String username){
        log.debug "bradcasting message: ${message}"
        brokerMessagingTemplate.convertAndSend("/topic/AnnotationNotification", message)
        log.debug "broadcast message: ${message}"
        if(username){
            log.debug "send error to user"
            sendError(new RuntimeException("whoops"),username)
            log.debug "sent error to user"
        }
        log.debug "sending annotation vent"
        sendAnnotationEvent("annotation event of some kind")
        log.debug "sent annotation event"
    }

// TODO: handle errors without broadcasting
    protected def sendError(Throwable exception, String username) {
        log.error "exception ${exception}"
        log.error "exception message ${exception.message}"
        log.error "username ${username}"

        JSONObject errorObject = new JSONObject()
        errorObject.put(REST_OPERATION, FeatureStringEnum.ERROR.name())
        errorObject.put(FeatureStringEnum.ERROR_MESSAGE.value, exception.message)
        errorObject.put(FeatureStringEnum.USERNAME.value, username)

        def destination = "/topic/AnnotationNotification/user/" + username
        log.error "error destination message: ${destination}"
        brokerMessagingTemplate.convertAndSend(destination, exception.message ?: exception.toString())

        return errorObject.toString()
    }


    @SendTo("/topic/AnnotationNotification")
    protected String sendAnnotationEvent(String returnString) {
        log.debug "sendAnnotationEvent ${returnString?.size()}"
        return returnString
    }

    synchronized void handleChangeEvent(AnnotationEvent... events) {
        log.debug "handleChangeEvent ${events.length}"
        if (events.length == 0) {
            return;
        }
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


    def web_services() {
        render view: "/web_services"
    }

}
