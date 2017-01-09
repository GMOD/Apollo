package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.bbop.apollo.gwt.shared.projection.Coordinate
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed
import org.hibernate.FetchMode
/**
 * This class is responsible for handling JSON requests from the AnnotationEditorController and routing
 * to the proper service classes.
 *
 * It's goal is to replace a a lot of the layers in AnnotationEditorController
 *
 * Furthermore, this handles requests for websocket, which come in via a different mechanism than the controller
 */
@Transactional
class RequestHandlingService {

    public static String REST_SEQUENCE_ALTERNATION_EVENT = "sequenceAlterationEvent"

    def featureService
    def featureRelationshipService
    def transcriptService
    def cdsService
    def exonService
    def nonCanonicalSplitSiteService
    def configWrapperService
    def nameService
    def permissionService
    def preferenceService
    def featurePropertyService
    def featureEventService
    def brokerMessagingTemplate
    def projectionService
    def assemblageService
    def featureProjectionService
    def sequenceService

    public static final List<String> viewableAnnotationFeatureList = [
            RepeatRegion.class.name,
            TransposableElement.class.name
    ]
    public static final List<String> viewableAnnotationTranscriptParentList = [
            Gene.class.name,
            Pseudogene.class.name
    ]

    public static final List<String> viewableAnnotationTranscriptList = [
            Transcript.class.name,
            MRNA.class.name,
            TRNA.class.name,
            SnRNA.class.name,
            SnoRNA.class.name,
            NcRNA.class.name,
            RRNA.class.name,
            MiRNA.class.name,
    ]

    public static final List<String> viewableAlterations = [
            Deletion.class.name,
            Insertion.class.name,
            Substitution.class.name
    ]

    public static final List<String> viewableAnnotationList = viewableAnnotationFeatureList + viewableAnnotationTranscriptParentList
    public static final List<String> viewableAnnotationTypesList = viewableAnnotationFeatureList + viewableAnnotationTranscriptList + viewableAnnotationTranscriptParentList

    private String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }


    JSONObject setSymbol(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String symbolString = jsonFeature.getString(FeatureStringEnum.SYMBOL.value);

            feature.symbol = symbolString
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature,assemblage)
        }

        User user = permissionService.getCurrentUser(inputObject)
        if(user && assemblage){
            user.addToAssemblages(assemblage)
            user.save(fluhs: true)
        }
        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)

        return new JSONObject()
    }

    JSONObject setDescription(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage =permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String descriptionString = jsonFeature.getString(FeatureStringEnum.DESCRIPTION.value);

            feature.description = descriptionString
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));
        }

        return updateFeatureContainer
    }

    private JSONObject wrapFeature(JSONObject jsonObject, Feature feature, Assemblage assemblage) {

        // only pass in transcript
        if (feature instanceof Gene) {
            feature.parentFeatureRelationships.childFeature.each { childFeature ->
                jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(childFeature,false,assemblage));
            }
        } else {
            jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));
        }

        return jsonObject
    }

    def deleteNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            JSONArray dbXrefJSONArray = jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)

            for (int j = 0; j < dbXrefJSONArray.size(); j++) {
                JSONObject dbXfrefJsonObject = dbXrefJSONArray.getJSONObject(j)
                String dbString = dbXfrefJsonObject.getString(FeatureStringEnum.DB.value)
                String accessionString = dbXfrefJsonObject.getString(FeatureStringEnum.ACCESSION.value)
                DB db = DB.findByName(dbString)
                if (db) {
                    DBXref dbXref = DBXref.findByAccessionAndDb(accessionString, db)
                    if (dbXref) {
                        feature.removeFromFeatureDBXrefs(dbXref)
                        feature.save(failOnError: true)
                    }
                }
            }

            feature.save(flush: true, failOnError: true)
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature,assemblage)
        }

        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.DELETE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer

    }

    /**
     * @return
     */
    def updateNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            log.debug "feature: ${jsonFeature.getJSONArray(FeatureStringEnum.OLD_DBXREFS.value)}"
            JSONObject oldDbXrefJSONObject = jsonFeature.getJSONArray(FeatureStringEnum.OLD_DBXREFS.value).getJSONObject(0)
            JSONObject newDbXrefJSONObject = jsonFeature.getJSONArray(FeatureStringEnum.NEW_DBXREFS.value).getJSONObject(0)

            String dbString = oldDbXrefJSONObject.getString(FeatureStringEnum.DB.value)
            log.debug "dbString: ${dbString}"
            String accessionString = oldDbXrefJSONObject.getString(FeatureStringEnum.ACCESSION.value)
            log.debug "accessionString : ${accessionString}"
            DB db = DB.findByName(dbString)
            if (!db) {
                db = new DB(name: dbString).save()
            }
            DBXref oldDbXref = DBXref.findByAccessionAndDb(accessionString, db)

            if (!oldDbXref) {
                log.error("could not find original dbxref: " + oldDbXrefJSONObject)
            }

            oldDbXref.db = DB.findOrSaveByName(newDbXrefJSONObject.getString(FeatureStringEnum.DB.value))
            oldDbXref.accession = newDbXrefJSONObject.getString(FeatureStringEnum.ACCESSION.value)

            oldDbXref.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature,assemblage)
        }

        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer


    }

    /**
     * For each feature add the list of comments
     * @param inputObject
     */
    def addComments(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            JSONArray commentsArray = jsonFeature.getJSONArray(FeatureStringEnum.COMMENTS.value)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (int commentIndex = 0; commentIndex < commentsArray.size(); commentIndex++) {
                String commentString = commentsArray.getString(commentIndex);
                Comment comment = new Comment(value: commentString, feature: feature).save()
                featurePropertyService.addComment(feature, comment)
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)

        }
        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def deleteComments(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            JSONArray commentsArray = jsonFeature.getJSONArray(FeatureStringEnum.COMMENTS.value)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (int commentIndex = 0; commentIndex < commentsArray.size(); commentIndex++) {
                String commentString = commentsArray.getString(commentIndex);
                featurePropertyService.deleteComment(feature, commentString)
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)

        }
        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)

        return updateFeatureContainer
    }

    def updateComments(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            JSONArray oldComments = jsonFeature.getJSONArray(FeatureStringEnum.OLD_COMMENTS.value);
            JSONArray newComments = jsonFeature.getJSONArray(FeatureStringEnum.NEW_COMMENTS.value);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (int commentIndex = 0; commentIndex < oldComments.size(); commentIndex++) {
                String oldCommentString = oldComments.getString(commentIndex)
                String newCommentString = newComments.getString(commentIndex)

                Comment comment = Comment.findByFeatureAndValue(feature, oldCommentString)
                comment.value = newCommentString
                comment.save()
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)

        }
        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        return updateFeatureContainer
    }

    def setStatus(JSONObject inputObject) {
        log.debug "status being set ${inputObject as JSON}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            String statusString = jsonFeature.getString(FeatureStringEnum.STATUS.value)
            AvailableStatus availableStatus = AvailableStatus.findByValue(statusString)
            Feature feature = Feature.findByUniqueName(uniqueName)
            if (availableStatus) {
                Status status = new Status(
                        value: availableStatus.value
                        , feature: feature
                ).save()
                feature.status = status
                feature.save()
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)

        }
        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        return updateFeatureContainer
    }

    /**
     * being invoked the RHS
     */
    def deleteStatus(JSONObject inputObject) {
        log.debug "deleteStatus ${inputObject as JSON}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            String statusString = jsonFeature.getString(FeatureStringEnum.STATUS.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            feature.status = null
            feature.save()
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)

        }
        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        return updateFeatureContainer
    }


    def getComments(JSONObject inputObject) {
        log.debug "getComments"
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.READ)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            JSONArray commentsArray = new JSONArray()
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (Comment comment in featurePropertyService.getComments(feature)) {
                String commentString = comment.value
                commentsArray.put(commentString)
            }
            jsonFeature.put(FeatureStringEnum.COMMENTS.value, commentsArray)
        }
        return featureContainer

    }

    def addNonPrimaryDbxrefs(JSONObject inputObject) {
        log.debug "addNonPrimaryDbxrefs"
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            log.debug "feature: ${jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)}"
            JSONArray dbXrefJSONArray = jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)

            for (int j = 0; j < dbXrefJSONArray.size(); j++) {
                JSONObject dbXfrefJsonObject = dbXrefJSONArray.getJSONObject(j)
                log.debug "innerArray ${j}: ${dbXfrefJsonObject}"
                String dbString = dbXfrefJsonObject.getString(FeatureStringEnum.DB.value)
                log.debug "dbString: ${dbString}"
                String accessionString = dbXfrefJsonObject.getString(FeatureStringEnum.ACCESSION.value)
                log.debug "accessionString : ${accessionString}"
                featureService.addNonPrimaryDbxrefs(feature, dbString, accessionString)
            }
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)
        }

        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.ADD)

        return updateFeatureContainer


    }

    JSONObject setName(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            feature.name = jsonFeature.get(FeatureStringEnum.NAME.value)

            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature, assemblage)
        }

        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)

        return updateFeatureContainer
    }

    // extract all names and compare
    Boolean sequencesMatchNames(List<String> sequenceNames, List<Sequence> sequences) {
        List<String> collectedNames = sequences.collect {
            it.name
        }.sort()
        return collectedNames as Set == sequenceNames as Set
    }

    @Timed
    @Transactional
    JSONObject getFeatures(JSONObject inputObject) {
        long start = System.currentTimeMillis()

        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.READ)
        List<Sequence> sequenceList = assemblageService.getSequencesFromAssemblage(assemblage)

        JSONArray sequenceListObject = new JSONArray(assemblage.sequenceList)

        Map<String,Sequence> sequenceMap = new HashMap<>()
        Map<Integer,Coordinate> sequenceOrder = new TreeMap<>()

        sequenceList.each {
            sequenceMap.put(it.name,it)
        }

        sequenceListObject.eachWithIndex { JSONObject it , int i ->
            ProjectionSequence projectionSequence = new ProjectionSequence(
                    name: it.name
            )
            Coordinate coordinate = new Coordinate(
                    it.start,
                    it.end,
                    projectionSequence
            )
            sequenceOrder.put(i,coordinate)
        }

        def features = Feature.createCriteria().listDistinct {
            or{
                sequenceOrder.each { sequenceEntry ->
                    Coordinate coordinate = sequenceEntry.value
                    Sequence sequence = sequenceMap.get(coordinate.sequence.name)
                    or {
                        // inbetween the projection
                        featureLocations {
                            'eq'('sequence', sequence)
                            'ge'('fmin', coordinate.min as Integer)
                            'le'('fmax', coordinate.max as Integer)
                        }
                        // overlaps the min edge
                        featureLocations {
                            'eq'('sequence', sequence)
                            'lt'('fmin', coordinate.min as Integer)
                            'gt'('fmax', coordinate.min as Integer)
                        }
                        // overlaps the max edge
                        featureLocations {
                            'eq'('sequence', sequence)
                            'lt'('fmin', coordinate.max as Integer)
                            'gt'('fmax', coordinate.max as Integer)
                        }
                    }
                }
            }
            fetchMode 'owners', FetchMode.JOIN
            fetchMode 'featureLocations', FetchMode.JOIN
            fetchMode 'featureLocations.sequence', FetchMode.JOIN
            fetchMode 'featureProperties', FetchMode.JOIN
            fetchMode 'featureDBXrefs', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships', FetchMode.JOIN
            fetchMode 'childFeatureRelationships', FetchMode.JOIN
            fetchMode 'childFeatureRelationships.parentFeature', FetchMode.JOIN
            fetchMode 'childFeatureRelationships.parentFeature.featureLocations', FetchMode.JOIN
            fetchMode 'childFeatureRelationships.parentFeature.featureLocations.sequence', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.parentFeature', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.parentFeature.featureLocations', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.parentFeature.featureLocations.sequence', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.parentFeatureRelationships', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.childFeatureRelationships', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.featureLocations', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.featureLocations.sequence', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.featureProperties', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.featureDBXrefs', FetchMode.JOIN
            fetchMode 'parentFeatureRelationships.childFeature.owners', FetchMode.JOIN
            'in'('class', viewableAnnotationTranscriptList + viewableAnnotationFeatureList)
            order("name","asc")
        }


        JSONArray jsonFeatures = new JSONArray()
        features.each { Feature feature ->
            JSONObject jsonObject = featureService.convertFeatureToJSON(feature, false,assemblage)
            jsonFeatures.put(jsonObject)
        }

