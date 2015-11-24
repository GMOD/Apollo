package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable
import org.grails.plugins.metrics.groovy.Timed
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Principal
import java.text.DateFormat
import static grails.async.Promises.*
import grails.converters.JSON
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.json.JsonBuilder
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

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
    def bookmarkService


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
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        String username = SecurityUtils.subject.principal
        int permission = PermissionEnum.NONE.value
        if (username) {

            User user = User.findByUsername(username)
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
            returnObject.put(REST_PERMISSION, permission)
            returnObject.put(REST_USERNAME, username)
            render returnObject
        }
        else {
            def errorMessage = [message:"You must first login before editing"]
            response.status=401
            render errorMessage as JSON
        }
    }


    //TODO: parse permissions
    def getDataAdapters() {
        log.debug "getDataAdapters"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        def set=configWrapperService.getDataAdapterTools()

        def obj=new JsonBuilder( set )
        def jre=["data_adapters": obj.content]
        render jre as JSON
    }

    @Timed
    def getHistoryForFeatures() {
        log.debug "getHistoryForFeatures ${params}"
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
            List<List<FeatureEvent>> transactionList = featureEventService.getHistory(feature.uniqueName)
            for (int j = 0; j < transactionList.size(); ++j) {
                FeatureEvent transaction = transactionList[j][0];
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
//                        if (transaction.getOperation().equals(FeatureOperation.SPLIT_TRANSCRIPT)) {
//                            Feature newFeature = Feature.findByUniqueName(featureJsonObject.getString(FeatureStringEnum.UNIQUENAME.value))
//                            if (overlapperService.overlaps(feature.featureLocation, newFeature.featureLocation, true)) {
//                                historyFeatures.put(featureJsonObject);
//                            }
//                        }
//                        else{
//                            historyFeatures.put(featureJsonObject);
//                        }
                        historyFeatures.put(featureJsonObject);
                    }
                    history.put(historyItem);
                }
            }
            historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature);
        }

        render historyContainer as JSON
    }


    def getTranslationTable() {
        log.debug "getTranslationTable"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        TranslationTable translationTable = SequenceTranslationHandler.getDefaultTranslationTable()
        JSONObject ttable = new JSONObject();
        for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
            ttable.put(t.getKey(), t.getValue());
        }
        returnObject.put(REST_TRANSLATION_TABLE, ttable);
        render returnObject
    }


    @RestApiMethod(description="Add non-coding genomic feature" ,path="/annotationEditor/addFeature",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="suppressHistory", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress the history of this operation")
            ,@RestApiParam(name="suppressEvents", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress instant update of the user interface")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ] )
    def addFeature() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set exon feature boundaries" ,path="/annotationEditor/setExonBoundaries",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="suppressHistory", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress the history of this operation")
            ,@RestApiParam(name="suppressEvents", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress instant update of the user interface")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def setExonBoundaries() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setExonBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description="Add an exon" ,path="/annotationEditor/addExon",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="suppressHistory", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress the history of this operation")
            ,@RestApiParam(name="suppressEvents", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress instant update of the user interface")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def addExon() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description="Add comments" ,path="/annotationEditor/addComments",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def addComments() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        println inputObject.toString()
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Delete comments" ,path="/annotationEditor/deleteComments",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def deleteComments() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description="Update comments" ,path="/annotationEditor/updateComments",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects ('uniquename' required) that include an added 'old_comments','new_comments' JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def updateComments() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.updateComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description="Get comments" ,path="/annotationEditor/getComments",verb = RestApiVerb.POST
    )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects ('uniquename' required) JSONArray described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ]
    )
    def getComments() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render requestHandlingService.getComments(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Add transcript" ,path="/annotationEditor/addTranscript",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="suppressHistory", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress the history of this operation")
            ,@RestApiParam(name="suppressEvents", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress instant update of the user interface")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of JSON feature objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/Feature.groovy")
    ] )
    def addTranscript() {
        try {
            log.debug "addTranscript ${params}"
            JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
            if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
                render requestHandlingService.addTranscript(inputObject)
            } else {
                render status: HttpStatus.UNAUTHORIZED
            }
        }
        catch(Exception e) {
            def error=[error: e.message]
            render error as JSON
        }
    }

    @RestApiMethod(description="Duplicate transcript" ,path="/annotationEditor/duplicateTranscript",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="suppressHistory", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress the history of this operation")
            ,@RestApiParam(name="suppressEvents", type="boolean", paramType = RestApiParamType.QUERY,description = "Suppress instant update of the user interface")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing a single JSONObject feature that contains 'uniquename'")
    ] )
    def duplicateTranscript() {
        log.debug "duplicateTranscript ${params}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.duplicateTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set translation start" ,path="/annotationEditor/setTranslationStart",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmin':12}}")
    ] )
    def setTranslationStart() {
        log.debug "setTranslationStart ${params}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationStart(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set translation end" ,path="/annotationEditor/setTranslationEnd",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmax':12}}")
    ] )
    def setTranslationEnd() {
        log.debug "setTranslationEnd ${params}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setTranslationEnd(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set longest ORF" ,path="/annotationEditor/setLongestOrf",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234'}")
    ] )
    def setLongestOrf() {
        log.debug "setLongestORF ${params}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setLongestOrf(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set boundaries of genomic feature" ,path="/annotationEditor/setBoundaries",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing feature objects with the location object defined {'uniquename':'ABCD-1234','location':{'fmin':2,'fmax':12}}")
    ] )
    def setBoundaries() {
        log.debug "setBoundaries ${params}"
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setBoundaries(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Get all annotated features for a sequence" ,path="/annotationEditor/getFeatures",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
    ] )
    def getFeatures() {
        JSONObject returnObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
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

            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(info);
        }

        render featureContainer
    }

    // TODO: implement
    def getResiduesWithAlterations() {
        throw new RuntimeException("Not yet implemented")
//        JSONObject featureContainer = createJSONFeatureContainer();
//        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
//        try {
//            permissionService.checkPermissions(inputObject, PermissionEnum.EXPORT)
//            println "updated 2 "
//            JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//            for (int i = 0; i < featuresArray.size(); ++i) {
//                JSONObject jsonFeature = featuresArray.getJSONObject(i);
//                String uniqueName = jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value);
//                Feature feature = Feature.findByUniqueName(uniqueName)
//                String residue = sequenceService.getResiduesFromFeature(feature)
//                JSONObject info = new JSONObject();
//                info.put(FeatureStringEnum.UNIQUENAME.value, uniqueName);
//                info.put("residues", residue)
//                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(info);
//            }
//            render featureContainer
//        } catch (e) {
//            def error= [error: 'problem getting features: '+e.fillInStackTrace()]
//            render error as JSON
//            log.error(error.error)
//        }
    }

    // TODO: implement
    def addFrameshift() {
        throw new RuntimeException("Not yet implemented")
    }

    // TODO: implement
    def getResiduesWithFrameShifts() {
        throw new RuntimeException("Not yet implemented")
    }

    // TODO: implement
    def getResiduesWithAlternationsAndFrameshifts() {
        throw new RuntimeException("Not yet implemented")
    }


    @RestApiMethod(description="Get sequence alterations for a given sequence" ,path="/annotationEditor/getSequenceAlterations",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
    ] )
    @Timed
    def getSequenceAlterations() {
        JSONObject returnObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject

        Bookmark bookmark = permissionService.checkPermissions(returnObject, PermissionEnum.READ)

        JSONArray jsonFeatures = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, jsonFeatures)
        def sequenceTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]

        List<SequenceAlteration> sequenceAlterationList = (List<SequenceAlteration>) Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s in( :sequences) and f.class in :sequenceTypes"
                , [sequences: bookmarkService.getSequencesFromBookmark(bookmark), sequenceTypes: sequenceTypes])
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

    def getAnnotationInfoEditorConfiguration() {
        JSONObject annotationInfoEditorConfigContainer = new JSONObject();
        JSONArray annotationInfoEditorConfigs = new JSONArray();
        annotationInfoEditorConfigContainer.put(FeatureStringEnum.ANNOTATION_INFO_EDITOR_CONFIGS.value, annotationInfoEditorConfigs);
        JSONObject annotationInfoEditorConfig = new JSONObject();
        annotationInfoEditorConfigs.put(annotationInfoEditorConfig);

        if (AvailableStatus.count) {
            JSONArray statusArray = new JSONArray()
            annotationInfoEditorConfig.put(FeatureStringEnum.STATUS.value, statusArray);
            AvailableStatus.all.each { status ->
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


    @RestApiMethod(description="Set description for a feature" ,path="/annotationEditor/setDescription",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','description':'some descriptive test'}")
    ] )
    def setDescription() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setDescription(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set symbol of a feature" ,path="/annotationEditor/setSymbol",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','symbol':'Pax6a'}")
    ] )
    def setSymbol() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setSymbol(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set status of a feature" ,path="/annotationEditor/setStatus",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','status':'existing-status-string'}.  Available status found here: /availableStatus/ ")
    ] )
    def setStatus() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setStatus(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Add attribute (key,value pair) to feature" ,path="/annotationEditor/addAttribute",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing JSON objects with {'uniquename':'ABCD-1234','non_reserved_properties':[{'tag':'clockwark','value':'orange'},{'tag':'color','value':'purple'}]}.  Available status found here: /availableStatus/ ")
    ] )
    def addAttribute() {
        println("PARAMS: " + params.data)
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addNonReservedProperties(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Set readthrough stop codon" ,path="/annotationEditor/setReadthroughStopCodon",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with one feature object {'uniquename':'ABCD-1234'}")
    ] )
    def setReadthroughStopCodon() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.setReadthroughStopCodon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Add sequence alteration" ,path="/annotationEditor/addSequenceAlteration",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with Sequence Alteration (Insertion, Deletion, Substituion) objects described by https://github.com/GMOD/Apollo/blob/master/grails-app/domain/org/bbop/apollo/")
    ] )
    def addSequenceAlteration() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.addSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Delete sequence alteration" ,path="/annotationEditor/deleteSequenceAlteration",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with Sequence Alteration identified by unique names {'uniquename':'ABC123'}")
    ] )
    def deleteSequenceAlteration() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteSequenceAlteration(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Flip strand" ,path="/annotationEditor/flipStrand",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with with objects of features defined as {'uniquename':'ABC123'}")
    ] )
    def flipStrand() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.flipStrand(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Merge exons" ,path="/annotationEditor/mergeExons",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with with two objects of referred to as defined as {'uniquename':'ABC123'}")
    ] )
    def mergeExons() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeExons(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Split exons" ,path="/annotationEditor/splitExon",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing feature objects with the location object defined {'uniquename':'ABCD-1234','location':{'fmin':2,'fmax':12}}")
    ] )
    def splitExon() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }


    @RestApiMethod(description="Delete feature" ,path="/annotationEditor/deleteFeature",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of features objects to delete defined by unique name {'uniquename':'ABC123'}")
    ] )
    def deleteFeature() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteFeature(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Delete exons" ,path="/annotationEditor/deleteExon",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of features objects, where the first is the parent transcript and the remaining are exons all defined by a unique name {'uniquename':'ABC123'}")
    ] )
    def deleteExon() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.deleteExon(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Make intron" ,path="/annotationEditor/makeIntron",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray containing a single JSONObject feature that contains {'uniquename':'ABCD-1234','location':{'fmin':12}}")
    ] )
    def makeIntron() {
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.makeIntron(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Split transcript" ,path="/annotationEditor/splitTranscript",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with with two exon objects referred to their unique names {'uniquename':'ABC123'}")
    ] )
    def splitTranscript() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.splitTranscript(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Merge transcripts" ,path="/annotationEditor/mergeTranscripts",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray with with two transcript objects referred to their unique names {'uniquename':'ABC123'}")
    ] )
    def mergeTranscripts() {
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (permissionService.hasPermissions(inputObject, PermissionEnum.WRITE)) {
            render requestHandlingService.mergeTranscripts(inputObject)
        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
    }

    @RestApiMethod(description="Get sequences for features" ,path="/annotationEditor/getSequences",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="sequence", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Sequence name")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "(optional) Organism ID or common name")
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of features objects to export defined by a unique name {'uniquename':'ABC123'}")
    ] )
    def getSequence() {
        log.debug "getSequence ${params.data}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.EXPORT)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        JSONObject featureContainer = createJSONFeatureContainer()
        JSONObject sequenceObject = sequenceService.getSequenceForFeatures(inputObject)
        featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(sequenceObject)
        render featureContainer
    }

    @RestApiMethod(description="Get sequences search tools" ,path="/annotationEditor/getSequenceSearchTools")
    def getSequenceSearchTools() {
        log.debug "getSequenceSearchTools ${params.data}"
        def set=configWrapperService.getSequenceSearchTools()
        def obj=new JsonBuilder( set )
        def jre=["sequence_search_tools": obj.content]
        render jre as JSON
    }

    @RestApiMethod(description="Get canned comments" ,path="/annotationEditor/getCannedComments",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
    ] )
    def getCannedComments() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        render CannedComment.listOrderByComment() as JSON
    }

    @RestApiMethod(description="Search sequences" ,path="/annotationEditor/searchSequences",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="search", type="JSONObject", paramType = RestApiParamType.QUERY,description = "{'key':'blat','residues':'ATACTAGAGATAC':'database_id':'abc123'}")
    ] )
    def searchSequence() {
        log.debug "sequenceSearch ${params.data}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(inputObject, PermissionEnum.READ)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        log.debug "Organism to string:  ${organism as JSON}"
        render sequenceSearchService.searchSequence(inputObject, organism.getBlatdb())
    }


    @RestApiMethod(description="Get gff3" ,path="/annotationEditor/getGff3",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="features", type="JSONArray", paramType = RestApiParamType.QUERY,description = "JSONArray of features objects to export defined by a unique name {'uniquename':'ABC123'}")
    ] )
    def getGff3() {
        log.debug "getGff3 ${params.data}"
        JSONObject inputObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
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
        JSONObject inputObject = (JSONObject) JSON.parse(params.data)
        try {
            permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
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
            JSONObject newFeature = featureService.convertFeatureToJSON(feature, false)

            if (feature.symbol) newFeature.put(FeatureStringEnum.SYMBOL.value, feature.symbol)
            if (feature.description) newFeature.put(FeatureStringEnum.DESCRIPTION.value, feature.description)

            jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, feature.dateCreated.time);
            jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, feature.lastUpdated.time);

            if (AvailableStatus.count > 0 && feature.status) {
                newFeature.put(FeatureStringEnum.STATUS.value, feature.status.value)
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
                if (featureTypeList) {
                    cannedCommentStrings.addAll(CannedComment.executeQuery("select cc from CannedComment cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]).comment)
                }
                cannedCommentStrings.addAll(CannedComment.executeQuery("select cc from CannedComment cc where cc.featureTypes is empty").comment)
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
    @Timed
    protected String annotationEditor(String inputString, Principal principal) {
        println  "Input String: annotation editor service ${inputString}"
        inputString = fixTrackString(inputString)
        println  "fixed string ${inputString}"
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
        log.debug "exception ${exception}"
        log.debug "exception message ${exception.message}"
        log.debug "username ${username}"

        JSONObject errorObject = new JSONObject()
        errorObject.put(REST_OPERATION, FeatureStringEnum.ERROR.name())
        errorObject.put(FeatureStringEnum.ERROR_MESSAGE.value, exception.message)
        errorObject.put(FeatureStringEnum.USERNAME.value, username)

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
