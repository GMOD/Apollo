package org.bbop.apollo

import grails.converters.JSON
import groovy.json.JsonBuilder
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.TranslationTable
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Principal

import static grails.async.Promises.task

/**
 * From the WA1 AnnotationEditorService class.
 *
 * This code primarily provides integration with genomic editing functionality visible in the JBrowse window.
 */
@RestApi(name = "Annotation Services", description = "Methods for running the annotation engine")
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
    def featureEventService
    def annotationEditorService
    def organismService
    def brokerMessagingTemplate


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
    @Timed
    def getUserPermission() {
        log.debug "getUserPermission ${params.data}"
        JSONObject returnObject = permissionService.handleInput(request, params)

        String username = SecurityUtils.subject.principal
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
            render returnObject
        } else {
            def errorMessage = [message: "You must first login before editing"]
            response.status = 401
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

    @Timed
    def getHistoryForFeatures() {
        log.debug "getHistoryForFeatures ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        inputObject.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        permissionService.checkPermissions(inputObject, PermissionEnum.READ)

        JSONObject historyContainer = createJSONFeatureContainer();
        historyContainer = featureEventService.generateHistory(historyContainer, featuresArray)

        render historyContainer as JSON
    }


    @RestApiMethod(description = "Returns a translation table as JSON", path = "/annotationEditor/getTranslationTable", verb = RestApiVerb.POST)
    @RestApiParams(params = [])
    def getTranslationTable() {
        log.debug "getTranslationTable"
        JSONObject returnObject = permissionService.handleInput(request, params)
        log.debug "return object ${returnObject as JSON}"
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser(returnObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        log.debug "has organism ${organism}"
        // use the over-wridden one
        TranslationTable translationTable = organismService.getTranslationTable(organism)

        JSONObject ttable = new JSONObject()
        for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
            ttable.put(t.getKey(), t.getValue())
        }

        JSONArray startProteins = new JSONArray()
        JSONArray stopProteins = new JSONArray()

        for(String startCodon in translationTable.getStartCodons()){
            startProteins.add(translationTable.getTranslationTable().get(startCodon))
        }
        for(String stopCodon in translationTable.getStopCodons()){
            stopProteins.add(translationTable.getTranslationTable().get(stopCodon))
        }

        returnObject.put(REST_TRANSLATION_TABLE, ttable)
        returnObject.put(REST_START_PROTEINS, startProteins.unique())
        returnObject.put(REST_STOP_PROTEINS, stopProteins.unique())
        render returnObject
    }


    @RestApiMethod(description = "Add non-coding genomic feature", path = "/annotationEditor/addFeature", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "suppressHistory", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress the history of this operation")
            , @RestApiParam(name = "suppressEvents", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress instant update of the user interface")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ])
    def addFeature() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set exon feature boundaries", path = "/annotationEditor/setExonBoundaries", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "suppressHistory", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress the history of this operation")
            , @RestApiParam(name = "suppressEvents", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress instant update of the user interface")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def setExonBoundaries() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setExonBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description = "Add an exon", path = "/annotationEditor/addExon", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "suppressHistory", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress the history of this operation")
            , @RestApiParam(name = "suppressEvents", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress instant update of the user interface")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def addExon() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description = "Add comments", path = "/annotationEditor/addComments", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def addComments() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Delete comments", path = "/annotationEditor/deleteComments", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def deleteComments() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description = "Update comments", path = "/annotationEditor/updateComments", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'old_comments','new_comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def updateComments() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.updateComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description = "Get comments", path = "/annotationEditor/getComments", verb = RestApiVerb.POST
    )
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects ('uniquename' required) JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def getComments() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render requestHandlingService.getComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Add transcript", path = "/annotationEditor/addTranscript", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "suppressHistory", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress the history of this operation")
            , @RestApiParam(name = "suppressEvents", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress instant update of the user interface")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ])
    def addTranscript() {
        try {
            log.debug "addTranscript ${params}"
            JSONObject inputObject = permissionService.handleInput(request, params)
            if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
                render requestHandlingService.addTranscript(inputObject)
            } else {
                render status: HttpStatus.UNAUTHORIZED
            }
        }
        catch (Exception e) {
            def error = [error: e.message]
            render error as JSON
        }
    }

    @RestApiMethod(description = "Duplicate transcript", path = "/annotationEditor/duplicateTranscript", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "suppressHistory", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress the history of this operation")
            , @RestApiParam(name = "suppressEvents", type = "boolean", paramType = RestApiParamType.QUERY, description = "Suppress instant update of the user interface")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing a single JSONObject feature that contains 'uniquename'")
    ])
    def duplicateTranscript() {
        log.debug "duplicateTranscript ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.duplicateTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set translation start", path = "/annotationEditor/setTranslationStart", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmin':12}}")
    ])
    def setTranslationStart() {
        log.debug "setTranslationStart ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationStart(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set translation end", path = "/annotationEditor/setTranslationEnd", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmax':12}}")
    ])
    def setTranslationEnd() {
        log.debug "setTranslationEnd ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationEnd(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set longest ORF", path = "/annotationEditor/setLongestOrf", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234'}")
    ])
    def setLongestOrf() {
        log.debug "setLongestORF ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setLongestOrf(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set boundaries of genomic feature", path = "/annotationEditor/setBoundaries", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing feature objects with the location object defined {'uniquename':'ABCD-1234','location':{'fmin':2,'fmax':12}}")
    ])
    def setBoundaries() {
        log.debug "setBoundaries ${params}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Get all annotated features for a sequence", path = "/annotationEditor/getFeatures", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
    ])
    def getFeatures() {
        JSONObject returnObject = permissionService.handleInput(request, params)
        try {
            permissionService.checkPermissions(returnObject, PermissionEnum.READ)
            render requestHandlingService.getFeatures(returnObject)
        } catch (e) {
            def error = [error: 'problem getting features: ' + e.fillInStackTrace()]
            render error as JSON
            log.error(error.error)
        }
    }

    @Timed
    def getInformation() {
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONObject inputObject = permissionService.handleInput(request, params)
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
            info.put("owner", gbolFeature.owner ? gbolFeature.owner.username : "N/A");
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

        render featureContainer
    }


    @RestApiMethod(description = "Get sequence alterations for a given sequence", path = "/annotationEditor/getSequenceAlterations", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
    ])
    @Timed
    def getSequenceAlterations() {
        JSONObject returnObject = permissionService.handleInput(request, params)
        Sequence sequence = permissionService.checkPermissions(returnObject, PermissionEnum.READ)
        JSONArray jsonFeatures = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, jsonFeatures)

        List<SequenceAlterationArtifact> sequenceAlterationList = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes"
                , [sequence: sequence, sequenceTypes: requestHandlingService.viewableAlterations])
        for (SequenceAlterationArtifact alteration : sequenceAlterationList) {
            jsonFeatures.put(featureService.convertFeatureToJSON(alteration, true));
        }

        render returnObject
    }

    /**
     * @deprecated This will likely be removed
     * @return
     */