//        featureProjectionService.projectRefSeq(sequence,"",jsonFeatures,false)
        // this will have all of the necessary projection criteria
        jsonFeatures = featureProjectionService.projectTrack(jsonFeatures, assemblage, false)

        inputObject.put(AnnotationEditorController.REST_FEATURES, jsonFeatures)
        log.debug "getFeatures ${System.currentTimeMillis() - start}ms"
        return inputObject

    }

    /**
     * First feature is transcript, and the rest must be exons to add
     * @param inputObject
     * @return
     */
    @Timed
    JSONObject addExon(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        features = featureProjectionService.projectTrack(features, assemblage, true)

        println "adding exon! ${features}"

        String uniqueName = features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value);
        Transcript transcript = Transcript.findByUniqueName(uniqueName)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)


        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i);
            // could be that this is null
            Exon gsolExon = (Exon) featureService.convertJSONToFeature(jsonExon, assemblage)

            featureService.updateNewGsolFeatureAttributes(gsolExon, assemblage);

            if (gsolExon.getFmin() < 0 || gsolExon.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

            transcriptService.addExon(transcript, gsolExon, false,assemblage)

            gsolExon.save()
        }
        featureService.removeExonOverlapsAndAdjacencies(transcript,assemblage)
        transcriptService.updateGeneBoundaries(transcript,assemblage)
        featureService.calculateCDS(transcript,false,assemblage)

        transcript.save(flush: true)
        transcript.attach()
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
        transcript.save(flush: true)

        // TODO: one of these two versions . . .
        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
        JSONObject returnObject = createJSONFeatureContainer(newJsonObject)


        Gene gene = transcriptService.getGene(transcript)

        featureEventService.addNewFeatureEvent(FeatureOperation.ADD_EXON, gene.name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONArray returnArray = featureProjectionService.projectTrack(returnObject.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        returnObject.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, returnObject, AnnotationEvent.Operation.UPDATE)

        return returnObject

    }

    @Timed
    JSONObject addTranscript(JSONObject inputObject) throws Exception {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject returnObject = createJSONFeatureContainer()

        log.info "addTranscript ${inputObject?.size()}"
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        log.debug "sequence: ${assemblage}"
        log.debug "organism: ${assemblage.organism}"
        // this does a reverse projection
        featuresArray = featureProjectionService.projectTrack(featuresArray, assemblage, true)
        log.info "number of features: ${featuresArray?.size()}"
        boolean useCDS = configWrapperService.useCDS()
        boolean suppressHistory = false
        boolean suppressEvents = false

        if (inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value)) {
            suppressHistory = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value)
        }
        if (inputObject.has(FeatureStringEnum.SUPPRESS_EVENTS.value)) {
            suppressEvents = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_EVENTS.value)
        }

        List<Transcript> transcriptList = new ArrayList<>()
        def transcriptJSONList = []
        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonTranscript = featuresArray.getJSONObject(i)
            jsonTranscript = permissionService.copyRequestValues(inputObject, jsonTranscript)
            if (jsonTranscript.has(FeatureStringEnum.USE_CDS.value)) {
                useCDS = jsonTranscript.getBoolean(FeatureStringEnum.USE_CDS.value)
            }
            Transcript transcript = featureService.generateTranscript(jsonTranscript, assemblage, suppressHistory, useCDS)

            // should automatically write to history
            transcript.save(flush: true)
            transcriptList.add(transcript)

            Gene gene = transcriptService.getGene(transcript)
            inputObject.put(FeatureStringEnum.NAME.value, gene.name)

            if (!suppressHistory) {
                def json = featureService.convertFeatureToJSON(transcript,false,assemblage)
                featureEventService.addNewFeatureEventWithUser(FeatureOperation.ADD_TRANSCRIPT, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, json, permissionService.getCurrentUser(inputObject))
                transcriptJSONList += json
            }
        }

        def returnTranscriptList = featureProjectionService.projectTrack(transcriptJSONList as JSONArray,assemblage,false)
        returnObject.put(FeatureStringEnum.FEATURES.value, returnTranscriptList)

        if (!suppressEvents) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnObject
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.ADD
            )

            fireAnnotationEvent(annotationEvent)
        }

        return returnObject
    }

    /**
     * Transcript is the first object
     * @param inputObject
     */
    @Timed
    JSONObject setTranslationStart(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
//        features = featureProjectionService.projectRefSeq(assemblage, features, true)
        features = featureProjectionService.projectTrack(features, assemblage, true)

        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))

        boolean setStart = transcriptJSONObject.has(FeatureStringEnum.LOCATION.value);
        if (!setStart) {
            CDS cds = transcriptService.getCDS(transcript)
            cdsService.setManuallySetTranslationStart(cds, false)
            featureService.calculateCDS(transcript,false,assemblage)
        } else {
            JSONObject jsonCDSLocation = transcriptJSONObject.getJSONObject(FeatureStringEnum.LOCATION.value);
            int genomicPosition = getGenomicPositionFromLocationObject(jsonCDSLocation,false)
            featureService.setTranslationStart(transcript, genomicPosition , true, configWrapperService.getTranslationTable() , false,projectionService.createMultiSequenceProjection(assemblage));
        }

        transcript.save()

        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        Gene gene = transcriptService.getGene(transcript)
        JSONObject newJSONObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
        featureEventService.addNewFeatureEvent(setStart ? FeatureOperation.SET_TRANSLATION_START : FeatureOperation.UNSET_TRANSLATION_START, gene.name, transcript.uniqueName, inputObject, transcriptJSONObject, newJSONObject, permissionService.getCurrentUser(inputObject))
        JSONObject featureContainer = createJSONFeatureContainer(newJSONObject);

