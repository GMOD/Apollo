package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.report.AnnotatorSummary
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.FetchMode
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus

/**
 * This is server-side code supporting the high-level functionality of the GWT AnnotatorPanel class.
 */
class AnnotatorController {

    def featureService
    def requestHandlingService
    def permissionService
    def annotatorService
    def preferenceService
    def reportService
    def assemblageService
    def featureRelationshipService
    def configWrapperService

    private List<String> reservedList = ["loc",
                                         FeatureStringEnum.CLIENT_TOKEN.value,
                                         FeatureStringEnum.ORGANISM.value,
                                         "action",
                                         "controller",
                                         "format"]

    /**
     * This is a public method, but is really used only internally.
     *
     * Loads the shared link and moves over:
     * http://localhost:8080/apollo/annotator/loadLink?loc=chrII:302089..337445&organism=23357&highlight=0&tracklist=0&tracks=Reference%20sequence,User-created%20Annotations&clientToken=12312321
     * @return
     */
    def loadLink() {
        String clientToken
        try {
            if (params.containsKey(FeatureStringEnum.CLIENT_TOKEN.value)) {
                clientToken = params[FeatureStringEnum.CLIENT_TOKEN.value]
            } else {
                clientToken = ClientTokenGenerator.generateRandomString()
                println 'generating client token on the backend: ' + clientToken
            }
            Organism organism
            // check organism first
            if (params.containsKey(FeatureStringEnum.ORGANISM.value)) {
                String organismString = params[FeatureStringEnum.ORGANISM.value]
                organism = preferenceService.getOrganismForTokenInDB(organismString)
            }
            organism = organism ?: preferenceService.getCurrentOrganismForCurrentUser(clientToken)
            def allowedOrganisms = permissionService.getOrganisms(permissionService.currentUser)
            if (!allowedOrganisms) {
                throw new RuntimeException("User does have permissions to access any organisms.")
            }

            if(params.uuid){
                Feature feature = Feature.findByUniqueName(params.uuid)
                FeatureLocation featureLocation = feature.featureLocation
                params.loc = featureLocation.sequence.name + ":" + featureLocation.fmin +".." + featureLocation.fmax
                organism = featureLocation.sequence.organism
            }

            if (!allowedOrganisms.contains(organism)) {
                log.error("Can not load organism ${organism?.commonName} so loading ${allowedOrganisms.first().commonName} instead.")
                params.loc = null
                organism = allowedOrganisms.first()
            }

            log.debug "loading organism: ${organism}"
            preferenceService.setCurrentOrganism(permissionService.currentUser, organism, clientToken)
            if (params.loc) {
                String location = params.loc
                String[] splitString = location.split(":")
                log.debug "splitString : ${splitString}"
                String sequenceString = splitString[0]
                Sequence sequence = Sequence.findByOrganismAndName(organism, sequenceString)
                String[] minMax = splitString[1].split("\\.\\.")

                log.debug "minMax: ${minMax}"
                int fmin, fmax
                try {
                    fmin = minMax[0] as Integer
                    fmax = minMax[1] as Integer
                } catch (e) {
                    log.error "error parsing ${e}"
                    fmin = sequence.start
                    fmax = sequence.end
                }
                log.debug "fmin ${fmin} . . fmax ${fmax} . . ${sequence}"

                preferenceService.setCurrentSequenceLocation(sequence.name, fmin, fmax, clientToken)
            }

        } catch (e) {
            log.error "problem parsing the string ${e}"
        }

        String queryParamString = ""
        def keyList = []
        // this fixes a bug in addStores being duplicated or processed incorrectly
        for (p in params) {
            // if "addStores is not being processed correclty, this will fix it
            if (p.key.startsWith("addStores=")) {
                if (!p.value) {
                    queryParamString += "&" + p.key
                    keyList << "addStores"
                }
            } else if (!reservedList.contains(p.key) && !keyList.contains(p.key)) {
                queryParamString += "&" + p
                keyList << p.key
            }
        }

        // for some reason the addTracks requires the context path, which seems to be an obscure bug in grails
        if (queryParamString.contains("addTracks")) {
            redirect uri: "${request.contextPath}/annotator/index?clientToken=" + clientToken + queryParamString
        } else {
            redirect uri: "/annotator/index?clientToken=" + clientToken + queryParamString
        }

    }

