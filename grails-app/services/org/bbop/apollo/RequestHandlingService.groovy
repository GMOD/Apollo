package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation

//import grails.compiler.GrailsCompileStatic
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
    def variantService
    def nonCanonicalSplitSiteService
    def configWrapperService
    def nameService
    def permissionService
    def preferenceService
    def featurePropertyService
    def featureEventService
    def brokerMessagingTemplate


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
            DeletionArtifact.class.name,
            InsertionArtifact.class.name,
            SubstitutionArtifact.class.name
    ]

    public static final List<String> viewableSequenceAlterationList = [
            Deletion.class.name,
            Insertion.class.name,
            Substitution.class.name,
            SNV.class.name,
            SNP.class.name,
            MNV.class.name,
            MNP.class.name,
            Indel.class.name
    ]

    public static
    final List<String> viewableAnnotationList = viewableAnnotationFeatureList + viewableAnnotationTranscriptParentList + viewableSequenceAlterationList
    public static
    final List<String> viewableAnnotationTypesList = viewableAnnotationFeatureList + viewableAnnotationTranscriptList + viewableAnnotationTranscriptParentList

    private String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }


    JSONObject setSymbol(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer()

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Sequence sequence = null

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String symbolString = jsonFeature.getString(FeatureStringEnum.SYMBOL.value)
            sequence = sequence ?: feature.getFeatureLocation().getSequence()
            permissionService.checkPermissions(inputObject, sequence.organism, PermissionEnum.WRITE)

            feature.symbol = symbolString
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }


        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return new JSONObject()
    }

    JSONObject setDescription(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = null

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String descriptionString = jsonFeature.getString(FeatureStringEnum.DESCRIPTION.value)
            sequence = sequence ?: feature.getFeatureLocation().getSequence()
            permissionService.checkPermissions(inputObject, sequence.organism, PermissionEnum.WRITE)


            feature.description = descriptionString
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    private JSONObject wrapFeature(JSONObject jsonObject, Feature feature) {

        // only pass in transcript
        if (feature instanceof Gene) {
            feature.parentFeatureRelationships.childFeature.each { childFeature ->
                jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(childFeature))
            }
        } else {
            jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        return jsonObject
    }

    def deleteNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)

        }
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def deleteComments(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            JSONArray commentsArray = jsonFeature.getJSONArray(FeatureStringEnum.COMMENTS.value)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (int commentIndex = 0; commentIndex < commentsArray.size(); commentIndex++) {
                String commentString = commentsArray.getString(commentIndex);
                featurePropertyService.deleteComment(feature, commentString)
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)

        }
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def updateComments(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            JSONArray oldComments = jsonFeature.getJSONArray(FeatureStringEnum.OLD_COMMENTS.value)
            JSONArray newComments = jsonFeature.getJSONArray(FeatureStringEnum.NEW_COMMENTS.value)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            for (int commentIndex = 0; commentIndex < oldComments.size(); commentIndex++) {
                String oldCommentString = oldComments.getString(commentIndex)
                String newCommentString = newComments.getString(commentIndex)

                Comment comment = Comment.findByFeatureAndValue(feature, oldCommentString)
                comment.value = newCommentString
                comment.save()
            }
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)

        }
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
        return updateFeatureContainer
    }

    def setStatus(JSONObject inputObject) {
        log.debug "status being set ${inputObject as JSON}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)

        }
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
        return updateFeatureContainer
    }

    /**
     * being invoked the RHS
     */
    def deleteStatus(JSONObject inputObject) {
        log.debug "deleteStatus ${inputObject as JSON}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            String statusString = jsonFeature.getString(FeatureStringEnum.STATUS.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            feature.status = null
            feature.save()
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)

        }
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
        return updateFeatureContainer
    }


    def getComments(JSONObject inputObject) {
        log.debug "getComments"
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.READ)

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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.ADD
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer


    }

    JSONObject setName(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            if (!sequence) sequence = feature.getFeatureLocation().getSequence()
            feature.name = jsonFeature.get(FeatureStringEnum.NAME.value)


            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    @Timed
    @Transactional(readOnly = true)
    JSONObject getFeatures(JSONObject inputObject) {
        long start = System.currentTimeMillis()

        String sequenceName = permissionService.getSequenceNameFromInput(inputObject)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.READ)
        if (sequenceName != sequence.name) {
            sequence = Sequence.findByNameAndOrganism(sequenceName, sequence.organism)
            preferenceService.setCurrentSequence(permissionService.getCurrentUser(inputObject), sequence, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        }
        log.debug "getFeatures for organism -> ${sequence.organism.commonName} and ${sequence.name}"

        def features = Feature.createCriteria().listDistinct {
            featureLocations {
                eq('sequence', sequence)
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
            'in'('class', viewableAnnotationTranscriptList + viewableAnnotationFeatureList + viewableSequenceAlterationList)
        }


        JSONArray jsonFeatures = new JSONArray()
        features.each { feature ->
            JSONObject jsonObject = featureService.convertFeatureToJSON(feature, false)
            jsonFeatures.put(jsonObject)
        }

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
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String uniqueName = features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value)
        Transcript transcript = Transcript.findByUniqueName(uniqueName)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript)

        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i);
            // could be that this is null
            Exon gsolExon = (Exon) featureService.convertJSONToFeature(jsonExon, sequence)

            featureService.updateNewGsolFeatureAttributes(gsolExon, sequence);

            if (gsolExon.getFmin() < 0 || gsolExon.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates")
            }

            transcriptService.addExon(transcript, gsolExon, false)

            gsolExon.save()
        }
        featureService.removeExonOverlapsAndAdjacencies(transcript)
        transcriptService.updateGeneBoundaries(transcript)
        featureService.calculateCDS(transcript)

        transcript.save(flush: true)
        transcript.attach()
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
        transcript.save(flush: true)

        // TODO: one of these two versions . . .
        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false)
        JSONObject returnObject = createJSONFeatureContainer(newJsonObject)

        Gene gene = transcriptService.getGene(transcript)

        featureEventService.addNewFeatureEvent(FeatureOperation.ADD_EXON, gene.name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject

    }

    @Timed
    JSONObject addTranscript(JSONObject inputObject) throws Exception {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject returnObject = createJSONFeatureContainer()

        log.info "addTranscript ${inputObject.toString()}"
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        log.debug "sequence: ${sequence}"
        log.debug "organism: ${sequence.organism}"
        log.info "number of features: ${featuresArray?.size()}"

        boolean useName = false
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
            useName = jsonTranscript.has(FeatureStringEnum.USE_NAME.value) ? jsonTranscript.getBoolean(FeatureStringEnum.USE_NAME.value) : false
            Transcript transcript = featureService.generateTranscript(jsonTranscript, sequence, suppressHistory, useCDS, useName)

            // should automatically write to history
            transcript.save(flush: true)
            transcriptList.add(transcript)

            Gene gene = transcriptService.getGene(transcript)
            inputObject.put(FeatureStringEnum.NAME.value, gene.name)

            if (!suppressHistory) {
                def json = featureService.convertFeatureToJSON(transcript)
                featureEventService.addNewFeatureEventWithUser(FeatureOperation.ADD_TRANSCRIPT, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, json, permissionService.getCurrentUser(inputObject))
                transcriptJSONList += json
            }
        }


        returnObject.put(FeatureStringEnum.FEATURES.value, transcriptJSONList as JSONArray)

        if (!suppressEvents) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnObject
                    , sequence: sequence
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
    JSONObject setTranslationStart(JSONObject inputObject) throws AnnotationException{
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        boolean setStart = transcriptJSONObject.has(FeatureStringEnum.LOCATION.value);
        if (!setStart) {
            CDS cds = transcriptService.getCDS(transcript)
            cdsService.setManuallySetTranslationStart(cds, false)
            featureService.calculateCDS(transcript)
        } else {
            JSONObject jsonCDSLocation = transcriptJSONObject.getJSONObject(FeatureStringEnum.LOCATION.value);
            featureService.setTranslationStart(transcript, jsonCDSLocation.getInt(FeatureStringEnum.FMIN.value), true)
        }

        transcript.save()

        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        Gene gene = transcriptService.getGene(transcript)
        JSONObject newJSONObject = featureService.convertFeatureToJSON(transcript, false)
        featureEventService.addNewFeatureEvent(setStart ? FeatureOperation.SET_TRANSLATION_START : FeatureOperation.UNSET_TRANSLATION_START, gene.name, transcript.uniqueName, inputObject, transcriptJSONObject, newJSONObject, permissionService.getCurrentUser(inputObject))
        JSONObject featureContainer = createJSONFeatureContainer(newJSONObject);

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }

    /**
     * Transcript is the first object
     * @param inputObject
     */
    @Timed
    JSONObject setTranslationEnd(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        boolean setEnd = transcriptJSONObject.has(FeatureStringEnum.LOCATION.value);
        if (!setEnd) {
            CDS cds = transcriptService.getCDS(transcript)
            cdsService.setManuallySetTranslationEnd(cds, false)
            featureService.calculateCDS(transcript)
        } else {
            JSONObject jsonCDSLocation = transcriptJSONObject.getJSONObject(FeatureStringEnum.LOCATION.value);
            //featureService.setTranslationEnd(transcript, jsonCDSLocation.getInt(FeatureStringEnum.FMAX.value), true)
            //TODO: Should translationStart be allowed to be set automatically?
            featureService.setTranslationEnd(transcript, jsonCDSLocation.getInt(FeatureStringEnum.FMAX.value))
        }
        transcript.save()
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject newJSONObject = featureService.convertFeatureToJSON(transcript, false)
        featureEventService.addNewFeatureEvent(setEnd ? FeatureOperation.SET_TRANSLATION_END : FeatureOperation.UNSET_TRANSLATION_END, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, transcriptJSONObject, newJSONObject, permissionService.getCurrentUser(inputObject))
        JSONObject featureContainer = createJSONFeatureContainer(newJSONObject);


        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }

    @Timed
    def setReadthroughStopCodon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0)

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript, false)

        boolean readThroughStopCodon = transcriptJSONObject.getBoolean(FeatureStringEnum.READTHROUGH_STOP_CODON.value)
        featureService.calculateCDS(transcript, readThroughStopCodon);
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        transcript.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false))

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false)
        featureEventService.addNewFeatureEvent(readThroughStopCodon ? FeatureOperation.SET_READTHROUGH_STOP_CODON : FeatureOperation.UNSET_READTHROUGH_STOP_CODON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        JSONObject returnObject = createJSONFeatureContainer(newJsonObject);

        return returnObject
    }

    @Timed
    def setAcceptor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONObject featureContainer = createJSONFeatureContainer()
        JSONArray transcriptArray = new JSONArray()
        featureContainer.put(FeatureStringEnum.FEATURES.value, transcriptArray)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject oldJsonObject = features.getJSONObject(i)
            String uniqueName = oldJsonObject.getString(FeatureStringEnum.UNIQUENAME.value)
            Exon exon = Exon.findByUniqueName(uniqueName)
            Transcript transcript = exonService.getTranscript(exon)

            if (upstreamDonor) {
                exonService.setToUpstreamAcceptor(exon)
            } else {
                exonService.setToDownstreamAcceptor(exon)
            }

            featureService.calculateCDS(transcript)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
                }
                if (sequence) {
                    AnnotationEvent annotationEvent = new AnnotationEvent(
                            features: updateFeatureContainer,
                            sequence: sequence,
                            operation: AnnotationEvent.Operation.UPDATE
                    )
                    fireAnnotationEvent(annotationEvent)
                }
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript)
            transcriptArray.add(newJsonObject)
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
        }



        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }


    @Timed
    def setDonor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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


            featureService.calculateCDS(transcript)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
                }
                if (sequence) {
                    AnnotationEvent annotationEvent = new AnnotationEvent(
                            features: updateFeatureContainer,
                            sequence: sequence,
                            operation: AnnotationEvent.Operation.UPDATE
                    )
                    fireAnnotationEvent(annotationEvent)
                }
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript)
            transcriptArray.add(newJsonObject)
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))
        }



        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }

    @Timed
    JSONObject setLongestOrf(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        featureService.setLongestORF(transcript, false)

        transcript.save(flush: true, insert: false)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false));

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return featureContainer
    }

    /**
     * TODO: test in interface
     * @param inputObject
     * @return
     */
    @Timed
    JSONObject setExonBoundaries(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            Exon exon = Exon.findByUniqueName(locationCommand.getString(FeatureStringEnum.UNIQUENAME.value))
            Transcript transcript = exonService.getTranscript(exon)
            JSONObject oldTranscriptJsonObject = featureService.convertFeatureToJSON(transcript)


            FeatureLocation transcriptFeatureLocation = FeatureLocation.findByFeature(transcript)
            FeatureLocation exonFeatureLocation = FeatureLocation.findByFeature(exon)
            if (transcriptFeatureLocation.fmin == exonFeatureLocation.fmin) {
                transcriptFeatureLocation.fmin = fmin
            }
            if (transcriptFeatureLocation.fmax == exonFeatureLocation.fmax) {
                transcriptFeatureLocation.fmax = fmax
            }


            exonFeatureLocation.fmin = fmin
            exonFeatureLocation.fmax = fmax
            featureService.removeExonOverlapsAndAdjacencies(transcript)
            transcriptService.updateGeneBoundaries(transcript)

            exon.save()

            featureService.calculateCDS(transcript)
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

            transcript.save()
            def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
            if (transcriptsToUpdate.size() > 0) {
                JSONObject updateFeatureContainer = createJSONFeatureContainer()
                transcriptsToUpdate.each {
                    updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
                }
                if (sequence) {
                    AnnotationEvent annotationEvent = new AnnotationEvent(
                            features: updateFeatureContainer,
                            sequence: sequence,
                            operation: AnnotationEvent.Operation.UPDATE
                    )
                    fireAnnotationEvent(annotationEvent)
                }
            }

            JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript, false)
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonObject);
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldTranscriptJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))

        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)


        return returnObject
    }

    @Timed
    JSONObject setBoundaries(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONObject returnObject = createJSONFeatureContainerFromFeatures()

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
            if (feature instanceof SequenceAlteration) {
                continue
            }
//            editor.setBoundaries(feature, fmin, fmax);
            FeatureLocation featureLocation = FeatureLocation.findByFeature(feature)

            featureLocation.fmin = fmin
            featureLocation.fmax = fmax
            feature.save()

            JSONObject newJsonFeature = featureService.convertFeatureToJSON(feature, false)
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonFeature);
            featureEventService.addNewFeatureEvent(FeatureOperation.SET_BOUNDARIES, feature.name, feature.uniqueName, inputObject, oldJsonFeature, newJsonFeature, permissionService.getCurrentUser(inputObject))
        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
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

    void sendAnnotationEvent(String returnString, Sequence sequence) {
        if (returnString.startsWith("[")) {
            returnString = returnString.substring(1, returnString.length() - 1)
        }
        try {
            brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/" + sequence.organismId + "/" + sequence.id, returnString
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
            if (event.username) {
                features.put(FeatureStringEnum.USERNAME.value, event.username);
            }
            operations.put(features);
        }
        catch (JSONException e) {
            log.error("error handling change event ${event}: ${e}")
        }

        sendAnnotationEvent(operations.toString(), event.sequence);

    }

    @Timed
    private JSONObject createJSONFeatureContainerFromFeatures(Feature... features) throws JSONException {
        def jsonObjects = new ArrayList()
        for (Feature feature in features) {
            JSONObject featureObject = featureService.convertFeatureToJSON(feature, false)
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            SequenceAlterationArtifact sequenceAlteration = SequenceAlterationArtifact.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            FeatureLocation sequenceAlterationFeatureLocation = sequenceAlteration.getFeatureLocation()
            deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration, true));
            FeatureLocation.deleteAll(sequenceAlteration.featureLocations)
            sequenceAlteration.delete(flush: true)

            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlterationFeatureLocation, false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        CDS cds = transcriptService.getCDS(transcript)
                        featureService.setLongestORF(transcript, cdsService.hasStopCodonReadThrough(cds))
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, true));
                    }
                    feature.save(flush: true)
                }
            }
        }

        AnnotationEvent deleteAnnotationEvent = new AnnotationEvent(
                features: deleteFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.DELETE
                , sequenceAlterationEvent: true
        )
        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
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

        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        User activeUser = permissionService.getCurrentUser(inputObject)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Integer fmin = jsonFeature.get(FeatureStringEnum.LOCATION.value)?.fmin
            Integer fmax = jsonFeature.get(FeatureStringEnum.LOCATION.value)?.fmax
            if(!fmax){
                fmax = fmin
                jsonFeature.get(FeatureStringEnum.LOCATION.value).fmax = fmax
            }
            if (featureService.getOverlappingSequenceAlterations(sequence, fmin, fmax)) {
                // found an overlapping sequence alteration
                throw new AnnotationException("Cannot create an overlapping sequence alteration");
            }
            SequenceAlterationArtifact sequenceAlteration = (SequenceAlterationArtifact) featureService.convertJSONToFeature(jsonFeature, sequence)
            if (activeUser) {
                featureService.setOwner(sequenceAlteration, activeUser)
            } else {
                log.error("Unable to find valid user to set on transcript!")
            }
            sequenceAlteration.save()

            featureService.updateNewGsolFeatureAttributes(sequenceAlteration, sequence)

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


            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        CDS cds = transcriptService.getCDS(transcript)
                        featureService.setLongestORF(transcript, cdsService.hasStopCodonReadThrough(cds))
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, false));
                    }
                }
            }
            addFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration, true));
        }

        AnnotationEvent addAnnotationEvent = new AnnotationEvent(
                features: addFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
                , sequenceAlterationEvent: true
        )
        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )
        fireAnnotationEvent(addAnnotationEvent)
        fireAnnotationEvent(updateAnnotationEvent)

        return addFeatureContainer

    }

    def addNonReservedProperties(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
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

        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
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
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
        }

        return updateFeatureContainer
    }

    def updateNonReservedProperties(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));

        }