//        JSONArray returnArray = featureProjectionService.projectRefSeq(assemblage, featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), false)
        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, featureContainer, AnnotationEvent.Operation.UPDATE)

        return featureContainer
    }

    /**
     * Transcript is the first object
     * @param inputObject
     */
    @Timed
    JSONObject setTranslationEnd(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        features = featureProjectionService.projectTrack(features, assemblage, true)

        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))

        boolean setEnd = transcriptJSONObject.has(FeatureStringEnum.LOCATION.value);
        if (!setEnd) {
            CDS cds = transcriptService.getCDS(transcript)
            cdsService.setManuallySetTranslationEnd(cds, false)
            featureService.calculateCDS(transcript,false,assemblage)
        } else {
            JSONObject jsonCDSLocation = transcriptJSONObject.getJSONObject(FeatureStringEnum.LOCATION.value);
            Integer genomicPosition = getGenomicPositionFromLocationObject(jsonCDSLocation,true)
            MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
            genomicPosition = genomicPosition - multiSequenceProjection.getProjectionSequence(genomicPosition).originalOffset
            featureService.setTranslationEnd(transcript, genomicPosition ,false,configWrapperService.getTranslationTable(),projectionService.createMultiSequenceProjection(assemblage))
        }
        transcript.save()
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }

            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        JSONObject newJSONObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
        featureEventService.addNewFeatureEvent(setEnd ? FeatureOperation.SET_TRANSLATION_END : FeatureOperation.UNSET_TRANSLATION_END, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, transcriptJSONObject, newJSONObject, permissionService.getCurrentUser(inputObject))
        JSONObject featureContainer = createJSONFeatureContainer(newJSONObject);

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, featureContainer, AnnotationEvent.Operation.UPDATE)

        return featureContainer
    }

    @NotTransactional
    Integer getGenomicPositionFromLocationObject(JSONObject jsonObject, boolean preferFmax= true) {
         if(preferFmax){
             return jsonObject.fmax ?: jsonObject.fmin
         }
        else {
             return jsonObject.fmin ?: jsonObject.fmax
         }
    }

    @Timed
    def setReadthroughStopCodon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))

        boolean readThroughStopCodon = transcriptJSONObject.getBoolean(FeatureStringEnum.READTHROUGH_STOP_CODON.value);
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
        featureService.calculateCDS(transcript, readThroughStopCodon,assemblage);

        transcript.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            if (assemblage) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        assemblage: assemblage,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false,assemblage));

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
        featureEventService.addNewFeatureEvent(readThroughStopCodon ? FeatureOperation.SET_READTHROUGH_STOP_CODON : FeatureOperation.UNSET_READTHROUGH_STOP_CODON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONObject returnObject = createJSONFeatureContainer(newJsonObject);

        JSONArray inputArray = new JSONArray(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray returnArray = featureProjectionService.projectTrack(inputArray, assemblage, false)
//        JSONArray returnArray = featureProjectionService.projectRefSeq(assemblage, featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
//        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false)
//        featureEventService.addNewFeatureEvent(readThroughStopCodon ? FeatureOperation.SET_READTHROUGH_STOP_CODON : FeatureOperation.UNSET_READTHROUGH_STOP_CODON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
//
//        JSONObject returnObject = createJSONFeatureContainer(newJsonObject);

        return returnObject
    }

    @Timed
    def setAcceptor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray transcriptArray = new JSONArray()
        featureContainer.put(FeatureStringEnum.FEATURES.value, transcriptArray)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject oldJsonObject = features.getJSONObject(i)
            String uniqueName = oldJsonObject.getString(FeatureStringEnum.UNIQUENAME.value);
            Exon exon = Exon.findByUniqueName(uniqueName)
            Transcript transcript = exonService.getTranscript(exon)

            if (upstreamDonor) {
                exonService.setToUpstreamAcceptor(exon)
            } else {
                exonService.setToDownstreamAcceptor(exon)
            }

            featureService.calculateCDS(transcript,false,assemblage)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
                }
                if (assemblage) {
                    AnnotationEvent annotationEvent = new AnnotationEvent(
                            features: updateFeatureContainer,
                            assemblage: assemblage,
                            operation: AnnotationEvent.Operation.UPDATE
                    )
                    fireAnnotationEvent(annotationEvent)
                }
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)
            transcriptArray.add(newJsonObject)
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
        }


        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }


    @Timed
    def setDonor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray transcriptArray = new JSONArray()
        featureContainer.put(FeatureStringEnum.FEATURES.value, transcriptArray)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject oldJsonObject = features.getJSONObject(i)
            String uniqueName = oldJsonObject.getString(FeatureStringEnum.UNIQUENAME.value);
            Exon exon = Exon.findByUniqueName(uniqueName)
            Transcript transcript = exonService.getTranscript(exon)
            if (upstreamDonor) {
                exonService.setToUpstreamDonor(exon)
            } else {
                exonService.setToDownstreamDonor(exon)
            }

            featureService.calculateCDS(transcript,false,assemblage)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
                }
                fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)
            transcriptArray.add(newJsonObject)
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
        }

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, featureContainer, AnnotationEvent.Operation.UPDATE)

        return featureContainer
    }

    def fireEvent(Assemblage assemblage, JSONObject jsonObject, AnnotationEvent.Operation operation) {
        if (assemblage) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: jsonObject,
                    assemblage: assemblage,
                    operation: operation
            )
            fireAnnotationEvent(annotationEvent)
        }
    }

    @Timed
    JSONObject setLongestOrf(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        featureService.setLongestORF(transcript, false,projectionService.createMultiSequenceProjection(assemblage))

        transcript.save(flush: true, insert: false)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }

            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false,assemblage));

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, featureContainer, AnnotationEvent.Operation.UPDATE)

        return featureContainer
    }

    /**
     *
     * @param inputObject
     * @return
     */
    @Timed
    JSONObject setExonBoundaries(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        // don't think we actually need to project anything right here
//        features = featureProjectionService.projectTrack(features, assemblage, true)

        JSONObject returnObject = createJSONFeatureContainer()

        for (int i = 0; i < features.length(); ++i) {
            JSONObject locationCommand = features.getJSONObject(i);
            if (!locationCommand.has(FeatureStringEnum.LOCATION.value)) {
                continue;
            }
            JSONObject jsonLocation = locationCommand.getJSONObject(FeatureStringEnum.LOCATION.value);
            int fmin = jsonLocation.getInt(FeatureStringEnum.FMIN.value);
            int fmax = jsonLocation.getInt(FeatureStringEnum.FMAX.value);



            if (fmin < 0 || fmax < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

            // next, we have to get the set of sequences and fmin/fmax for this location
            MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)

            Exon exon = Exon.findByUniqueName(locationCommand.getString(FeatureStringEnum.UNIQUENAME.value))

            Transcript transcript = exonService.getTranscript(exon)
            JSONObject oldTranscriptJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)

            Integer exonFmin = projectionService.getMinForFeatureInProjection(exon,multiSequenceProjection)
            Integer exonFmax = projectionService.getMaxForFeatureInProjection(exon,multiSequenceProjection)
            Integer transcriptFmin = projectionService.getMinForFeatureInProjection(transcript,multiSequenceProjection)
            Integer transcriptFmax = projectionService.getMaxForFeatureInProjection(transcript,multiSequenceProjection)

            // TODO: not sure what happens when it crosses over
            if(multiSequenceProjection.getReverseProjectionSequence(exonFmin).reverse){
                int tempExon = exonFmin
                exonFmin = exonFmax
                exonFmax = tempExon
                int tempTranscript = transcriptFmin
                transcriptFmin = transcriptFmax
                transcriptFmax = tempTranscript
            }


            boolean updateTransriptBoundaries = false
            if(exonFmin==transcriptFmin){
                transcriptFmin=fmin
                updateTransriptBoundaries = true
            }
            if(exonFmax==transcriptFmax){
                transcriptFmax=fmax
                updateTransriptBoundaries = true
            }

            featureProjectionService.setFeatureLocationsForProjection(multiSequenceProjection,exon,fmin,fmax)
            if(updateTransriptBoundaries){
                featureProjectionService.setFeatureLocationsForProjection(multiSequenceProjection,transcript,transcriptFmin,transcriptFmax)
            }

            featureService.removeExonOverlapsAndAdjacencies(transcript,assemblage)
            transcriptService.updateGeneBoundaries(transcript,assemblage)

            exon.save()

            featureService.calculateCDS(transcript,false,assemblage)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)

            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
                }

                fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false,assemblage)
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonObject);
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldTranscriptJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
        }

        JSONArray returnArray = featureProjectionService.projectTrack(returnObject.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        returnObject.put(FeatureStringEnum.FEATURES.value, returnArray)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)


        return returnObject
    }

    @Timed
    JSONObject setBoundaries(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        features = featureProjectionService.projectTrack(features, assemblage, true)

        JSONObject returnObject = createJSONFeatureContainerFromFeatures(assemblage)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject oldJsonFeature = features.getJSONObject(i);
            if (!oldJsonFeature.has(FeatureStringEnum.LOCATION.value)) {
                continue;
            }
            JSONObject jsonLocation = oldJsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value);
            int fmin = jsonLocation.getInt(FeatureStringEnum.FMIN.value);
            int fmax = jsonLocation.getInt(FeatureStringEnum.FMAX.value);
            if (fmin < 0 || fmax < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }
            Feature feature = Feature.findByUniqueName(oldJsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
//            editor.setBoundaries(feature, fmin, fmax);
            FeatureLocation featureLocation = FeatureLocation.findByFeature(feature)

            featureLocation.fmin = fmin
            featureLocation.fmax = fmax
            feature.save()

            JSONObject newJsonFeature = featureService.convertFeatureToJSON(feature, false,assemblage)
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonFeature);
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_BOUNDARIES, feature.name, feature.uniqueName, inputObject, oldJsonFeature, newJsonFeature, permissionService.getCurrentUser(inputObject))
        }

        JSONArray returnArray = featureProjectionService.projectTrack(returnObject.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        returnObject.put(FeatureStringEnum.FEATURES.value, returnArray)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject
    }

    def fireAnnotationEvent(AnnotationEvent... annotationEvents) {
        for (AnnotationEvent annotationEvent in annotationEvents) {
            handleChangeEvent(annotationEvent)
        }
    }

    void sendAnnotationEvent(String returnString, Assemblage assemblage) {
        if (returnString.startsWith("[")) {
            returnString = returnString.substring(1, returnString.length() - 1)
        }
        try {
            if(assemblage){
                // TODO: also send to any overlapping sequences as well?
//            brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/" + sequence.organismId + "/" + sequence.id, returnString
                brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/" + assemblage.organismId + "/" + assemblage.id, returnString
//            println "sending: /topic/AnnotationNotification/" + assemblage.organismId + "/" + assemblage.id

                JSONArray sequenceArray = JSON.parse(assemblage.sequenceList) as JSONArray
                for (int i = 0; i < sequenceArray.size(); i++) {
                    String sequenceName = sequenceArray.getJSONObject(i).name
                    brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/" + assemblage.organismId + "/" + sequenceName, returnString
//                println "sending: /topic/AnnotationNotification/" + assemblage.organismId + "/" + sequenceName
                }
            }
            else{
                log.info("Can not send event without a assemblage: ${returnString}")
            }
        } catch (e) {
            log.error("problem sending message: ${e}")
        }
    }

    void handleChangeEvent(AnnotationEvent event) {

        if (!event) {
            return;
        }
        JSONArray operations = new JSONArray();
        JSONObject features = event.getFeatures();
        try {
            features.put(AnnotationEditorController.REST_OPERATION, event.getOperation().name());
            features.put(REST_SEQUENCE_ALTERNATION_EVENT, event.isSequenceAlterationEvent());
            if(event.username){
                features.put(FeatureStringEnum.USERNAME.value, event.username);
            }
            operations.put(features);
        }
        catch (JSONException e) {
            log.error("error handling change event ${event}: ${e}")
        }

        sendAnnotationEvent(operations.toString(), event.assemblage);

    }

    @Timed
    private JSONObject createJSONFeatureContainerFromFeatures(Assemblage assemblage, Feature... features) throws JSONException {
        def jsonObjects = new ArrayList()
        for (Feature feature in features) {
            JSONObject featureObject = featureService.convertFeatureToJSON(feature, false,assemblage)
            jsonObjects.add(featureObject)
        }
        return createJSONFeatureContainer(jsonObjects as JSONObject[])
    }

    @Timed
    JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    @Timed
    JSONObject deleteSequenceAlteration(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONObject deleteFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            SequenceAlteration sequenceAlteration = SequenceAlteration.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            FeatureLocation sequenceAlterationFeatureLocation = sequenceAlteration.firstFeatureLocation
            deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration, true,assemblage));
            FeatureLocation.deleteAll(sequenceAlteration.featureLocations)
            sequenceAlteration.delete(flush: true)

            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlterationFeatureLocation, false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        featureService.setLongestORF(transcript,false,projectionService.createMultiSequenceProjection(assemblage))
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, true,assemblage));
                    }
                    feature.save(flush: true)
                }
            }
        }

        AnnotationEvent deleteAnnotationEvent = new AnnotationEvent(
                features: deleteFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.DELETE
                , sequenceAlterationEvent: true
        )
        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )
        fireAnnotationEvent(deleteAnnotationEvent)
        fireAnnotationEvent(updateAnnotationEvent)

        return createJSONFeatureContainer()
    }