    /**
     * Loads the main annotator panel.
     */
    @NotTransactional
    def index() {
        log.debug "loading the index"
        String uuid = UUID.randomUUID().toString()
        String clientToken = params.containsKey(FeatureStringEnum.CLIENT_TOKEN.value) ? params.get(FeatureStringEnum.CLIENT_TOKEN.value) : null
        [userKey: uuid, clientToken: clientToken]
    }

    @NotTransactional
    def getExtraTabs(){
        def extraTabs = configWrapperService.extraTabs
        render extraTabs as JSON
    }

    @NotTransactional
    def adminPanel() {
        if (permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)) {
            def administativePanel = grailsApplication.config.apollo.administrativePanel
            [links: administativePanel]
        } else {
            render text: "Unauthorized"
        }
    }

    /**
     * updates shallow properties of gene / feature
     * @return
     */
    @RestApiMethod(description = "Update shallow feature properties", path = "/annotator/updateFeature", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "uniquename", type = "string", paramType = RestApiParamType.QUERY, description = "Uniquename (UUID) of the feature we are editing")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Updated feature name")
            , @RestApiParam(name = "symbol", type = "string", paramType = RestApiParamType.QUERY, description = "Updated feature symbol")
            , @RestApiParam(name = "description", type = "string", paramType = RestApiParamType.QUERY, description = "Updated feature description")
    ]
    )
    @Transactional
    def updateFeature() {
        log.debug "updateFeature ${params.data}"
        JSONObject data = permissionService.handleInput(request, params)
        Assemblage assemblage
        try {
            assemblage = permissionService.checkPermissions(data,PermissionEnum.WRITE)
        } catch (e) {
            log.error("Unauthorized: "+e)
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        Feature feature = Feature.findByUniqueName(data.uniquename)

        feature.name = data.name
        feature.symbol = data.symbol
        feature.description = data.description

        feature.save(flush: true, failOnError: true)

        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        if (feature instanceof Gene) {
            List<Feature> childFeatures = feature.parentFeatureRelationships*.childFeature
            for (childFeature in childFeatures) {
                JSONObject jsonFeature = featureService.convertFeatureToJSON(childFeature, false,assemblage)
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
            }
        } else {
            JSONObject jsonFeature = featureService.convertFeatureToJSON(feature, false,assemblage)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , assemblage: assemblageService.generateAssemblageForFeature(feature)
                , operation: AnnotationEvent.Operation.UPDATE
                , sequenceAlterationEvent: false
        )
        requestHandlingService.fireAnnotationEvent(annotationEvent)

        render updateFeatureContainer
    }


    @RestApiMethod(description = "Update exon boundaries", path = "/annotator/setExonBoundaries", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "uniquename", type = "string", paramType = RestApiParamType.QUERY, description = "Uniquename (UUID) of the exon we are editing")
            , @RestApiParam(name = "fmin", type = "int", paramType = RestApiParamType.QUERY, description = "fmin for Exon Location")
            , @RestApiParam(name = "fmax", type = "int", paramType = RestApiParamType.QUERY, description = "fmax for Exon Location")
            , @RestApiParam(name = "strand", type = "int", paramType = RestApiParamType.QUERY, description = "strand for Feature Location 1 or -1")
    ]
    )
    def setExonBoundaries() {
        JSONObject data = permissionService.handleInput(request, params)
        Assemblage assemblage

        try {
            assemblage = permissionService.checkPermissions(data,PermissionEnum.WRITE)
        } catch (e) {
            log.error("Failed to authorize: "+e)
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        // TODO: this comes from the little interface.
        // Not sure how to handle this there, other than you need to figure out if the feature
        // location corresponds to the feature being used
        Feature feature = Feature.findByUniqueName(data.uniquename)
        feature.featureLocations.first().fmin = data.fmin
        feature.featureLocations.first().fmax = data.fmax
        feature.featureLocations.first().strand = data.strand
        feature.save(flush: true, failOnError: true)

        JSONObject jsonObject = new JSONObject()
        JSONObject featureObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        featureObject.put(FeatureStringEnum.UNIQUENAME.value, data.uniquename)
        JSONObject featureLocationObject = new JSONObject()
        featureLocationObject.put(FeatureStringEnum.FMIN.value, data.fmin)
        featureLocationObject.put(FeatureStringEnum.FMAX.value, data.fmax)
        featureLocationObject.put(FeatureStringEnum.STRAND.value, data.strand)
        featureObject.put(FeatureStringEnum.LOCATION.value, featureLocationObject)

        featuresArray.add(featureObject)

        jsonObject.put("features", featuresArray)
        jsonObject.put(FeatureStringEnum.CLIENT_TOKEN.value, data.clientToken)
        jsonObject.put(FeatureStringEnum.TRACK.value, data.track)
        jsonObject.put("operation", "set_exon_boundaries")
        jsonObject.put(FeatureStringEnum.USERNAME.value, data.username)

        return requestHandlingService.setExonBoundaries(jsonObject)
    }

    private static JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    /**
     * Not really setup for a REST service as is specific to the Annotator Panel interface.
     * If a user has read permissions this method will work.
     * @param sequenceName
     * @param request
     * @param annotationName
     * @param type
     * @param user
     * @param offset
     * @param max
     * @param sortorder
     * @param sort
     * @return
     */
    @Transactional
    def findAnnotationsForSequence(String sequenceName, String request, String annotationName, String type, String user, Integer offset, Integer max, String sortorder, String sort,String clientToken) {
        JSONObject inputObject = permissionService.handleInput(this.request,params)
        try {
            JSONObject returnObject = createJSONFeatureContainer()
            returnObject.clientToken = clientToken
//            if (sequenceName && !Sequence.countByName(sequenceName)) return
            String[] sequenceNames = sequenceName.length() == 0 ? [] : sequenceName.split("\\:\\:")
            List<Sequence> sequences = Sequence.findAllByNameInList(sequenceNames as List<String>)
            if (!sequences) {
                sequences = []
//                render returnObject as JSON
//              return
            } else {
                Assemblage generatedAssemblage = assemblageService.generateAssemblageForSequence(sequences as Sequence[])
                String s = assemblageService.convertAssemblageToJson(generatedAssemblage).toString()

                if (sequenceName) {
                    returnObject.track = s
                }
            }

            Assemblage assemblage
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)

            returnObject.organism = organism?.id
            if (returnObject.has("track")) {
                assemblage = permissionService.checkPermissions(returnObject, PermissionEnum.READ)
            } else {
                assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.READ)
            }
            Integer index = Integer.parseInt(request)

            List<String> viewableTypes

            if (type) {
                viewableTypes = new ArrayList<>()
                switch (type) {
                    case "Gene": viewableTypes.add(Gene.class.canonicalName)
                        break
                    case "Pseudogene": viewableTypes.add(Pseudogene.class.canonicalName)
                        break
                    case "repeat_region": viewableTypes.add(RepeatRegion.class.canonicalName)
                        break
                    case "transposable_element": viewableTypes.add(TransposableElement.class.canonicalName)
                        break
                    default:
                        log.info "Type not found for annotation filter '${type}'"
                        viewableTypes = requestHandlingService.viewableAnnotationList
                        break
                }
            } else {
                viewableTypes = requestHandlingService.viewableAnnotationList
            }

            long start = System.currentTimeMillis()
            log.debug "${sort} ${sortorder}"

//            List<Sequence> sequenceList
//            if (assemblage) {
//                sequenceList = assemblageService.getSequencesFromAssemblage(assemblage)
//            }
//            else{
//
//            }
            //use two step query. step 1 gets genes in a page
            def pagination = Feature.createCriteria().list(max: max, offset: offset) {
                featureLocations {
                    if(sequences && !AssemblageService.isProjectionString(sequenceName)) {
                        'in'('sequence',sequences)
                    }
                    if (sort == "length") {
                        order('length', sortorder)
                    }
                    sequence {
                        if (sort == "sequence") {
                            order('name', sortorder)
                        }
                        eq('organism', organism)
                    }
                }
                if (sort == "name") {
                    order('name', sortorder)
                }
                if (sort == "date") {
                    order('lastUpdated', sortorder)
                }
                if (annotationName) {
                    ilike('name', '%' + annotationName + '%')
                }
                'in'('class', viewableTypes)
            }

            //step 2 does a distinct query with extra attributes added in
            def features = pagination.size() == 0 ? [] : Feature.createCriteria().listDistinct {
                'in'('id', pagination.collect { it.id })
                featureLocations {
                    if (sort == "length") {
                        order('length', sortorder)
                    }
                    sequence {
                        if (sort == "sequence") {
                            order('name', sortorder)
                        }
                    }
                }
                if (sort == "name") {
                    order('name', sortorder)
                }
                if (sort == "date") {
                    order('lastUpdated', sortorder)
                }
                fetchMode 'owners', FetchMode.JOIN
                fetchMode 'featureLocations', FetchMode.JOIN
                fetchMode 'featureLocations.sequence', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.parentFeature', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.parentFeatureRelationships', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.parentFeatureRelationships.childFeature', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.parentFeatureRelationships.childFeature.featureLocations', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.parentFeatureRelationships.childFeature.featureLocations.sequence', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.childFeatureRelationships', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.featureLocations', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.featureLocations.sequence', FetchMode.JOIN
                fetchMode 'parentFeatureRelationships.childFeature.owners', FetchMode.JOIN
            }
            long durationInMilliseconds = System.currentTimeMillis() - start;
            log.debug "criteria query ${durationInMilliseconds}"

            start = System.currentTimeMillis();
            for (Feature feature in features) {
                JSONObject featureObject = featureService.convertFeatureToJSONLite(feature, false, 0,assemblage)
                returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureObject)
            }
            durationInMilliseconds = System.currentTimeMillis() - start;
            log.debug "convert to json ${durationInMilliseconds}"

            returnObject.put(FeatureStringEnum.REQUEST_INDEX.getValue(), index + 1)
            returnObject.put(FeatureStringEnum.ANNOTATION_COUNT.value, pagination.totalCount)

            render returnObject
        }
        catch (PermissionException e) {
            def error = [error: e.message]
            log.warn "Permission exception: " + e.message
            render error as JSON
        }
        catch (Exception e) {
            def error = [error: e.message]
            log.error e.message
            e.printStackTrace()
            render error as JSON
        }

    }

    /**
     * This is a public passthrough to version
     */
    def version() {}

    /**
     * This is a very specific method for the GWT interface.
     * An additional method should be added.
     *
     * AnnotatorService.getAppState() throws an exception and returns an empty JSON string
     * if the user has insufficient permissions.
     */
    @Transactional
    def getAppState() {
        render annotatorService.getAppState(params.get(FeatureStringEnum.CLIENT_TOKEN.value).toString()) as JSON
    }

    /**
     */
    @Transactional
    def setCurrentOrganism(Organism organismInstance) {
        // set the current organism
        preferenceService.setCurrentOrganism(permissionService.currentUser, organismInstance, params[FeatureStringEnum.CLIENT_TOKEN.value] as String)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organismInstance.directory)

        if (!permissionService.checkPermissions(PermissionEnum.READ)) {
            flash.message = permissionService.getInsufficientPermissionMessage(PermissionEnum.READ)
            redirect(uri: "/auth/login")
            return
        }

        render annotatorService.getAppState(params[FeatureStringEnum.CLIENT_TOKEN.value] as String) as JSON
    }

    /**
     */
    @Transactional
    def setCurrentSequence(Sequence sequenceInstance) {
        if (!permissionService.checkPermissions(PermissionEnum.READ)) {
            flash.message = permissionService.getInsufficientPermissionMessage(PermissionEnum.READ)
            redirect(uri: "/auth/login")
            return
        }
        // set the current organism and sequence Id (if both)
        preferenceService.setCurrentSequence(permissionService.currentUser, sequenceInstance, params[FeatureStringEnum.CLIENT_TOKEN.value] as String)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, sequenceInstance.organism.directory)

        render annotatorService.getAppState(params[FeatureStringEnum.CLIENT_TOKEN.value] as String) as JSON
    }

    def notAuthorized() {
        log.error "not authorized"
    }

    /**
     * Permissions handled upstream
     * @param max
     * @return
     */
    def report(Integer max) {
        List<AnnotatorSummary> annotatorSummaryList = new ArrayList<>()
        params.max = Math.min(max ?: 20, 100)

        List<User> annotators = User.list(params)

        annotators.each {
            annotatorSummaryList.add(reportService.generateAnnotatorSummary(it, true))
        }

        render view: "report", model: [annotatorInstanceList: annotatorSummaryList, annotatorInstanceCount: User.count]
    }

    def detail(User user) {
        if (!permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)) {
            flash.message = permissionService.getInsufficientPermissionMessage(PermissionEnum.ADMINISTRATE)
            redirect(uri: "/auth/login")
            return
        }
        render view: "detail", model: [annotatorInstance: reportService.generateAnnotatorSummary(user)]
    }

    def ping() {
        if (permissionService.checkPermissions(PermissionEnum.READ)) {
            log.debug("permissions checked and alive")
            render new JSONObject() as JSON
        } else {
            log.error("User does not have permissions for the site")
            redirect(uri: "/auth/login")
        }
    }
}