//        fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
    }

    def lockFeature(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject featureContainer = createJSONFeatureContainer();
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

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
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false));
            }
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def unlockFeature(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject featureContainer = createJSONFeatureContainer();
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)


        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            FeatureProperty featureProperty = FeatureProperty.findByFeatureAndValue(feature, FeatureStringEnum.LOCKED.value)
            if (featureProperty) {
                feature.removeFromFeatureProperties(featureProperty)
                feature.save()
                FeatureProperty.deleteAll(featureProperty)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
            } else {
                log.error("Feature ${feature.name} was not locked.  Doing nothing.")
            }
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def flipStrand(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        JSONObject featureContainer = createJSONFeatureContainer()
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i)
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))

            if (feature instanceof Transcript) {
                feature = transcriptService.flipTranscriptStrand((Transcript) feature)
                featureService.setLongestORF((Transcript) feature)
                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites((Transcript) feature)
                def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(feature)
                if (transcriptService.getGene((Transcript) feature)) {
                    featureEventService.addNewFeatureEventWithUser(FeatureOperation.FLIP_STRAND, transcriptService.getGene((Transcript) feature).name, feature.uniqueName, inputObject, featureService.convertFeatureToJSON((Transcript) feature), permissionService.getCurrentUser(inputObject))
                    if (transcriptsToUpdate.size()) {
                        JSONObject updateFeatureContainer = createJSONFeatureContainer()
                        transcriptsToUpdate.each {
                            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
                        }
                        if (sequence) {
                            AnnotationEvent annotationEvent = new AnnotationEvent(
                                    features: updateFeatureContainer,
                                    sequence: sequence,
                                    operation: AnnotationEvent.Operation.UPDATE
                            )
                            fireAnnotationEvent(annotationEvent)
                        }
                    }
                }
                else{
                    log.error("Transcript failed to produce gene with moving to opposite strand: "+feature.name)
                }
            } else {
                feature = featureService.flipStrand(feature)
                featureEventService.addNewFeatureEventWithUser(FeatureOperation.FLIP_STRAND, feature.name, feature.uniqueName, inputObject, featureService.convertFeatureToJSON(feature), permissionService.getCurrentUser(inputObject))
            }
            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false));
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def mergeExons(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Exon exon1 = (Exon) Exon.findByUniqueName(features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value));
        Exon exon2 = (Exon) Exon.findByUniqueName(features.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value));
        Transcript transcript1 = exonService.getTranscript(exon1)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript1)
        exonService.mergeExons(exon1, exon2)
        featureService.calculateCDS(transcript1);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1);
        // rename?

        transcript1.save(flush: true)
        exon1.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript1)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript1)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.MERGE_EXONS, transcriptService.getGene(transcript1).name, transcript1.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def splitExon(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonExon = features.getJSONObject(0)
        Exon exon = (Exon) Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value);
        Transcript transcript = exonService.getTranscript(exon)
        JSONObject oldJsonObject = featureService.convertFeatureToJSON(transcript)


        Exon splitExon = exonService.splitExon(exon, exonLocation.getInt(FeatureStringEnum.FMAX.value), exonLocation.getInt(FeatureStringEnum.FMIN.value))
        featureService.updateNewGsolFeatureAttributes(splitExon, sequence)
        featureService.calculateCDS(transcript)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

        exon.save()
        transcript.save(flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject);

        featureEventService.addNewFeatureEvent(FeatureOperation.SPLIT_EXON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonObject, newJsonObject, permissionService.getCurrentUser(inputObject))


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonTranscript = features.getJSONObject(0)

        Transcript transcript = Transcript.findByUniqueName(jsonTranscript.getString(FeatureStringEnum.UNIQUENAME.value));
        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i)
            Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));
            checkOwnersDelete(exon, inputObject)

            exonService.deleteExon(transcript, exon);
        }
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        Feature topLevelFeature = featureService.getTopLevelFeature(transcript)
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topLevelFeature))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.DELETE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def addFeature(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        log.debug "adding sequence with found sequence ${sequence}"
        User user = permissionService.getCurrentUser(inputObject)

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject returnObject = createJSONFeatureContainer()

        boolean useName = false
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
            useName = jsonFeature.has(FeatureStringEnum.USE_NAME.value) ? jsonFeature.get(FeatureStringEnum.USE_NAME.value) : false
            if (jsonFeature.get(FeatureStringEnum.TYPE.value).name == Gene.cvTerm ||
                    jsonFeature.get(FeatureStringEnum.TYPE.value).name == Pseudogene.cvTerm) {
                // if jsonFeature is of type gene or pseudogene
                JSONObject jsonGene = JSON.parse(jsonFeature.toString())
                jsonGene.remove(FeatureStringEnum.CHILDREN.value)
                if (jsonFeature.containsKey(FeatureStringEnum.CHILDREN.value)) {
                    for (JSONObject transcriptJsonFeature in jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                        // look at its children JSON Array to get the features at the *RNA level
                        // adding jsonGene to each individual transcript
                        transcriptJsonFeature.put(FeatureStringEnum.PARENT.value, jsonGene)
                        Feature newFeature = featureService.addFeature(transcriptJsonFeature, sequence, user, suppressHistory, useName)
                        JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature)
                        JSONObject jsonObject = newFeatureJsonObject

                        if (!suppressHistory) {
                            featureEventService.addNewFeatureEvent(FeatureOperation.ADD_FEATURE, newFeature.name, newFeature.uniqueName, inputObject, newFeatureJsonObject, user)
                        }
                        returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonObject);
                    }
                }
            } else {
                // jsonFeature is of type transposable_element or repeat_region
                Feature newFeature = featureService.addFeature(jsonFeature, sequence, user, suppressHistory, true)
                JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature)
                JSONObject jsonObject = newFeatureJsonObject

                if (!suppressHistory) {
                    featureEventService.addNewFeatureEvent(FeatureOperation.ADD_FEATURE, newFeature.name, newFeature.uniqueName, inputObject, newFeatureJsonObject, user)
                }
                returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonObject);
            }
        }

        if (!suppressEvents) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnObject
                    , sequence: sequence
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
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

            checkOwnersDelete(feature, inputObject)

            log.debug "feature found to delete ${feature?.name}"
            if (feature) {
                if (feature instanceof Exon) {
                    Transcript transcript = exonService.getTranscript((Exon) feature)
                    // if its the same transcript, we don't want to overwrite it
                    if (!oldFeatureMap.containsKey(transcript.uniqueName)) {
                        oldFeatureMap.put(transcript.uniqueName, featureService.convertFeatureToJSON(transcript))
                    }
                } else {
                    if (!oldFeatureMap.containsKey(feature.uniqueName)) {
                        oldFeatureMap.put(feature.uniqueName, featureService.convertFeatureToJSON(feature))
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
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
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
                                    , sequence: sequence
                                    , operation: AnnotationEvent.Operation.DELETE
                            )

                            fireAnnotationEvent(annotationEvent)
                        }
                    } else {
                        featureRelationshipService.removeFeatureRelationship(gene, transcript)
                        featureRelationshipService.deleteFeatureAndChildren(transcript)
                        featureService.updateGeneBoundaries(gene)
                        gene.save()

                        if (!suppressEvents) {
                            AnnotationEvent annotationEvent = new AnnotationEvent(
                                    features: featureContainer
                                    , sequence: sequence
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
                                , sequence: sequence
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
                    featureService.calculateCDS(transcript)
                    nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                    transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)
                    Gene gene = transcriptService.getGene(transcript)
                    gene.save()

                    def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
                    featureService.updateGeneBoundaries(gene)
                    if (transcriptsToUpdate.size() > 0) {
                        JSONObject updateFeatureContainer = createJSONFeatureContainer()
                        transcriptsToUpdate.each {
                            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
                        }
                        if (sequence) {
                            AnnotationEvent annotationEvent = new AnnotationEvent(
                                    features: updateFeatureContainer,
                                    sequence: sequence,
                                    operation: AnnotationEvent.Operation.UPDATE
                            )
                            fireAnnotationEvent(annotationEvent)
                        }
                    }

                    featureOperation = FeatureOperation.DELETE_EXON
                    featureName = gene.name
                } else {
                    feature.save()
                    featureOperation = FeatureOperation.DELETE_FEATURE
                    featureName = feature.name
                }

                JSONObject newJsonObject = featureService.convertFeatureToJSON(feature)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newJsonObject);


                if (!suppressEvents) {
                    featureEventService.addNewFeatureEvent(featureOperation, featureName, feature.uniqueName, inputObject, new JSONObject().put(FeatureStringEnum.FEATURES.value, oldJsonObjectsArray), newJsonObject, permissionService.getCurrentUser(inputObject))
                }
            }
        }



        if (!suppressEvents) {
            AnnotationEvent finalAnnotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
            )

            finalAnnotationEvent.operation = isUpdateOperation ? AnnotationEvent.Operation.UPDATE : AnnotationEvent.Operation.DELETE
            fireAnnotationEvent(finalAnnotationEvent)
        }

        return createJSONFeatureContainer()
    }

    def checkOwnersDelete(Feature feature, JSONObject inputObject) {
        if (configWrapperService.onlyOwnersDelete) {
            def currentUser = permissionService.getCurrentUser(inputObject)
            def isAdmin = permissionService.isUserGlobalAdmin(currentUser)
            def owners = findOwners(feature)
            if (!isAdmin && !(currentUser in owners)) {
                throw new AnnotationException("Only feature owner or admin may delete, change type, or revert annotation to an earlier state")
            }
        }
    }

    private findOwners(Feature feature) {
        if (!feature) return null
        if (feature.owners) {
            return feature.owners
        }
        return findOwners(featureRelationshipService.getParentForFeature(feature))
    }

    @Timed
    def makeIntron(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonExon = featuresArray.getJSONObject(0)
        Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value))
        Transcript transcript = exonService.getTranscript(exon)
        JSONObject oldJsonTranscript = featureService.convertFeatureToJSON(transcript)
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value)

        Exon splitExon = exonService.makeIntron(
                exon
                , exonLocation.getInt(FeatureStringEnum.FMIN.value)
                , configWrapperService.getDefaultMinimumIntronSize()
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
        featureService.updateNewGsolFeatureAttributes(splitExon, sequence)
        featureService.calculateCDS(transcript)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

        transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)

        transcript.save(failOnError: true)
        exon.save(failOnError: true)
        splitExon.save(failOnError: true, flush: true)
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        JSONObject newJsonObject = featureService.convertFeatureToJSON(transcript)
        JSONObject featureContainer = createJSONFeatureContainer(newJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.SPLIT_EXON, transcriptService.getGene(transcript).name, transcript.uniqueName, inputObject, oldJsonTranscript, newJsonObject, permissionService.getCurrentUser(inputObject))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    @Timed
    def splitTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        Exon exon1 = Exon.findByUniqueName(featuresArray.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))
        Exon exon2 = Exon.findByUniqueName(featuresArray.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript transcript1 = exonService.getTranscript(exon1)
        // transcript2 should contain the second part of transcript1 starting from exon2
        Transcript transcript2 = transcriptService.splitTranscript(transcript1, exon1, exon2)

        featureService.updateNewGsolFeatureAttributes(transcript2, sequence);
        featureService.calculateCDS(transcript1)
        featureService.calculateCDS(transcript2)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript2);
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
            updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t));
        }

        if (updatedGene1.uniqueName != updatedGene2.uniqueName) {
            for (Transcript t : transcriptService.getTranscripts(updatedGene2)) {
                updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t));
            }
        }

        // returnContainer for return object
        Feature topLevelExonFeature = featureService.getTopLevelFeature(transcript1)
        JSONObject returnContainer = createJSONFeatureContainerFromFeatures(topLevelExonFeature)

        // features to add to history
        JSONObject featureForHistory = createJSONFeatureContainer()
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript1))
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript2))

        // add history for transcript1 and transcript2
        Boolean suppressHistory = inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value) ? inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value) : false
        if (!suppressHistory) {
            try {
                featureEventService.addSplitFeatureEvent(updatedGene1.name, transcript1.uniqueName
                        , updatedGene2.name, transcript2.uniqueName
                        , inputObject
                        , featureService.convertFeatureToJSON(transcript1)
                        , featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value)
                        , permissionService.getCurrentUser(inputObject)
                )
            } catch (e) {
                log.error "There was an error adding history ${e}"
            }
        }

        // firing annotation update event
        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateContainer,
                sequence: sequence,
                operation: AnnotationEvent.Operation.UPDATE
        )
        fireAnnotationEvent(updateAnnotationEvent)

        return returnContainer
    }

    @Timed
    def mergeTranscripts(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
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
            a.fmin <=> b.fmin ?: a.fmax <=> b.fmax ?: a.name <=> b.name
        }

        if (transcript1.strand == Strand.NEGATIVE.value) {
            sortedTranscripts.reverse(true)
        }

        if (sortedTranscripts.get(0).cvTerm == Transcript.cvTerm && sortedTranscripts.get(1).cvTerm != Transcript.cvTerm) {
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

        JSONObject transcript2JSONObject = featureService.convertFeatureToJSON(transcript2)

        // calculate longest ORF, to reset any changes made to the CDS, before a merge
        featureService.setLongestORF(transcript1);
        featureService.setLongestORF(transcript2);
        // merging transcripts
        transcriptService.mergeTranscripts(transcript1, transcript2)
        featureService.calculateCDS(transcript1)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1)

        // calling handleDynamicIsoformOverlap() to account for all overlapping transcripts to the merged transcript
        def transcriptsToUpdate = featureService.handleDynamicIsoformOverlap(transcript1)
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = createJSONFeatureContainer()
            transcriptsToUpdate.each {
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(it))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                fireAnnotationEvent(annotationEvent)
            }
        }

        Gene mergedTranscriptGene = transcriptService.getGene(transcript1)
        transcript1.name = transcript1.name ?: nameService.generateUniqueName(transcript1)

        JSONObject returnObject = createJSONFeatureContainerFromFeatures(featureService.getTopLevelFeature(transcript1))

        // update feature container for update annotation event for transcripts of gene1
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        gene1.refresh()
        for (Transcript transcript : transcriptService.getTranscripts(gene1)) {
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript))
        }
        for (Transcript transcript : transcriptService.getTranscripts(mergedTranscriptGene)) {
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript))
        }

        // delete feature container for delete annotation event
        JSONObject deleteFeatureContainer = createJSONFeatureContainer()
        deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(transcript2JSONObject)

        // TODO: history tracking
        JSONObject featureForHistory = createJSONFeatureContainer()
        featureForHistory.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript1))

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
                , sequence: sequence
                , operation: AnnotationEvent.Operation.DELETE
        )

        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        // firing update and delete annotation event
        fireAnnotationEvent(updateAnnotationEvent, deleteAnnotationEvent)

        return returnObject
    }

    @Timed
    def duplicateTranscript(JSONObject inputObject) {
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        Transcript transcript = Transcript.findByUniqueName(inputObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript duplicateTranscript = transcriptService.duplicateTranscript(transcript)
        duplicateTranscript.save()
        Feature topFeature = featureService.getTopLevelFeature(transcript)
        topFeature.save()
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topFeature))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
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
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        User user = permissionService.getCurrentUser(inputObject)
        JSONObject featureContainer = createJSONFeatureContainer()

        def singletonFeatureTypes = [RepeatRegion.cvTerm, TransposableElement.cvTerm]
        def rnaFeatureTypes = [MRNA.cvTerm, MiRNA.cvTerm, NcRNA.cvTerm, RRNA.cvTerm, SnRNA.cvTerm, SnoRNA.cvTerm, TRNA.cvTerm, Transcript.cvTerm]

        for (int i = 0; i < features.length(); i++) {
            String type = features.get(i).type
            String uniqueName = features.get(i).uniquename
            Feature feature = Feature.findByUniqueName(uniqueName)
            checkOwnersDelete(feature, inputObject)
            FeatureEvent currentFeatureEvent = featureEventService.findCurrentFeatureEvent(feature.uniqueName).get(0)
            JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
            JSONObject originalFeatureJsonObject = JSON.parse(currentFeatureEvent.newFeaturesJsonArray) as JSONObject
            String originalType = feature.cvTerm

            if (originalType == type) {
                log.warn "Cannot change ${uniqueName} from ${originalType} -> ${type}. Nothing to do."
            } else if (originalType in singletonFeatureTypes && type in rnaFeatureTypes) {
                log.error "Not enough information available to change ${uniqueName} from ${originalType} -> ${type}."
            } else {
                log.info "Changing ${uniqueName} from ${originalType} to ${type}"
                Feature newFeature = featureService.changeAnnotationType(inputObject, feature, sequence, user, type)
                JSONObject newFeatureJsonObject = featureService.convertFeatureToJSON(newFeature)
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
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.DELETE
                )
                fireAnnotationEvent(deleteAnnotationEvent)

                JSONObject addFeatureContainer = createJSONFeatureContainer()
                addFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeatureJsonObject)
                AnnotationEvent addAnnotationEvent = new AnnotationEvent(
                        features: addFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.ADD
                )
                fireAnnotationEvent(addAnnotationEvent)
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(newFeatureJsonObject)
            }
        }

        return featureContainer
    }

    def associateTranscriptToGene(JSONObject inputObject) {
        log.debug "associateTranscriptToGene: ${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.get(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        User user = permissionService.getCurrentUser(inputObject)
        String transcriptUniqueName = featuresArray.getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)
        String geneUniqueName = featuresArray.getJSONObject(1).get(FeatureStringEnum.UNIQUENAME.value)
        Transcript transcript = Transcript.findByUniqueName(transcriptUniqueName)
        Gene gene = Gene.findByUniqueName(geneUniqueName)
        log.debug "transcript: ${transcript}"
        log.debug "gene: ${gene}"

        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(transcript)
        transcript = featureService.associateTranscriptToGene(transcript, gene)
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(transcript)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        JSONArray newFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        newFeaturesJsonArray.add(currentFeatureJsonObject)
        featureEventService.addNewFeatureEvent(FeatureOperation.ASSOCIATE_TRANSCRIPT_TO_GENE, transcript.name,
                transcriptUniqueName, inputObject, oldFeaturesJsonArray, newFeaturesJsonArray, user)
        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(currentFeatureJsonObject)

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        log.debug "Return object: ${updateFeatureContainer.toString()}"
        return updateFeatureContainer
    }

    def dissociateTranscriptFromGene(JSONObject inputObject) {
        log.debug "dissociateTranscriptFromGene: ${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.get(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        User user = permissionService.getCurrentUser(inputObject)
        String uniqueName = featuresArray.getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        Transcript transcript = Transcript.findByUniqueName(uniqueName)
        log.debug "transcript: ${transcript}"

        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(transcript)
        transcript = featureService.dissociateTranscriptFromGene(transcript)
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(transcript)

        JSONObject jsonObject = featureService.convertFeatureToJSON(transcriptService.getGene(transcript))
        JSONObject mrnaObject = jsonObject.getJSONArray(FeatureStringEnum.CHILDREN.value).getJSONObject(0)
        JSONObject parentObject = new JSONObject()
        jsonObject.keySet().each { key ->
            if (key != FeatureStringEnum.CHILDREN.value) {
                parentObject.put(key, jsonObject.get(key))
            }
        }
        mrnaObject.put(FeatureStringEnum.PARENT.value, parentObject)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        JSONArray newFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        newFeaturesJsonArray.add(mrnaObject)
        featureEventService.addNewFeatureEvent(FeatureOperation.DISSOCIATE_TRANSCRIPT_FROM_GENE, transcript.name,
                uniqueName, inputObject, oldFeaturesJsonArray, newFeaturesJsonArray, user)

        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(currentFeatureJsonObject)

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        log.debug "Return object: ${updateFeatureContainer.toString()}"
        return updateFeatureContainer
    }

    def addVariant(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject returnObject = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)
        boolean suppressHistory = false
        boolean suppressEvents = false

        if (inputObject.has(FeatureStringEnum.SUPPRESS_HISTORY.value)) {
            suppressHistory = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_HISTORY.value)
        }
        if (inputObject.has(FeatureStringEnum.SUPPRESS_EVENTS.value)) {
            suppressEvents = inputObject.getBoolean(FeatureStringEnum.SUPPRESS_EVENTS.value)
        }

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i)
            jsonFeature = permissionService.copyRequestValues(inputObject, jsonFeature)
            SequenceAlteration variant = variantService.createVariant(jsonFeature, sequence, suppressHistory)

            if (variant.fmin < 0 || variant.fmax < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

            if (!suppressHistory) {
                // TODO: History tracking
            }
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(variant, true))
        }

        if (!suppressEvents) {
            AnnotationEvent addAnnotationEvent = new AnnotationEvent(
                    features: returnObject,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.ADD,
                    sequenceAlterationEvent: false
            )
            fireAnnotationEvent(addAnnotationEvent)
        }

        return returnObject
    }

    def addAlternateAlleles(JSONObject inputObject) {
        println "@addAlternateAlleles: ${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature feature = variantService.addAlternateAlleles(jsonFeature)
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.ADD
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def deleteAlternateAlleles(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature feature = variantService.deleteAlternateAlleles(jsonFeature)
            updateFeatureContainerdeleteAlternateAlleles = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def updateAlternateAlleles(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            Feature feature = variantService.updateAlternateAlleles(jsonFeature)
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def addVariantInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature feature = variantService.addVariantInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def deleteVariantInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); i++) {
            JSONObject jsonFeature = features.getJSONObject(i)
            Feature feature = variantService.deleteVariantInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def updateVariantInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); i++) {
            JSONObject jsonFeature = features.getJSONObject(i)
            Feature feature = variantService.updateVariantInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def addAlleleInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature feature = variantService.addAlleleInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def deleteAlleleInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); i++) {
            JSONObject jsonFeature = features.getJSONObject(i)
            Feature feature = variantService.deleteAlleleInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }

    def updateAlleleInfo(JSONObject inputObject) {
        log.debug "${inputObject.toString()}"
        JSONObject updateFeatureContainer = createJSONFeatureContainer()
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = permissionService.checkPermissions(inputObject, PermissionEnum.WRITE)

        for (int i = 0; i < features.length(); i++) {
            JSONObject jsonFeature = features.getJSONObject(i)
            Feature feature = variantService.updateAlleleInfo(jsonFeature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature))
        }

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer,
                    sequence: sequence,
                    operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer
    }
}