//    { "track": "GroupUn4157", "features": [ { "location": { "fmin": 1284, "fmax": 1284, "strand": 1 }, "type": {"name": "insertion", "cv": { "name":"sequence" } }, "residues": "ATATATA" } ], "operation": "add_sequence_alteration" }
    @Timed
    def addSequenceAlteration(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONObject addFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)


        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        // TODO: add projection here
//        features = featureProjectionService.projectTrack(features , assemblage, true)

        User activeUser = permissionService.getCurrentUser(inputObject)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            SequenceAlteration sequenceAlteration = (SequenceAlteration) featureService.convertJSONToFeature(jsonFeature, assemblage)
            if (activeUser) {
                featureService.setOwner(sequenceAlteration, activeUser)
            } else {
                log.error("Unable to find valid user to set on transcript!" )
            }
            sequenceAlteration.save()

            featureService.updateNewGsolFeatureAttributes(sequenceAlteration, assemblage)

            if (sequenceAlteration.getFmin() < 0 || sequenceAlteration.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

            sequenceAlteration.save(flush: true)

            // TODO: Should we make it compulsory for the request object to have comments
            // TODO: If so, then we should change all of our integration tests for sequence alterations to have comments
            if (jsonFeature.has(FeatureStringEnum.NON_RESERVED_PROPERTIES.value)) {
                JSONArray properties = jsonFeature.getJSONArray(FeatureStringEnum.NON_RESERVED_PROPERTIES.value);
                for (int j = 0; j < properties.length(); ++j) {
                    JSONObject property = properties.getJSONObject(i);
                    String tag = property.getString(FeatureStringEnum.TAG.value)
                    String value = property.getString(FeatureStringEnum.VALUE.value)
                    FeatureProperty featureProperty = new FeatureProperty(
                            feature: sequenceAlteration,
                            value: value,
                            tag: tag
                    ).save()
                    featurePropertyService.addProperty(sequenceAlteration, featureProperty)
                    sequenceAlteration.save(flush: true)
                }
            }


            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlteration.firstFeatureLocation, false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        featureService.setLongestORF(transcript,false,projectionService.createMultiSequenceProjection(assemblage))
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, false,assemblage));
                    }
                }
            }

            // TODO: revert projection
            addFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration, true,assemblage));
        }

        AnnotationEvent addAnnotationEvent = new AnnotationEvent(
                features: addFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.ADD
                , sequenceAlterationEvent: true
        )
        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )
        fireAnnotationEvent(addAnnotationEvent)
        fireAnnotationEvent(updateAnnotationEvent)

        return addFeatureContainer

    }

    def addNonReservedProperties(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            JSONArray properties = jsonFeature.getJSONArray(FeatureStringEnum.NON_RESERVED_PROPERTIES.value);
            for (int j = 0; j < properties.length(); ++j) {
                JSONObject property = properties.getJSONObject(j);
                String tag = property.getString(FeatureStringEnum.TAG.value)
                String value = property.getString(FeatureStringEnum.VALUE.value)
                featureService.addNonReservedProperties(feature, tag, value)
            }
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));
        }