//    def getOrganism() {
//        JSONObject inputObject = permissionService.handleInput(request, params)
//        Organism organism = preferenceService.getCurrentOrganismForCurrentUser(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
//        if (organism) {
//            render organism as JSON
//        } else {
//            render new JSONObject()
//        }
//    }

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
        render annotationInfoEditorConfigContainer
    }

    @RestApiMethod(description = "Set name of a feature", path = "/annotationEditor/setName", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','name':'gene01'}")
    ])
    def setName() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setName(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set description for a feature", path = "/annotationEditor/setDescription", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','description':'some descriptive test'}")
    ])
    def setDescription() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setDescription(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set symbol of a feature", path = "/annotationEditor/setSymbol", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','symbol':'Pax6a'}")
    ])
    def setSymbol() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setSymbol(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set status of a feature", path = "/annotationEditor/setStatus", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','status':'existing-status-string'}.  Available status found here: /availableStatus/ ")
    ])
    def setStatus() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setStatus(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Add attribute (key,value pair) to feature", path = "/annotationEditor/addAttribute", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','non_reserved_properties':[{'tag':'clockwork','value':'orange'},{'tag':'color','value':'purple'}]}.  Available status found here: /availableStatus/ ")
    ])
    def addAttribute() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addNonReservedProperties(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Delete attribute (key,value pair) for feature", path = "/annotationEditor/deleteAttribute", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','non_reserved_properties':[{'tag':'clockwork','value':'orange'},{'tag':'color','value':'purple'}]}.  Available status found here: /availableStatus/ ")
    ])
    def deleteAttribute() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteNonReservedProperties(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Update attribute (key,value pair) for feature", path = "/annotationEditor/updateAttribute", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','old_non_reserved_properties':[{'color': 'red'}], 'new_non_reserved_properties': [{'color': 'green'}]}.")
    ])
    def updateAttribute() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.updateNonReservedProperties(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Add dbxref (db,id pair) to feature", path = "/annotationEditor/addDbxref", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','dbxrefs': [{'db': 'PMID', 'accession': '19448641'}]}.")
    ])
    def addDbxref() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addNonPrimaryDbxrefs(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Update dbxrefs (db,id pairs) for a feature", path = "/annotationEditor/updateDbxref", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','old_dbxrefs': [{'db': 'PMID', 'accession': '19448641'}], 'new_dbxrefs': [{'db': 'PMID', 'accession': '19448642'}]}.")
    ])
    def updateDbxref() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.updateNonPrimaryDbxrefs(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Delete dbxrefs (db,id pairs) for a feature", path = "/annotationEditor/deleteDbxref", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','dbxrefs': [{'db': 'PMID', 'accession': '19448641'}]}.")
    ])
    def deleteDbxref() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteNonPrimaryDbxrefs(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Set readthrough stop codon", path = "/annotationEditor/setReadthroughStopCodon", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with one feature object {'uniquename':'ABCD-1234'}")
    ])
    def setReadthroughStopCodon() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setReadthroughStopCodon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Add sequence alteration", path = "/annotationEditor/addSequenceAlteration", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with Sequence Alteration (Insertion, Deletion, Substituion) objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/")
    ])
    def addSequenceAlteration() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Delete sequence alteration", path = "/annotationEditor/deleteSequenceAlteration", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with Sequence Alteration identified by unique names {'uniquename':'ABC123'}")
    ])
    def deleteSequenceAlteration() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Flip strand", path = "/annotationEditor/flipStrand", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with with objects of features defined as {'uniquename':'ABC123'}")
    ])
    def flipStrand() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.flipStrand(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Merge exons", path = "/annotationEditor/mergeExons", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with with two objects of referred to as defined as {'uniquename':'ABC123'}")
    ])
    def mergeExons() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeExons(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Split exons", path = "/annotationEditor/splitExon", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing feature objects with the location object defined {'uniquename':'ABCD-1234','location':{'fmin':2,'fmax':12}}")
    ])
    def splitExon() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description = "Delete feature", path = "/annotationEditor/deleteFeature", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of features objects to delete defined by unique name {'uniquename':'ABC123'}")
    ])
    def deleteFeature() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Delete exons", path = "/annotationEditor/deleteExon", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of features objects, where the first is the parent transcript and the remaining are exons all defined by a unique name {'uniquename':'ABC123'}")
    ])
    def deleteExon() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Make intron", path = "/annotationEditor/makeIntron", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmin':12}}")
    ])
    def makeIntron() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.makeIntron(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Split transcript", path = "/annotationEditor/splitTranscript", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with with two exon objects referred to their unique names {'uniquename':'ABC123'}")
    ])
    def splitTranscript() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Merge transcripts", path = "/annotationEditor/mergeTranscripts", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray with with two transcript objects referred to their unique names {'uniquename':'ABC123'}")
    ])
    def mergeTranscripts() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeTranscripts(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description = "Get sequences for features", path = "/annotationEditor/getSequences", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Sequence name")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism ID or common name")
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of features objects to export defined by a unique name {'uniquename':'ABC123'}")
    ])
    def getSequence() {
        log.debug "getSequence ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.EXPORT)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        JSONObject featureContainer = createJSONFeatureContainer()
        JSONObject sequenceObject = sequenceService.getSequenceForFeatures(inputObject)
        featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(sequenceObject)
        render featureContainer
    }

    @RestApiMethod(description = "Get sequences search tools", path = "/annotationEditor/getSequenceSearchTools")
    def getSequenceSearchTools() {
        log.debug "getSequenceSearchTools ${params.data}"
        def set = configWrapperService.getSequenceSearchTools()
        def obj = new JsonBuilder(set)
        def jre = ["sequence_search_tools": obj.content]
        render jre as JSON
    }

    @RestApiMethod(description = "Get canned comments", path = "/annotationEditor/getCannedComments", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
    ])
    def getCannedComments() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        render CannedComment.listOrderByComment() as JSON
    }

    @RestApiMethod(description = "Search sequences", path = "/annotationEditor/searchSequences", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "client_token", type = "string", paramType = RestApiParamType.QUERY, description = "Organism ID/Name or Client-generated ")
            , @RestApiParam(name = "search", type = "JSONObject", paramType = RestApiParamType.QUERY, description = "{'key':'blat','residues':'ATACTAGAGATAC':'database_id':'abc123'}")
    ])
    def searchSequence() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        log.debug "Organism to string:  ${organism as JSON}"
        render sequenceSearchService.searchSequence(inputObject, organism.getBlatdb())
    }


    @RestApiMethod(description = "Get gff3", path = "/annotationEditor/getGff3", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "features", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "JSONArray of features objects to export defined by a unique name {'uniquename':'ABC123'}")
    ])
    def getGff3() {
        log.debug "getGff3 ${params.data}"
        JSONObject inputObject = permissionService.handleInput(request, params)
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

    @Timed
    def getAnnotationInfoEditorData() {
        Sequence sequence
        JSONObject inputObject = permissionService.handleInput(request, params)
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
//            JSONObject newFeature = featureService.convertFeatureToJSON(feature, false)
            JSONObject newFeature = new JSONObject()

            if (feature.symbol) {
                newFeature.put(FeatureStringEnum.SYMBOL.value, feature.symbol)
            }
            if (feature.description) {
                newFeature.put(FeatureStringEnum.DESCRIPTION.value, feature.description)
            }

            jsonFeature.put(FeatureStringEnum.ID.value, feature.id);
            if (feature.getName() != null) {
                newFeature.put(FeatureStringEnum.NAME.value, feature.getName());
            }
            newFeature.put(FeatureStringEnum.ID.value, feature.id)
            newFeature.put(FeatureStringEnum.OWNER.value, featureService.generateOwnerString(feature));
            newFeature.put(FeatureStringEnum.DATE_CREATION.value, feature.dateCreated.time);
            newFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, feature.lastUpdated.time);
            newFeature.put(FeatureStringEnum.TYPE.value, featureService.generateJSONFeatureStringForType(feature.ontologyId));

            if (feature instanceof SequenceAlteration) {
                newFeature.put(FeatureStringEnum.LOCATION.value, featureService.convertFeatureLocationToJSON(feature.featureLocation));

                JSONArray alternateAllelesArray = new JSONArray()
                println("Features has altAlleles: ${feature.alleles}")
                for (Allele allele : feature.alleles) {
                    JSONObject alleleObject = new JSONObject()
                    alleleObject.put(FeatureStringEnum.BASES.value, allele.bases)
//                    if (allele.alleleFrequency) {
//                        alternateAlleleObject.put(FeatureStringEnum.ALLELE_FREQUENCY.value, String.valueOf(allele.alleleFrequency))
//                    }
//                    if (allele.provenance) {
//                        alternateAlleleObject.put(FeatureStringEnum.PROVENANCE.value, allele.provenance)
//                    }
                    if (allele.alleleInfo) {
                        JSONArray alleleInfoArray = new JSONArray()
                        allele.alleleInfo.each { alleleInfo ->
                            JSONObject alleleInfoObject = new JSONObject()
                            alleleInfoObject.put(FeatureStringEnum.TAG.value, alleleInfo.tag)
                            alleleInfoObject.put(FeatureStringEnum.VALUE.value, alleleInfo.value)
                            alleleInfoArray.add(alleleInfoObject)
                        }
                        alleleObject.put(FeatureStringEnum.ALLELE_INFO.value, alleleInfoArray)
                    }

                    if (allele.reference) {
                        newFeature.put(FeatureStringEnum.REFERENCE_ALLELE.value, alleleObject)
                    }
                    else {
                        alternateAllelesArray.add(alleleObject)
                    }
                }
                newFeature.put(FeatureStringEnum.ALTERNATE_ALLELES.value, alternateAllelesArray)

                if (feature.variantInfo) {
                    JSONArray variantInfoArray = new JSONArray()
                    for (VariantInfo variantInfo : feature.variantInfo) {
                        JSONObject variantInfoObject = new JSONObject()
                        variantInfoObject.put(FeatureStringEnum.TAG.value, variantInfo.tag)
                        variantInfoObject.put(FeatureStringEnum.VALUE.value, variantInfo.value)
                        variantInfoArray.add(variantInfoObject)
                    }
                    newFeature.put(FeatureStringEnum.VARIANT_INFO.value, variantInfoArray)
                }

            }

            if (feature.featureLocation) {
                newFeature.put(FeatureStringEnum.SEQUENCE.value, feature.featureLocation.sequence.name);
            }


            List<FeatureType> featureTypeList = FeatureType.findAllByOntologyId(feature.ontologyId)

            if (AvailableStatus.count > 0) {
                if(feature.status){
                    newFeature.put(FeatureStringEnum.STATUS.value, feature.status.value)
                }
                JSONArray availableStatuses = new JSONArray()
                newFeature.put(FeatureStringEnum.AVAILABLE_STATUSES.value,availableStatuses)

                List<AvailableStatus> availableStatusList = new ArrayList<>()
                if (featureTypeList) {
                    availableStatusList.addAll(AvailableStatus.executeQuery("select cc from AvailableStatus cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
                }
                availableStatusList.addAll(AvailableStatus.executeQuery("select cc from AvailableStatus cc where cc.featureTypes is empty"))

//                // if there are organism filters for these statuses for this organism, then apply them
                List<AvailableStatusOrganismFilter> availableStatusOrganismFilters = AvailableStatusOrganismFilter.findAllByAvailableStatusInList(availableStatusList)
                if (availableStatusOrganismFilters) {
                    AvailableStatusOrganismFilter.findAllByOrganismAndAvailableStatusInList(sequence.organism, availableStatusList).each {
                        availableStatuses.put(it.availableStatus.value)
                        availableStatusList.remove(it.availableStatus)
                    }
                    availableStatusList.each {
                        availableStatuses.put(it.value)
                    }
                }
//                // otherwise ignore them
                else {
                    availableStatusList.each {
                        availableStatuses.put(it.value)
                    }
                }

            }


            // TODO: add the rest of the attributes
            if (configWrapperService.hasAttributes()) {
                JSONArray properties = new JSONArray();
                newFeature.put(FeatureStringEnum.NON_RESERVED_PROPERTIES.value, properties);
                for (FeatureProperty property : featurePropertyService.getNonReservedProperties(feature)) {
                    JSONObject jsonProperty = new JSONObject();
                    jsonProperty.put(FeatureStringEnum.TAG.value, property.getTag());
                    jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                    properties.put(jsonProperty);
                }



                List<CannedKey> cannedKeyList = new ArrayList<>()
                JSONArray cannedKeys = new JSONArray();
                newFeature.put(FeatureStringEnum.CANNED_KEYS.value, cannedKeys);
                if (featureTypeList) {
                    cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
                }
                cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc where cc.featureTypes is empty"))

//                // if there are organism filters for these canned comments for this organism, then apply them
                List<CannedKeyOrganismFilter> cannedKeyOrganismFilters = CannedKeyOrganismFilter.findAllByCannedKeyInList(cannedKeyList)
                if (cannedKeyOrganismFilters) {
                    CannedKeyOrganismFilter.findAllByOrganismAndCannedKeyInList(sequence.organism, cannedKeyList).each {
                        cannedKeys.put(it.cannedKey.label)
                    }
                }
//                // otherwise ignore them
                else {
                    cannedKeyList.each {
                        cannedKeys.put(it.label)
                    }
                }

                // handle canned Values
                List<CannedValue> cannedValueList = new ArrayList<>()
                JSONArray cannedValues = new JSONArray();
                newFeature.put(FeatureStringEnum.CANNED_VALUES.value, cannedValues);
                if (featureTypeList) {
                    cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
                }
                cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc where cc.featureTypes is empty"))

//                // if there are organism filters for these canned comments for this organism, then apply them
                List<CannedValueOrganismFilter> cannedValueOrganismFilters = CannedValueOrganismFilter.findAllByCannedValueInList(cannedValueList)
                if (cannedValueOrganismFilters) {
                    CannedValueOrganismFilter.findAllByOrganismAndCannedValueInList(sequence.organism, cannedValueList).each {
                        cannedValues.put(it.cannedValue.label)
                    }
                }
//                // otherwise ignore them
                else {
                    cannedValueList.each {
                        cannedValues.put(it.label)
                    }
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

                List<CannedComment> cannedCommentList = new ArrayList<>()
                if (featureTypeList) {
                    cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
                }
                cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc where cc.featureTypes is empty"))

                // if there are organism filters for these canned comments for this organism, then apply them
                List<CannedCommentOrganismFilter> cannedCommentOrganismFilters = CannedCommentOrganismFilter.findAllByCannedCommentInList(cannedCommentList)
                if (cannedCommentOrganismFilters) {
                    CannedCommentOrganismFilter.findAllByOrganismAndCannedCommentInList(sequence.organism, cannedCommentList).each {
                        cannedComments.put(it.cannedComment.comment)
                    }
                }
                // otherwise ignore them
                else {
                    cannedCommentList.each {
                        cannedComments.put(it.comment)
                    }
                }
            }

            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeature);
        }


        render returnObject
    }


    @MessageMapping("/AnnotationNotification")
    @SendTo("/topic/AnnotationNotification")
    @Timed
    protected String annotationEditor(String inputString, Principal principal) {
        inputString = annotationEditorService.cleanJSONString(inputString)
        JSONObject rootElement = (JSONObject) JSON.parse(inputString)
        rootElement.put(FeatureStringEnum.USERNAME.value, principal.name)

        String operation = ((JSONObject) rootElement).get(REST_OPERATION)

        String operationName = underscoreToCamelCase(operation)
        log.debug "operationName: ${operationName}"
        def p = task {
            switch (operationName) {
                case "logout":
                    SecurityUtils.subject.logout()
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
                                } catch (e) {
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
        brokerMessagingTemplate.convertAndSend(destination, exception.message ?: exception.fillInStackTrace().fillInStackTrace())

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