//        AnnotationEvent annotationEvent = new AnnotationEvent(
//                features: updateFeatureContainer
//                , sequence:sequence
//                , operation: AnnotationEvent.Operation.ADD
//                , sequenceAlterationEvent: false
//        )
//
//        fireAnnotationEvent(annotationEvent)

        return updateFeatureContainer
    }

    def deleteNonReservedProperties(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            JSONArray properties = jsonFeature.getJSONArray(FeatureStringEnum.NON_RESERVED_PROPERTIES.value);
            for (int j = 0; j < properties.length(); ++j) {
                JSONObject property = properties.getJSONObject(j);
                String tagString = property.getString(FeatureStringEnum.TAG.value)
                String valueString = property.getString(FeatureStringEnum.VALUE.value)
                log.debug "tagString ${tagString}"
                log.debug "valueString ${valueString}"
                // a NonReservedProperty will always have a tag
                FeatureProperty featureProperty = FeatureProperty.findByTagAndValueAndFeature(tagString, valueString, feature)
                if (featureProperty) {
                    log.info "Removing feature property"
                    feature.removeFromFeatureProperties(featureProperty)
                    feature.save()
                    featureProperty.delete(flush: true)
                } else {
                    log.error "Could not find feature property to delete ${property as JSON}"
                }
            }
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));
        }

        return updateFeatureContainer
    }

    def updateNonReservedProperties(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            JSONArray oldProperties = jsonFeature.getJSONArray(FeatureStringEnum.OLD_NON_RESERVED_PROPERTIES.value);
            JSONArray newProperties = jsonFeature.getJSONArray(FeatureStringEnum.NEW_NON_RESERVED_PROPERTIES.value);
            for (int j = 0; j < oldProperties.length(); ++j) {
                JSONObject oldProperty = oldProperties.getJSONObject(j);
                JSONObject newProperty = newProperties.getJSONObject(j);
                String oldTag = oldProperty.getString(FeatureStringEnum.TAG.value)
                String oldValue = oldProperty.getString(FeatureStringEnum.VALUE.value)
                String newTag = newProperty.getString(FeatureStringEnum.TAG.value)
                String newValue = newProperty.getString(FeatureStringEnum.VALUE.value)

                FeatureProperty featureProperty = FeatureProperty.findByTagAndValueAndFeature(oldTag, oldValue, feature)
                if (feature) {
                    featureProperty.tag = newTag
                    featureProperty.value = newValue
                    featureProperty.save()
                } else {
                    log.error("No feature property found for tag ${oldTag} and value ${oldValue} for feature ${feature}")
                }
            }
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));

        }
//        fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
    }

    def lockFeature(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject featureContainer = createJSONFeatureContainer();
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            if (FeatureProperty.findByFeatureAndValue(feature, FeatureStringEnum.LOCKED.value)) {
                log.error("Feature ${feature.name} already locked")
            } else {
                FeatureProperty featureProperty = new FeatureProperty(
                        value: FeatureStringEnum.LOCKED.value
                        , feature: feature
                ).save()
                feature.addToFeatureProperties(featureProperty)
                feature.save()
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false,assemblage));
            }
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def unlockFeature(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject featureContainer = createJSONFeatureContainer();
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)


        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            FeatureProperty featureProperty = FeatureProperty.findByFeatureAndValue(feature, FeatureStringEnum.LOCKED.value)
            if (featureProperty) {
                feature.removeFromFeatureProperties(featureProperty)
                feature.save()
                FeatureProperty.deleteAll(featureProperty)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage));
            } else {
                log.error("Feature ${feature.name} was not locked.  Doing nothing.")
            }
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def flipStrand(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))

            if (feature instanceof Transcript) {
                feature = transcriptService.flipTranscriptStrand((Transcript) feature,assemblage);
                featureService.setLongestORF((Transcript) feature,false,projectionService.createMultiSequenceProjection(assemblage))
                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites((Transcript) feature,assemblage)
                featureEventService.addNewFeatureEventWithUser(FeatureOperation.FLIP_STRAND, transcriptService.getGene((Transcript) feature).name, feature.uniqueName, inputObject, featureService.convertFeatureToJSON((Transcript) feature,false,assemblage), permissionService.getCurrentUser(inputObject))
                def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(feature)
                if (transcriptsToUpdate.size() > 0) {
                    JSONObject updateFeatureContainer = createJSONFeatureContainer()
                    transcriptsToUpdate.each {
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
                    }

                    fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
                }
            } else {
                feature = featureService.flipStrand(feature)
                featureEventService.addNewFeatureEventWithUser(FeatureOperation.FLIP_STRAND, feature.name, feature.uniqueName, inputObject, featureService.convertFeatureToJSON(feature,false,assemblage), permissionService.getCurrentUser(inputObject))
            }
            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false,assemblage));
        }

        JSONArray jsonArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, jsonArray)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def mergeExons(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Exon exon1 = (Exon) Exon.findByUniqueName(features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value));
        Exon exon2 = (Exon) Exon.findByUniqueName(features.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value));
        Transcript transcript1 = exonService.getTranscript(exon1)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript1,false,assemblage)
        exonService.mergeExons(exon1, exon2)
        featureService.calculateCDS(transcript1,false,assemblage);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1,assemblage);
        // rename?

        transcript1.save(flush: true)
        exon1.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript1)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript1,false,assemblage)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.MERGE_EXONS, transcriptService.getGene(transcript1).name, transcript1.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        fireEvent(assemblage, featureContainer, AnnotationEvent.Operation.UPDATE)

        return featureContainer
    }

    @Timed
    def splitExon(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        features = featureProjectionService.projectTrack(features, assemblage, true)

        JSONObject jsonExon = features.getJSONObject(0)
        Exon exon = (Exon) Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value);
        Transcript transcript = exonService.getTranscript(exon)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)

        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
        Integer genomicMaxPosition = exonLocation.getInt(FeatureStringEnum.FMAX.value)
        Integer genomicMinPosition = exonLocation.getInt(FeatureStringEnum.FMIN.value)
        ProjectionSequence projectionSequence = multiSequenceProjection.getProjectionSequence(genomicMaxPosition)
        genomicMaxPosition = genomicMaxPosition - projectionSequence.originalOffset
        genomicMinPosition = genomicMinPosition - projectionSequence.originalOffset


        Exon splitExon = exonService.splitExon(exon, genomicMaxPosition, genomicMinPosition ,assemblage)
        //featureService.updateNewGsolFeatureAttributes(splitExon, sequence)
        featureService.updateNewGsolFeatureAttributes(splitExon, assemblage)
        featureService.calculateCDS(transcript,false,assemblage)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)

        exon.save()
        transcript.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }

            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject);

        featureEventService.addNewFeatureEvent(FeatureOperation.SPLIT_EXON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    /**
     * First object is Transcript.
     * Subsequence objects are exons
     * @param inputObject
     */
    @Timed
    def deleteExon(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonTranscript = features.getJSONObject(0)

        Transcript transcript = Transcript.findByUniqueName(jsonTranscript.getString(FeatureStringEnum.UNIQUENAME.value));
        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i)
            Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));

            exonService.deleteExon(transcript, exon);
        }
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        Feature topLevelFeature = featureService.getTopLevelFeature(transcript)
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topLevelFeature,false,assemblage))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.DELETE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def addFeature(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        log.debug "adding sequence with found assemblage ${assemblage}"
        User user = permissionService.getCurrentUser(inputObject)

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        featuresArray = featureProjectionService.projectTrack(featuresArray, assemblage, true)
        JSONObject returnObject = createJSONFeatureContainer()

        boolean suppressHistory = false
        boolean suppressEvents = false
        if (inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value)) {
            suppressHistory = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value)
        }
        if (inputObject.has(FeatureStringEnum.SUPPRESS_EVENTS.value)) {
            suppressEvents = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_EVENTS.value)
        }

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            if (jsonFeature.get(FeatureStringEnum.TYPE.value).name == Gene.alternateCvTerm ||
                    jsonFeature.get(FeatureStringEnum.TYPE.value).name == Pseudogene.alternateCvTerm) {
                // if jsonFeature is of type gene or pseudogene
                JSONObject jsonGene = JSON.parse(jsonFeature.toString())
                jsonGene.remove(FeatureStringEnum.CHILDREN.value)
                if(jsonFeature.containsKey(FeatureStringEnum.CHILDREN.value)){
                    for (JSONObject transcriptJsonFeature in jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                        // look at its children JSON Array to get the features at the *RNA level
                        // adding jsonGene to each individual transcript
                        transcriptJsonFeature.put(FeatureStringEnum.PARENT.value, jsonGene)
                        Feature newFeature = featureService.addFeature(transcriptJsonFeature, assemblage, user, suppressHistory)
                        JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature,false,assemblage)
                        JSONObject jsonObject = newFeatureJsonObject

                        if (!suppressHistory) {
                            featureEventService.addNewFeatureEvent(FeatureOperation.ADD_FEATURE, newFeature.name, newFeature.uniqueName, inputObject, newFeatureJsonObject, user)
                        }
                        returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonObject);
                    }
                }
            }
            else {
                // jsonFeature is of type *RNA, transposable_element or repeat_region
                Feature newFeature = featureService.addFeature(jsonFeature, assemblage, user, suppressHistory)
                JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature,false,assemblage)
                log.debug "newFeatureJsonObject: ${newFeatureJsonObject.toString()}"
                JSONObject jsonObject = newFeatureJsonObject
//            Feature newFeature = featureService.addFeature(jsonFeature, assemblage, user, suppressHistory)
////            Feature newFeature = featureService.addFeature(jsonFeature, sequence, user, suppressHistory)
//            JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature)
//            log.debug "newFeatureJsonObject: ${newFeatureJsonObject.toString()}"
//            JSONObject jsonObject = newFeatureJsonObject

                if (!suppressHistory) {
                    featureEventService.addNewFeatureEvent(FeatureOperation.ADD_FEATURE, newFeature.name, newFeature.uniqueName, inputObject, newFeatureJsonObject, user)
                }
                returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonObject);
            }
        }


        JSONArray returnArray = featureProjectionService.projectTrack(returnObject.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        returnObject.put(FeatureStringEnum.FEATURES.value, returnArray)

        if (!suppressEvents) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnObject
                    , assemblage: assemblage
                    , operation: AnnotationEvent.Operation.ADD
            )

            fireAnnotationEvent(annotationEvent)
        }
        return returnObject
    }

    /**
     *  From AnnotationEditorService
     */
//    { "track": "Group1.3", "features": [ { "uniquename": "179e77b9-9329-4633-9f9e-888e3cf9b76a" } ], "operation": "delete_feature" }:
    @Timed
    def deleteFeature(JSONObject inputObject) {
        log.debug "in delete feature ${inputObject as JSON}"
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        boolean suppressEvents = false
        if (inputObject.has(FeatureStringEnum.SUPPRESS_EVENTS.value)) {
            suppressEvents = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_EVENTS.value)
        }
        boolean suppressHistory = false
        if (inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value)) {
            suppressHistory = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value)
        }

        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Map<String, List<Feature>> modifiedFeaturesUniqueNames = new HashMap<String, List<Feature>>();
        boolean isUpdateOperation = false

        JSONArray oldJsonObjectsArray = new JSONArray()
        // we have to hold transcripts if feature is an exon, etc. or a feature itself if not a transcript
        Map<String, JSONObject> oldFeatureMap = new HashMap<>()
        log.debug "features to delete: ${featuresArray.size()}"

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature feature
            String uniqueName
            if (jsonFeature.has(FeatureStringEnum.UNIQUENAME.value)) {
                uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
                feature = Feature.findByUniqueName(uniqueName)
            } else {
                feature = Feature.findByName(jsonFeature.getString(FeatureStringEnum.NAME.value))
                uniqueName = feature.uniqueName
            }
            // TODO: can not do this as it will aggressively delete history
            // that other objects might need
//            if (!suppressHistory) {
//                featureEventService.deleteHistory(uniqueName)
//            }

            log.debug "feature found to delete ${feature?.name}"
            if (feature) {
                if (feature instanceof Exon) {
                    Transcript transcript = exonService.getTranscript((Exon) feature)
                    // if its the same transcript, we don't want to overwrite it
                    if (!oldFeatureMap.containsKey(transcript.uniqueName)) {
                        oldFeatureMap.put(transcript.uniqueName, featureService.convertFeatureToJSON(transcript,false,assemblage))
                    }
                } else {
                    if (!oldFeatureMap.containsKey(feature.uniqueName)) {
                        oldFeatureMap.put(feature.uniqueName, featureService.convertFeatureToJSON(feature,false,assemblage))
                    }
                }
                //oldJsonObjectsArray.add(featureService.convertFeatureToJSON(feature))
                // is this a bug?
                isUpdateOperation = featureService.deleteFeature(feature, modifiedFeaturesUniqueNames) || isUpdateOperation;
                List<Feature> modifiedFeaturesList = modifiedFeaturesUniqueNames.get(uniqueName)
                if (modifiedFeaturesList == null) {
                    modifiedFeaturesList = new ArrayList<>()
                }
                modifiedFeaturesList.add(feature)
                modifiedFeaturesUniqueNames.put(uniqueName, modifiedFeaturesList)
            }

        }
        for (String key : oldFeatureMap.keySet()) {
            log.debug "setting keys"
            oldJsonObjectsArray.add(oldFeatureMap.get(key))
        }

        for (Map.Entry<String, List<Feature>> entry : modifiedFeaturesUniqueNames.entrySet()) {
            String uniqueName = entry.getKey();
            Feature feature = Feature.findByUniqueName(uniqueName);
            log.debug "updating name for feature ${uniqueName} -> ${feature}"
            if (feature == null) {
                log.info("Feature already deleted");
                continue;
            }
            if (!isUpdateOperation) {
                log.debug "is not update operation "
                // when the line below is used, the client gives an error saying TypeError: Cannot read property 'fmin' of undefined(…)
                // featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(new JSONObject().put(FeatureStringEnum.UNIQUENAME.value, uniqueName));
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false,assemblage))
                if (feature instanceof Transcript) {
                    Transcript transcript = (Transcript) feature;
                    Gene gene = transcriptService.getGene(transcript)
                    if (!gene) {
                        gene = transcriptService.getPseudogene(transcript)
                    }
                    int numberTranscripts = transcriptService.getTranscripts(gene).size()
                    if (numberTranscripts == 1) {
                        Feature topLevelFeature = featureService.getTopLevelFeature(gene)
                        featureRelationshipService.deleteFeatureAndChildren(topLevelFeature)

                        if (!suppressEvents) {
                            AnnotationEvent annotationEvent = new AnnotationEvent(
                                    features: featureContainer
                                    , assemblage: assemblage
                                    , operation: AnnotationEvent.Operation.DELETE
                            )

                            fireAnnotationEvent(annotationEvent)
                        }
                    } else {
                        featureRelationshipService.removeFeatureRelationship(gene, transcript)
                        featureRelationshipService.deleteFeatureAndChildren(transcript)
//                        gene.save(flush: true)
                        gene.save()

                        if (!suppressEvents) {
                            AnnotationEvent annotationEvent = new AnnotationEvent(
                                    features: featureContainer
                                    , assemblage: assemblage
                                    , operation: AnnotationEvent.Operation.UPDATE
                            )
                            fireAnnotationEvent(annotationEvent)
                        }
                    }

                    // TODO: handle transcript merging ??
//                    List<String> toBeDeleted = new ArrayList<String>();
//                    toBeDeleted.add(feature.getUniqueName());
//                    while (!toBeDeleted.isEmpty()) {
//                        String id = toBeDeleted.remove(toBeDeleted.size() - 1);
//                        for (Transaction t : historyStore.getTransactionListForFeature(id)) {
//                            if (t.getOperation().equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
//                                if (editor.getSession().getFeatureByUniqueName(t.getOldFeatures().get(1).getUniqueName()) == null) {
//                                    toBeDeleted.add(t.getOldFeatures().get(1).getUniqueName());
//                                }
//                            }
//                        }
//                        historyStore.deleteHistoryForFeature(id);
//                    }

                } else {
                    Feature topLevelFeature = featureService.getTopLevelFeature(feature)
                    featureRelationshipService.deleteFeatureAndChildren(topLevelFeature)

                    if (!suppressEvents) {
                        AnnotationEvent annotationEvent = new AnnotationEvent(
                                features: featureContainer
                                , assemblage: assemblage
                                , operation: AnnotationEvent.Operation.DELETE
                        )

                        fireAnnotationEvent(annotationEvent)
                    }
                }
            } else {
                String featureName
                log.debug "IS update operation "
                FeatureOperation featureOperation
                if (feature instanceof Transcript) {
                    Transcript transcript = (Transcript) feature;
                    featureService.calculateCDS(transcript,false,assemblage)
                    nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)
                    transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)
                    Gene gene = transcriptService.getGene(transcript)
                    gene.save()

                    def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
                    if (transcriptsToUpdate.size() > 0) {
                        JSONObject updateFeatureContainer = createJSONFeatureContainer()
                        transcriptsToUpdate.each {
                            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
                        }
                        fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
                    }

                    featureOperation = FeatureOperation.DELETE_EXON
                    featureName = gene.name
                } else {
                    feature.save()
                    featureOperation = FeatureOperation.DELETE_FEATURE
                    featureName = feature.name
                }

                JSONObject newJsonObject = featureService.convertFeatureToJSON(feature,false,assemblage)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonObject);

                JSONArray updateArray = featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
                updateArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value),assemblage,false)
                featureContainer.put(FeatureStringEnum.FEATURES.value,updateArray)


                if (!suppressEvents) {
                    featureEventService.addNewFeatureEvent(featureOperation, featureName, feature.uniqueName, inputObject, new JSONObject().put(FeatureStringEnum.FEATURES.value, oldJsonObjectsArray), newJsonObject, permissionService.getCurrentUser(inputObject))
                }
            }
        }



        if (!suppressEvents) {
            AnnotationEvent finalAnnotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , assemblage: assemblage
            )

            finalAnnotationEvent.operation = isUpdateOperation ? AnnotationEvent.Operation.UPDATE : AnnotationEvent.Operation.DELETE
            fireAnnotationEvent(finalAnnotationEvent)
        }

        return createJSONFeatureContainer()
    }

    @Timed
    def makeIntron(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        // TODO: project fmin only
        println "making intron ${featuresArray as JSON}"
        featuresArray = featureProjectionService.projectTrack(featuresArray, assemblage, true)
        println "converted -> ${featuresArray}"

        JSONObject jsonExon = featuresArray.getJSONObject(0)
        Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value))
        Transcript transcript = exonService.getTranscript(exon)
        JSONObject oldJsonTranscript = featureService.convertFeatureToJSON(transcript,false,assemblage)
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value)
        // NOTE: exonLocation.get("FMIN") of a null value throws an error if null
        Integer genomicPosition = exonLocation.fmin ?: exonLocation.fmax
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
        ProjectionSequence projectionSequence = multiSequenceProjection.getProjectionSequence(genomicPosition)
        assert (projectionSequence.reverse && exonLocation.fmax) || (!projectionSequence.reverse && exonLocation.fmin)
        genomicPosition = genomicPosition - projectionSequence.originalOffset

        Exon splitExon = exonService.makeIntron(
                exon
                , genomicPosition
                , configWrapperService.getDefaultMinimumIntronSize()
                , assemblage
        )
        if (splitExon == null) {
            def returnContainer = createJSONFeatureContainer()
            returnContainer.put(FeatureStringEnum.ERROR_MESSAGE.value, "Unable to find canonical splice sites.")
            String username = permissionService.getCurrentUser(inputObject)?.username
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.ERROR
                    , username: username
            )

            fireAnnotationEvent(annotationEvent)
            return returnContainer
        }
        featureService.updateNewGsolFeatureAttributes(splitExon, assemblage)
        featureService.calculateCDS(transcript,false,assemblage)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript,assemblage)

        transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)

        transcript.save(failOnError: true)
        exon.save(failOnError: true)
        splitExon.save(failOnError: true, flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript,false,assemblage)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.SPLIT_EXON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonTranscript, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONArray returnArray = featureProjectionService.projectTrack(featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        featureContainer.put(FeatureStringEnum.FEATURES.value, returnArray)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def splitTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)


        Exon exon1 = Exon.findByUniqueName(featuresArray.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))
        Exon exon2 = Exon.findByUniqueName(featuresArray.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript transcript1 = exonService.getTranscript(exon1)
        Transcript transcript2 = transcriptService.splitTranscript(transcript1, exon1, exon2,assemblage)

        featureService.updateNewGsolFeatureAttributes(transcript2, assemblage);

        featureService.calculateCDS(transcript1,false,assemblage)
        featureService.calculateCDS(transcript2,false,assemblage)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1,assemblage);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript2,assemblage);
        transcript1.name = transcript1.name ?: nameService.generateUniqueName(transcript1)
        transcript2.name = transcript2.name ?: nameService.generateUniqueName(transcript2)

        transcript1.owners.each { transcript2.addToOwners(it) }

        Gene gene1 = transcriptService.getGene(transcript1)
        Gene gene2 = transcriptService.getGene(transcript2)

        // relying on featureService::handleDynamicIsoformOverlap() to assign the proper parent
        // to transcript2, based on isoform overlap rule
        ArrayList<Transcript> transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript1)
        transcriptsToUpdate.addAll(featureService.handleDynamicIsoformOverlap(transcript2))

        // updateContainer for update annotation event
        JSONObject updateContainer = createJSONFeatureContainer();
        Gene updatedGene1 = transcriptService.getGene(transcript1)
        Gene updatedGene2 = transcriptService.getGene(transcript2)
        for (Transcript t : transcriptService.getTranscripts(updatedGene1)) {
            updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t,false,assemblage));
        }

        if (updatedGene1.uniqueName != updatedGene2.uniqueName) {
            for (Transcript t : transcriptService.getTranscripts(updatedGene2)) {
                updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t,false,assemblage));
            }
        }

        // returnContainer for return object
        Feature topLevelExonFeature = featureService.getTopLevelFeature(transcript1)
        JSONObject returnContainer = createJSONFeatureContainerFromFeatures(assemblage,topLevelExonFeature)

        // features to add to history
        JSONObject featureForHistory = createJSONFeatureContainer()
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript1,false,assemblage))
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript2,false,assemblage))

        // add history for transcript1 and transcript2
        Boolean suppressHistory = inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value) ? inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value) : false
        if (!suppressHistory) {
            try {
                featureEventService.addSplitFeatureEvent(updatedGene1.name, transcript1.uniqueName
                        , updatedGene2.name, transcript2.uniqueName
                        , inputObject
                        , featureService.convertFeatureToJSON(transcript1,false,assemblage)
                        , featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value)
                        , permissionService.getCurrentUser(inputObject)
                )
            } catch (e) {
                log.error "There was an error adding history ${e}"
            }
        }
//
        JSONArray updateArray = featureProjectionService.projectTrack(updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value), assemblage, false)
        updateContainer.put(FeatureStringEnum.FEATURES.value, updateArray)

        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )
        fireAnnotationEvent(updateAnnotationEvent)

        return returnContainer
    }

    @Timed
    def mergeTranscripts(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonTranscript1 = featuresArray.getJSONObject(0)
        JSONObject jsonTranscript2 = featuresArray.getJSONObject(1)
        Transcript transcript1 = Transcript.findByUniqueName(jsonTranscript1.getString(FeatureStringEnum.UNIQUENAME.value))
        Transcript transcript2 = Transcript.findByUniqueName(jsonTranscript2.getString(FeatureStringEnum.UNIQUENAME.value))

        // cannot merge transcripts from different strands
        if (!transcript1.getStrand().equals(transcript2.getStrand())) {
            throw new AnnotationException("You cannot merge transcripts on opposite strands");
        }

        List<Transcript> sortedTranscripts = [transcript1, transcript2].sort { a, b ->
            a.fmin <=> b.fmin
        }
        if (transcript1.strand == Strand.NEGATIVE.value) {
            sortedTranscripts.reverse(true)
        }
        transcript1 = sortedTranscripts.get(0)
        transcript2 = sortedTranscripts.get(1)
        Gene gene1 = transcriptService.getGene(transcript1)
        Gene gene2 = transcriptService.getGene(transcript2)
        String gene1Name = gene1.name
        String gene2Name = gene2.name
        String transcript1UniqueName = transcript1.uniqueName
        String transcript2UniqueName = transcript2.uniqueName

        JSONObject transcript2JSONObject = featureService.convertFeatureToJSON(transcript2,false,assemblage)
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)

        // calculate longest ORF, to reset any changes made to the CDS, before a merge
        featureService.setLongestORF(transcript1,false,multiSequenceProjection);
        featureService.setLongestORF(transcript2,false,multiSequenceProjection);
        // merging transcripts
        transcriptService.mergeTranscripts(transcript1, transcript2,assemblage)
        featureService.calculateCDS(transcript1,false,assemblage)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1,assemblage)

        // calling handleDynamicIsoformOverlap() to account for all overlapping transcripts to the merged transcript
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript1)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it,false,assemblage))
            }
            fireEvent(assemblage, updateFeatureContainer, AnnotationEvent.Operation.UPDATE)
        }

        Gene mergedTranscriptGene = transcriptService.getGene(transcript1)
        transcript1.name = transcript1.name ?: nameService.generateUniqueName(transcript1)

        JSONObject returnObject = createJSONFeatureContainerFromFeatures(assemblage,featureService.getTopLevelFeature(transcript1))

        // update feature container for update annotation event for transcripts of gene1
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        gene1.refresh()
        for (Transcript transcript : transcriptService.getTranscripts(gene1)) {
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript,false,assemblage));
        }
        if(mergedTranscriptGene && mergedTranscriptGene!=gene1){
            for (Transcript transcript : transcriptService.getTranscripts(mergedTranscriptGene)) {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript,false,assemblage));
            }
        }
        JSONArray updatedFeatures = updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
        updatedFeatures = featureProjectionService.projectTrack(updatedFeatures,assemblage,false)
        updateFeatureContainer.put(FeatureStringEnum.FEATURES.value, updatedFeatures)

        // delete feature container for delete annotation event
        JSONObject deleteFeatureContainer = createJSONFeatureContainer()
        deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(transcript2JSONObject);

        // TODO: history tracking
        JSONObject featureForHistory = createJSONFeatureContainer()
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript1,false,assemblage));

        Boolean suppressHistory = inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value) ? inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value) : false
        if (!suppressHistory) {
            JSONArray oldJsonArray = new JSONArray()
            oldJsonArray.add(jsonTranscript1)
            oldJsonArray.add(jsonTranscript2)
            try {
                log.debug "trying to add history"
                featureEventService.addMergeFeatureEvent(gene1Name, transcript1UniqueName
                        , gene2Name, transcript2UniqueName
                        , inputObject, oldJsonArray
                        , featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0)
                        , permissionService.getCurrentUser(inputObject)
                )
                log.debug "ADDED history"
            } catch (e) {
                log.error " There was a problem adding history for this merge event ${e}"
            }
        }

        AnnotationEvent deleteAnnotationEvent = new AnnotationEvent(
                features: deleteFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.DELETE
        )

        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.UPDATE
        )

        // firing update and delete annotation event
        fireAnnotationEvent(updateAnnotationEvent, deleteAnnotationEvent)

        return returnObject
    }

    @Timed
    def duplicateTranscript(JSONObject inputObject) {
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        Transcript transcript = Transcript.findByUniqueName(inputObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript duplicateTranscript = transcriptService.duplicateTranscript(transcript,assemblage)
        duplicateTranscript.save()
        Feature topFeature = featureService.getTopLevelFeature(transcript)
        topFeature.save()
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topFeature,false,assemblage))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , assemblage: assemblage
                , operation: AnnotationEvent.Operation.ADD
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def undo(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        permissionService.getCurrentUser(inputObject)

        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            int count = inputObject.containsKey(FeatureStringEnum.COUNT.value) ? inputObject.getInt(FeatureStringEnum.COUNT.value) : false
            jsonFeature = permissionService.copyRequestValues(inputObject, jsonFeature)
            featureEventService.undo(jsonFeature, count)
        }
        return new JSONObject()
    }

    @Timed
    def redo(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        permissionService.getCurrentUser(inputObject)

        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            int count = inputObject.containsKey(FeatureStringEnum.COUNT.value) ? inputObject.getInt(FeatureStringEnum.COUNT.value) : false
            jsonFeature = permissionService.copyRequestValues(inputObject, jsonFeature)
            featureEventService.redo(jsonFeature, count)
        }
        return new JSONObject()
    }

    def changeAnnotationType(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Assemblage assemblage = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        User user = permissionService.getCurrentUser(inputObject)
        JSONObject featureContainer = createJSONFeatureContainer()

        def singletonFeatureTypes = [RepeatRegion.alternateCvTerm, TransposableElement.alternateCvTerm]
        def rnaFeatureTypes = [MRNA.alternateCvTerm, MiRNA.alternateCvTerm, NcRNA.alternateCvTerm, RRNA.alternateCvTerm, SnRNA.alternateCvTerm, SnoRNA.alternateCvTerm, TRNA.alternateCvTerm, Transcript.alternateCvTerm]

        for (int i = 0; i < features.length(); i++) {
            String type = features.get(i).type
            String uniqueName = features.get(i).uniquename
            Feature feature = Feature.findByUniqueName(uniqueName)
            FeatureEvent currentFeatureEvent = featureEventService.findCurrentFeatureEvent(feature.uniqueName).get(0)
            JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature,false,assemblage)
            JSONObject originalFeatureJsonObject = JSON.parse(currentFeatureEvent.newFeaturesJsonArray) as JSONObject
            String originalType = feature.alternateCvTerm ? feature.alternateCvTerm : feature.cvTerm

            if (originalType == type) {
                log.warn "Cannot change ${uniqueName} from ${originalType} -> ${type}. Nothing to do."
            } else if (originalType in singletonFeatureTypes && type in rnaFeatureTypes) {
                log.error "Not enough information available to change ${uniqueName} from ${originalType} -> ${type}."
            } else {
                log.info "Changing ${uniqueName} from ${originalType} to ${type}"
                Feature newFeature = featureService.changeAnnotationType(inputObject, feature, assemblage, user, type)
                JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature,false,assemblage)
                log.debug "New feature json object: ${newFeatureJsonObject.toString()}"
                JSONArray oldFeatureJsonArray = new JSONArray()
                JSONArray newFeatureJsonArray = new JSONArray()
                oldFeatureJsonArray.add(originalFeatureJsonObject)
                newFeatureJsonArray.add(newFeatureJsonObject)
                featureEventService.addNewFeatureEvent(FeatureOperation.CHANGE_ANNOTATION_TYPE, feature.name,
                        uniqueName, inputObject, oldFeatureJsonArray, newFeatureJsonArray, user)

                JSONObject deleteFeatureContainer = createJSONFeatureContainer()
                deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(currentFeatureJsonObject)
                AnnotationEvent deleteAnnotationEvent = new AnnotationEvent(
                        features: deleteFeatureContainer,
                        assemblage: assemblage,
                        operation: AnnotationEvent.Operation.DELETE
                )
                fireAnnotationEvent(deleteAnnotationEvent)

                JSONObject addFeatureContainer = createJSONFeatureContainer()
                addFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeatureJsonObject)
                AnnotationEvent addAnnotationEvent = new AnnotationEvent(
                        features: addFeatureContainer,
                        assemblage: assemblage,
                        operation: AnnotationEvent.Operation.ADD
                )
                fireAnnotationEvent(addAnnotationEvent)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeatureJsonObject)
            }
        }

        return featureContainer
    }
}
