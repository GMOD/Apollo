package org.bbop.apollo

import grails.converters.JSON

//import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * This class is responsible for handling JSON requests from the AnnotationEditorController and routing
 * to the proper service classes.
 *
 * Its goal is to replace a a lot of the layers in AnnotationEditorController
 */
//@GrailsCompileStatic
@Transactional
//class RequestHandlingService implements  AnnotationListener{
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
    def overlapperService


    def brokerMessagingTemplate

    // TODO: make a grails singleton
//    DataListenerHandler dataListenerHandler = DataListenerHandler.getInstance()

//    public RequestHandlingService(){
//        dataListenerHandler.addDataStoreChangeListener(this);
//    }

    private String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }


    JSONObject setSymbol(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();

        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Sequence sequence = null

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String symbolString = jsonFeature.getString(FeatureStringEnum.SYMBOL.value);
            if (!sequence) sequence = feature.getFeatureLocation().getSequence()
//            Symbol symbol = feature.symbol
//            if (!symbol) {
//                symbol = new Symbol(
//                        value: symbolString
//                        , feature: feature
//                ).save()
//            } else {
//                symbol.value = symbolString
//                symbol.save()
//            }

            feature.symbol = symbolString
            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
//            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
        }


        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }
    }

    JSONObject setDescription(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Sequence sequence = null

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            String descriptionString = jsonFeature.getString(FeatureStringEnum.DESCRIPTION.value);
            if (!sequence) sequence = feature.getFeatureLocation().getSequence()

//            Description description = feature.description
//            if (!description) {
//                description = new Description(
//                        value: descriptionString
//                        , feature: feature
//                ).save()
//            } else {
//                description.value = descriptionString
//                description.save()
//            }

            feature.description = descriptionString
            feature.save(flush: true, failOnError: true)

            // TODO: need to fire
//            updateFeatureContainer = wrapFeature(updateFeatureContainer,feature)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
        }
//        if (sequence) {
//            AnnotationEvent annotationEvent = new AnnotationEvent(
//                    features: updateFeatureContainer
//                    , sequence: sequence
//                    , operation: AnnotationEvent.Operation.UPDATE
//            )
//            fireAnnotationEvent(annotationEvent)
//        }

        return updateFeatureContainer
    }

    private JSONObject wrapFeature(JSONObject jsonObject, Feature feature) {

        // only pass in transcript
        if (feature instanceof Gene) {
            feature.parentFeatureRelationships.childFeature.each { childFeature ->
                jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(childFeature));
            }
        } else {
            jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
        }
    }

    def deleteNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

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
                        DBXref.deleteAll(dbXref)
                        feature.save(failOnError: true)
                    }
                }
            }

            feature.save(flush: true, failOnError: true)
            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.DELETE
            )
            fireAnnotationEvent(annotationEvent)
        }

        return updateFeatureContainer

    }

    /**
     *{ "track": "Annotations-GroupUn4254", "features": [ { "uniquename": "19c39835-d10c-4ed3-a90c-c6608a49d5af", "old_dbxrefs": [ { "db": "aaa", "accession": "111" } ], "new_dbxrefs": [ { "db": "mmmm", "accession": "111" } ] } ], "operation": "update_non_primary_dbxrefs" }* @param inputObject
     * @return
     */
    def updateNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

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

//            DB newDB = DB.findOrSaveByName(newDbXrefJSONObject.getString(FeatureStringEnum.DB.value))
            oldDbXref.db = DB.findOrSaveByName(newDbXrefJSONObject.getString(FeatureStringEnum.DB.value))
            oldDbXref.accession = newDbXrefJSONObject.getString(FeatureStringEnum.ACCESSION.value)

            oldDbXref.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
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

    def addNonPrimaryDbxrefs(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            log.debug "feature: ${jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)}"
            JSONArray dbXrefJSONArray = jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)

            for (int j = 0; j < dbXrefJSONArray.size(); j++) {
                JSONObject dbXfrefJsonObject = dbXrefJSONArray.getJSONObject(j)
                log.debug "innerArray ${j}: ${dbXfrefJsonObject}"
//                for(int k = 0 ; k < innerArray.size(); k++){
//                    String jsonString = innerArray.getString(k)
//                println "string ${k} ${jsonString}"
                String dbString = dbXfrefJsonObject.getString(FeatureStringEnum.DB.value)
                log.debug "dbString: ${dbString}"
                String accessionString = dbXfrefJsonObject.getString(FeatureStringEnum.ACCESSION.value)
                log.debug "accessionString : ${accessionString}"
                DB db = DB.findByName(dbString)
                if (!db) {
                    db = new DB(name: dbString).save()
                }
                DBXref dbXref = DBXref.findOrSaveByAccessionAndDb(accessionString, db)
                dbXref.save(flush: true)

                feature.addToFeatureDBXrefs(dbXref)
                feature.save()
//                }

            }


            feature.save(flush: true, failOnError: true)

            updateFeatureContainer = wrapFeature(updateFeatureContainer, feature)
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
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

        Sequence sequence = null

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


    JSONObject getFeatures(JSONObject returnObject) {
        String trackName = fixTrackHeader(returnObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        Set<Feature> featureSet = new HashSet<>()

        /**
         * TODO: this should be one single query
         */
        // 1. - handle genes
        List<Gene> topLevelGenes = Gene.executeQuery("select f from Gene f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        for (Gene gene : topLevelGenes) {
            for (Transcript transcript : transcriptService.getTranscripts(gene)) {
                log.debug "Getting transcript ${transcript.uniqueName} for gene ${gene.uniqueName} "
//                    jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
                featureSet.add(transcript)
//                    jsonFeatures.put(transcript as JSON);
            }
        }

        // 1b. - handle psuedogenes
        List<Pseudogene> listOfPseudogenes = Pseudogene.executeQuery("select f from Pseudogene f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        for (Gene gene : listOfPseudogenes) {
            for (Transcript transcript : transcriptService.getTranscripts(gene)) {
                log.debug " getting transcript ${transcript.uniqueName} for gene ${gene.uniqueName} "
                featureSet.add(transcript)
            }
        }

        // 2. - handle transcripts
        List<Transcript> topLevelTranscripts = Transcript.executeQuery("select f from Transcript f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        log.debug "# of top level features ${topLevelTranscripts.size()}"
        for (Transcript transcript1 in topLevelTranscripts) {
            featureSet.add(transcript1)
        }

        log.debug "feature set size: ${featureSet.size()}"

        JSONArray jsonFeatures = new JSONArray()
        featureSet.each { feature ->
            JSONObject jsonObject = featureService.convertFeatureToJSON(feature, false)
            jsonFeatures.put(jsonObject)
        }

        returnObject.put(AnnotationEditorController.REST_FEATURES, jsonFeatures)

        fireAnnotationEvent(new AnnotationEvent(
                features: returnObject
                , operation: AnnotationEvent.Operation.ADD
                , sequence: sequence
        ))


        return returnObject

    }

    /**
     * First feature is transcript . . . all the first must be exons to add
     * @param inputObject
     * @return
     * TODO: test in interface
     */
    JSONObject addExon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String uniqueName = features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value);

        Transcript transcript = Transcript.findByUniqueName(uniqueName)
        Sequence sequence = transcript.featureLocation.sequence

        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i);
            // could be that this is null
//            Feature gsolExon = featureService.convertJSONToFeature(jsonExon,transcript,sequence)
            Exon gsolExon = (Exon) featureService.convertJSONToFeature(jsonExon, sequence)

//            featureService.updateNewGsolFeatureAttributes(gsolExon, transcript);
            featureService.updateNewGsolFeatureAttributes(gsolExon, sequence);

            if (gsolExon.getFmin() < 0 || gsolExon.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

            transcriptService.addExon(transcript, gsolExon)

            featureService.calculateCDS(transcript)

            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

            gsolExon.save()
        }

        transcript.save(flush: true)
//        featureService.getTopLevelFeature(transcript)?.save(flush: true)
        transcript.attach()
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
        transcript.save(flush: true)
//        transcript.attach()

        // TODO: one of these two versions . . .
        JSONObject returnObject = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false))
//        JSONObject returnObject = featureService.convertFeatureToJSON(transcript,false)
//

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject

    }

    JSONObject addTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        JSONObject returnObject = createJSONFeatureContainer()

        log.info "RHS::adding transcript return object ${inputObject?.size()}"
        String trackName = fixTrackHeader(inputObject.track)
        log.info "final trackNAme [${trackName}]"
        Sequence sequence = Sequence.findByName(trackName)
        log.info "sequences avaialble ${Sequence.count} -> ${Sequence.first()?.name}"
        log.info "sequence ${sequence}"
        log.info "RHS::PRE featuresArray ${featuresArray?.size()}"

        List<Transcript> transcriptList = new ArrayList<>()
        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonTranscript = featuresArray.getJSONObject(i)
            Transcript transcript = featureService.generateTranscript(jsonTranscript, trackName)

            // should automatically write to history
            transcript.save(flush: true)
            transcriptList.add(transcript)
        }
        
//        sequence.save(flush: true)
        // do I need to put it back in?
        transcriptList.each { transcript ->
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, false));
        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject

    }

    /**
     * Transcript is the first object
     * @param inputObject
     */
    JSONObject setTranslationStart(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

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
//        out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false));
//        fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);

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
    JSONObject setTranslationEnd(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        boolean setStart = transcriptJSONObject.has(FeatureStringEnum.LOCATION.value);
        if (!setStart) {
            CDS cds = transcriptService.getCDS(transcript)
            cdsService.setManuallySetTranslationEnd(cds, false)
            featureService.calculateCDS(transcript)
        } else {
            JSONObject jsonCDSLocation = transcriptJSONObject.getJSONObject(FeatureStringEnum.LOCATION.value);
            featureService.setTranslationStart(transcript, jsonCDSLocation.getInt(FeatureStringEnum.FMAX.value), true)
        }
        transcript.save()
//        out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false));
//        fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);

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

    def setReadthroughStopCodon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        boolean readThroughStopCodon = transcriptJSONObject.getBoolean(FeatureStringEnum.READTHROUGH_STOP_CODON.value);
        featureService.calculateCDS(transcript, readThroughStopCodon);

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        transcript.save(flush: true)

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript, false));

        if (sequence) {
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: featureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            fireAnnotationEvent(annotationEvent)
        }

        JSONObject returnObject = createJSONFeatureContainer(featureService.convertFeatureToJSON(featureService.getTopLevelFeature(transcript), false));

        return returnObject
    }

    def setAcceptor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray transcriptArray = new JSONArray()
        featureContainer.put(FeatureStringEnum.FEATURES.value, transcriptArray)

        Transcript.withNewSession {
            for (int i = 0; i < features.length(); ++i) {
                String uniqueName = features.getJSONObject(i).getString(FeatureStringEnum.UNIQUENAME.value);
                Exon exon = Exon.findByName(uniqueName)
                Transcript transcript = exonService.getTranscript(exon)

//            editor.setToDownstreamDonor(exon);
                if (upstreamDonor) {
                    exonService.setToUpstreamAcceptor(exon)
                } else {
                    exonService.setToDownstreamAcceptor(exon)
                }


                featureService.calculateCDS(transcript)

                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
//            findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);

                transcript.save()

                transcriptArray.add(featureService.convertFeatureToJSON(transcript))
            }
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


    def setDonor(JSONObject inputObject, boolean upstreamDonor) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray transcriptArray = new JSONArray()
        featureContainer.put(FeatureStringEnum.FEATURES.value, transcriptArray)

        Transcript.withNewSession {
            for (int i = 0; i < features.length(); ++i) {
                String uniqueName = features.getJSONObject(i).getString(FeatureStringEnum.UNIQUENAME.value);
                Exon exon = Exon.findByName(uniqueName)
                Transcript transcript = exonService.getTranscript(exon)
//            editor.setToDownstreamDonor(exon);
                if (upstreamDonor) {
                    exonService.setToUpstreamDonor(exon)
                } else {
                    exonService.setToDownstreamDonor(exon)
                }


                featureService.calculateCDS(transcript)

                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
//            findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);

                transcript.save()

                transcriptArray.add(featureService.convertFeatureToJSON(transcript))
            }
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

    JSONObject setLongestOrf(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject transcriptJSONObject = features.getJSONObject(0);

        Transcript transcript = Transcript.findByUniqueName(transcriptJSONObject.getString(FeatureStringEnum.UNIQUENAME.value))
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        featureService.setLongestORF(transcript, false)

        transcript.save(flush: true, insert: false)

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
    JSONObject setExonBoundaries(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        JSONObject returnObject = createJSONFeatureContainer()

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            if (!jsonFeature.has(FeatureStringEnum.LOCATION.value)) {
                continue;
            }
            JSONObject jsonLocation = jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value);
            int fmin = jsonLocation.getInt(FeatureStringEnum.FMIN.value);
            int fmax = jsonLocation.getInt(FeatureStringEnum.FMAX.value);
            if (fmin < 0 || fmax < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }
//            Exon exon = (Exon) editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
            Exon exon = Exon.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
            Transcript transcript = exonService.getTranscript(exon)
            
            
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


            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript));

        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)


        return returnObject
    }

    JSONObject setBoundaries(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        JSONObject returnObject = createJSONFeatureContainerFromFeatures()

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            if (!jsonFeature.has(FeatureStringEnum.LOCATION.value)) {
                continue;
            }
            JSONObject jsonLocation = jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value);
            int fmin = jsonLocation.getInt(FeatureStringEnum.FMIN.value);
            int fmax = jsonLocation.getInt(FeatureStringEnum.FMAX.value);
            if (fmin < 0 || fmax < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
//            editor.setBoundaries(feature, fmin, fmax);
            FeatureLocation featureLocation = FeatureLocation.findByFeature(feature)

            featureLocation.fmin = fmin
            featureLocation.fmax = fmax
            feature.save()

            returnObject.getJSONArray("features").put(featureService.convertFeatureToJSON(feature));
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
        handleChangeEvent(annotationEvents)
    }

    public void sendAnnotationEvent(String returnString) {
        log.debug "RHS::return operations sent . . ${returnString?.size()}"
//        println "returnString ${returnString}"
        if (returnString.startsWith("[")) {
            returnString = returnString.substring(1, returnString.length() - 1)
        }
        try {
            brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification", returnString
        } catch (e) {
            log.error("problem sending message: ${e}")
        }
    }

    synchronized void handleChangeEvent(AnnotationEvent... events) {
//        println "handingling event ${events.length}"
        if (events.length == 0) {
            return;
        }
//        println "handling first event ${events[0] as JSON}"
        JSONArray operations = new JSONArray();
        for (AnnotationEvent event : events) {
            JSONObject features = event.getFeatures();
            try {
                features.put(AnnotationEditorController.REST_OPERATION, event.getOperation().name());
                features.put(REST_SEQUENCE_ALTERNATION_EVENT, event.isSequenceAlterationEvent());
                operations.put(features);
            }
            catch (JSONException e) {
                log.error("error handling change event ${event}: ${e}")
            }
        }

        sendAnnotationEvent(operations.toString())

    }

    private JSONObject createJSONFeatureContainerFromFeatures(Feature... features) throws JSONException {
        def jsonObjects = new ArrayList()
        for (Feature feature in features) {
            JSONObject featureObject = featureService.convertFeatureToJSON(feature)
            jsonObjects.add(featureObject)
        }
        return createJSONFeatureContainer(jsonObjects as JSONObject[])
    }

    JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    private static String fixTrackHeader(String trackInput) {
        return !trackInput.startsWith("Annotations-") ? trackInput : trackInput.substring("Annotations-".size())
    }



    JSONObject deleteSequenceAlteration(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONObject deleteFeatureContainer = createJSONFeatureContainer();

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            SequenceAlteration sequenceAlteration = SequenceAlteration.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
//            SequenceAlteration sequenceAlteration = (SequenceAlteration) getFeature(editor, features.getJSONObject(i));

//            editor.deleteSequenceAlteration(sequenceAlteration);
            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        featureService.setLongestORF(transcript)
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript));
                    }
                    feature.save()
                }
            }
            FeatureLocation.deleteAll(sequenceAlteration.featureLocations)
            sequenceAlteration.delete()
            deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration));
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
                , sequenceAlterationEvent: true
        )
        fireAnnotationEvent(deleteAnnotationEvent)
        fireAnnotationEvent(updateAnnotationEvent)

        return createJSONFeatureContainer()
    }

//    { "track": "Annotations-GroupUn4157", "features": [ { "location": { "fmin": 1284, "fmax": 1284, "strand": 1 }, "type": {"name": "insertion", "cv": { "name":"sequence" } }, "residues": "ATATATA" } ], "operation": "add_sequence_alteration" }
    def addSequenceAlteration(JSONObject inputObject) {
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONObject addFeatureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
//            Feature gsolFeature = JSONUtil.convertJSONToFeature(features.getJSONObject(i), bioObjectConfiguration, trackToSourceFeature.get(track), new HttpSessionTimeStampNameAdapter(session, editor.getSession()));
//            updateNewGsolFeatureAttributes(gsolFeature, trackToSourceFeature.get(track));
            SequenceAlteration sequenceAlteration = (SequenceAlteration) featureService.convertJSONToFeature(jsonFeature, sequence)


            featureService.updateNewGsolFeatureAttributes(sequenceAlteration, sequence)

//            SequenceAlteration sequenceAlteration = (SequenceAlteration) BioObjectUtil.createBioObject(gsolFeature, bioObjectConfiguration);
            if (sequenceAlteration.getFmin() < 0 || sequenceAlteration.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }

//            setOwner(sequenceAlteration, (String) session.getAttribute("username"));
//            editor.addSequenceAlteration(sequenceAlteration);

            sequenceAlteration.save(insert: true, failOnError: true, flush: true)
//
//            if (dataStore != null) {
//                writeFeatureToStore(editor, dataStore, sequenceAlteration, track);
//            }
//            for (AbstractSingleLocationBioFeature feature : editor.getSession().getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
            for (Feature feature : featureService.getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
                if (feature instanceof Gene) {
                    for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                        featureService.setLongestORF(transcript)
//                        editor.setLongestORF(transcript);
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
//                        findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
//                        updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
                        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, false));
                    }
//                    if (dataStore != null) {
//                        writeFeatureToStore(editor, dataStore, feature, track);
//                    }
                }
            }
//            addFeatureContainer.getJSONArray("features").put(JSONUtil.convertFeatureToJSON(gsolFeature));
            addFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(sequenceAlteration));
        }


        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: addFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
                , sequenceAlterationEvent: true
        )

        fireAnnotationEvent(annotationEvent)

        return addFeatureContainer

    }

    def lockFeature(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject featureContainer = createJSONFeatureContainer();
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
//            if (!feature.getOwner().getOwner().equals(username) && (permission & Permission.ADMIN) == 0) {
//                throw new AnnotationEditorServiceException("Cannot lock someone else's annotation");
//            }
            if (FeatureProperty.findByFeatureAndValue(feature, FeatureStringEnum.LOCKED.value)) {
                log.error("Feature ${feature.name} already locked")
            } else {
                FeatureProperty featureProperty = new FeatureProperty(
                        value: FeatureStringEnum.LOCKED.value
                        , feature: feature
                ).save()
                feature.addToFeatureProperties(featureProperty)
                feature.save()
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
            }
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

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
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
//            if (!feature.getOwner().getOwner().equals(username) && (permission & Permission.ADMIN) == 0) {
//                throw new AnnotationEditorServiceException("Cannot lock someone else's annotation");
//            }
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

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def flipStrand(JSONObject inputObject) {
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))

            if (feature instanceof Transcript) {
                feature = transcriptService.flipTranscriptStrand((Transcript) feature);
            } else {
//                feature = featureService.flipFeatureStrand(feature);
                feature = featureService.flipStrand(feature)
            }
            featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false));
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def mergeExons(JSONObject inputObject) {

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Exon exon1 = (Exon) Exon.findByUniqueName(features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value));
        Exon exon2 = (Exon) Exon.findByUniqueName(features.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value));
        Transcript transcript1 = exonService.getTranscript(exon1)
//        Transcript oldTransript = transcript1.generateClone()
//        Transcript oldTranscript = cloneTranscript(transcript);
//        editor.mergeExons(exon1, exon2);
        exonService.mergeExons(exon1, exon2)
        featureService.calculateCDS(transcript1);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1);
        // rename?
//        updateTranscriptAttributes(exon1.getTranscript());

        transcript1.save(flush: true)
        exon1.save(flush: true)

        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript1))


        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def splitExon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonExon = features.getJSONObject(0)
        Exon exon = (Exon) Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value);
        Transcript transcript = exonService.getTranscript(exon)

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
        
        
        Exon splitExon = exonService.splitExon(exon, exonLocation.getInt(FeatureStringEnum.FMAX.value), exonLocation.getInt(FeatureStringEnum.FMIN.value))
        featureService.updateNewGsolFeatureAttributes(splitExon, sequence)
//        transcript.attach()
        featureService.calculateCDS(transcript)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

        exon.save()
        transcript.save(flush:true)
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript));


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
    def deleteExon(JSONObject inputObject) {
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonTranscript = features.getJSONObject(0)

        Transcript transcript = Transcript.findByUniqueName(jsonTranscript.getString(FeatureStringEnum.UNIQUENAME.value));
        for (int i = 1; i < features.length(); ++i) {
            JSONObject jsonExon = features.getJSONObject(i)
            Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value));

            exonService.deleteExon(transcript, exon);
            Exon.deleteAll(exon)
        }

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
        Feature topLevelFeature = featureService.getTopLevelFeature(transcript)
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topLevelFeature))

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.DELETE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer

//        if (dataStore != null) {
//            writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
//        }
//        out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());

    }

    def addFeature(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        JSONObject returnObject = createJSONFeatureContainer()

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            Feature newFeature = featureService.convertJSONToFeature(jsonFeature, sequence)
            featureService.updateNewGsolFeatureAttributes(newFeature, sequence)
            featureService.addFeature(newFeature)
            newFeature.save(insert: true, flush: true)

            if (newFeature instanceof Gene) {
                for (Transcript transcript : transcriptService.getTranscripts((Gene) newFeature)) {
                    if (!(newFeature instanceof Pseudogene) && transcriptService.isProteinCoding(transcript)) {
                        if (!configWrapperService.useCDS() || transcriptService.getCDS(transcript) == null) {
                            featureService.calculateCDS(transcript);
                        }
                    } else {
                        if (transcriptService.getCDS(transcript) != null) {
                            featureRelationshipService.deleteChildrenForTypes(transcript, CDS.ontologyId)
                        }
                    }
                    nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript);
                    transcript.name = nameService.generateUniqueName(transcript)
                    transcript.uniqueName = transcript.name

                    returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript));
                }
            } else {
                returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(newFeature));
            }
        }

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject
    }

    /**
     * TODO
     *  From AnnotationEditorService .. . deleteFeature 1 and 2
     */
//    { "track": "Annotations-Group1.3", "features": [ { "uniquename": "179e77b9-9329-4633-9f9e-888e3cf9b76a" } ], "operation": "delete_feature" }:
    def deleteFeature(JSONObject inputObject) {

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
        
        JSONObject featureContainer = createJSONFeatureContainer();
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        Map<String, List<Feature>> modifiedFeaturesUniqueNames = new HashMap<String, List<Feature>>();
        boolean isUpdateOperation = false

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)
            if (feature) {
                // is this a bug?
                isUpdateOperation = isUpdateOperation || featureService.deleteFeature(feature,modifiedFeaturesUniqueNames);
                List<Feature> modifiedFeaturesList = modifiedFeaturesUniqueNames.get(uniqueName)
                if (modifiedFeaturesList == null) {
                    modifiedFeaturesList = new ArrayList<>()
                }
                modifiedFeaturesList.add(feature)
                modifiedFeaturesUniqueNames.put(uniqueName,modifiedFeaturesList)
            }
        }

//        featureService.updateModifiedFeaturesAfterDelete(modifiedFeaturesUniqueNames, isUpdateOperation)
        for (Map.Entry<String, List<Feature>> entry : modifiedFeaturesUniqueNames.entrySet()) {
            String uniqueName = entry.getKey();
            // needed only for managing transaction history
//            List<Feature> deletedFeatures = entry.getValue();
            Feature feature = Feature.findByUniqueName(uniqueName);
            if (feature == null) {
                log.info("Feature already deleted");
                continue;
            }
//            SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
//            Feature gsolFeature = (Feature) iterator.next();
            if (!isUpdateOperation) {
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(new JSONObject().put(FeatureStringEnum.UNIQUENAME.value, uniqueName));
//
                if (feature instanceof Transcript) {
                    Transcript transcript = (Transcript) feature;
                    Gene gene = transcriptService.getGene(transcript)
//                    transcriptService.deleteTranscript(gene, transcript)
                    int numberTranscripts = transcriptService.getTranscripts(gene).size()
                    if (numberTranscripts == 1) {
//                        editor.deleteFeature(gene);
                        // wouldn't this be a gene?
                        Feature topLevelFeature = featureService.getTopLevelFeature(gene)
                        featureRelationshipService.deleteFeatureAndChildren(topLevelFeature)
//                        Feature.deleteAll(topLevelFeature)

                        AnnotationEvent annotationEvent = new AnnotationEvent(
                                features: featureContainer
                                , sequence: sequence
                                ,operation: AnnotationEvent.Operation.DELETE
                        )

                        fireAnnotationEvent(annotationEvent)
                    }
                    else{
                        featureRelationshipService.deleteFeatureAndChildren(transcript)
                        gene.save()
                        AnnotationEvent annotationEvent = new AnnotationEvent(
                                features: featureContainer
                                , sequence: sequence
                                ,operation: AnnotationEvent.Operation.UPDATE
                        )

                        fireAnnotationEvent(annotationEvent)
                    }

//                    if (numberTranscripts > 1) {
//                        gene.save()
//                    } else {
//                        featureService.deleteFeatureAndChildren(gene)
////                        gene.delete(flush: true )
////                        Gene.deleteAll(gene)
//                    }

                } else {
                    Feature topLevelFeature = featureService.getTopLevelFeature(feature)
                    featureRelationshipService.deleteFeatureAndChildren(topLevelFeature)
//                    topLevelFeature.delete(flush: true)
//                    Feature.deleteAll(topLevelFeature)

                    AnnotationEvent annotationEvent = new AnnotationEvent(
                            features: featureContainer
                            , sequence: sequence
                            ,operation: AnnotationEvent.Operation.DELETE
                    )

                    fireAnnotationEvent(annotationEvent)

//                    feature.delete(flush: true )
//                    Feature.deleteAll(feature)
//
                }
            } 
            else {
                if (feature instanceof Transcript) {
                    Transcript transcript = (Transcript) feature;
                    featureService.calculateCDS(transcript)
                    nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                    transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)
//                    operation = Transaction.Operation.DELETE_EXON;
                    Gene gene = transcriptService.getGene(transcript)
                    gene.save()
                } else {
//                    operation = Transaction.Operation.DELETE_FEATURE;
                    feature.save()
                }
                featureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature));
            }
        }



        AnnotationEvent finalAnnotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
        )

        finalAnnotationEvent.operation = isUpdateOperation ? AnnotationEvent.Operation.UPDATE : AnnotationEvent.Operation.DELETE
        fireAnnotationEvent(finalAnnotationEvent)

        return createJSONFeatureContainer()
    }

    def makeIntron(JSONObject inputObject) {
//        throw new RuntimeException("Implement make intron")
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//        Exon exon = (Exon) getFeature(editor, jsonExon);
        JSONObject jsonExon = featuresArray.getJSONObject(0)
        Exon exon = Exon.findByUniqueName(jsonExon.getString(FeatureStringEnum.UNIQUENAME.value))
//        Transcript oldTranscript = cloneTranscript(exon.getTranscript());
//        JSONObject exonLocation = jsonExon.getJSONObject("location");
        JSONObject exonLocation = jsonExon.getJSONObject(FeatureStringEnum.LOCATION.value)
//        Exon splitExon = editor.makeIntron(exon, exonLocation.getInt("fmin"), defaultMinimumIntronSize, nameAdapter.generateUniqueName());

        Exon splitExon = exonService.makeIntron(
                exon
                , exonLocation.getInt(FeatureStringEnum.FMIN.value)
                , configWrapperService.getDefaultMinimumIntronSize()
        )
        if (splitExon == null) {
            def returnContainer = createJSONFeatureContainer()
            returnContainer.put("alert", "Unable to find canonical splice sites.");
            return returnContainer
        }
//        updateNewGbolFeatureAttributes(splitExon, trackToSourceFeature.get(track));
        featureService.updateNewGsolFeatureAttributes(splitExon, sequence)
//        calculateCDS(editor, exon.getTranscript());
        Transcript transcript = exonService.getTranscript(exon)
        featureService.calculateCDS(transcript)
//        findNonCanonicalAcceptorDonorSpliceSites(editor, exon.getTranscript());
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)

//        updateTranscriptAttributes(exon.getTranscript());
        transcript.name = transcript.name ?: nameService.generateUniqueName(transcript)

        transcript.save(failOnError: true)
        exon.save(failOnError: true)
        splitExon.save(failOnError: true, flush: true)

//        JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(exon.getTranscript()));
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(transcript))
//        out.write(featureContainer.toString());
//        fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }

    def splitTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        Exon exon1 = Exon.findByUniqueName(featuresArray.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))
        Exon exon2 = Exon.findByUniqueName(featuresArray.getJSONObject(1).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript transcript1 = exonService.getTranscript(exon1)
        Transcript transcript2 = transcriptService.splitTranscript(transcript1, exon1, exon2)
        
        featureService.updateNewGsolFeatureAttributes(transcript2, sequence);
        
        featureService.calculateCDS(transcript1)
        featureService.calculateCDS(transcript2)

        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1);
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript2);
        
        transcript1.name = transcript1.name ?: nameService.generateUniqueName(transcript1)
        transcript2.name = transcript2.name ?: nameService.generateUniqueName(transcript2)
       
        transcript1.owners.each { transcript2.addToOwners(it)}

        
        Gene gene1 = transcriptService.getGene(transcript1)
//
        if (gene1) {
            Set<Transcript> gene1Transcripts = new HashSet<Transcript>();
            Set<Transcript> gene2Transcripts = new HashSet<Transcript>();

            // TODO:
//            List<Transcript> transcripts = BioObjectUtil.createSortedFeatureListByLocation(gene1.getTranscripts(), false);
            List<Transcript> transcripts = transcriptService.getTranscriptsSortedByFeatureLocation(gene1, false)
            gene1Transcripts.add(transcripts.get(0))

            // determine if transcripts belong on a new gene
            for (int i = 0; i < transcripts.size() - 1; ++i) {
                Transcript t1 = transcripts.get(i);
                for (int j = i + 1; j < transcripts.size(); ++j) {
                    Transcript t2 = transcripts.get(j);
                    if (gene1Transcripts.contains(t2) || gene2Transcripts.contains(t2)) {
                        continue;
                    }
                    if (t1.getFmin() < transcript2.featureLocation.getFmin()) {
                        if (overlapperService.overlaps(t1, t2)) {
                            gene1Transcripts.add(t2);
                        } else {
                            gene2Transcripts.add(t2);
                        }
                    } else {
                        gene2Transcripts.add(t2);
                    }
                }
                if (t1.featureLocation.getFmin() > transcript2.featureLocation.getFmin()) {
                    break;
                }
            }
//            /*
//            for (Transcript t : transcripts) {
//                if (t.getFmin() < splitTranscript.getFmin()) {
//                    if (overlapper.overlaps(t, splitTranscript)) {
//                        gene2Transcripts.add(t);
//                    }
//                    else {
//                        gene1Transcripts.add(t);
//                    }
//                }
//                else {
//                    gene2Transcripts.add(t);
//                }
//            }
//            */

            // we delete transcripts that belong on the other gene
//            for (Transcript t : gene2Transcripts) {
//                featureRelationshipService.removeFeatureRelationship(gene1,t)
////                transcriptService.deleteTranscript(gene1, t)
//            }
           
            gene1.featureLocation.fmax = exon1.featureLocation.fmax
            gene1.save(flush: true)

            // we add transcript 2 explicitly
            JSONObject addSplitTranscriptJSONObject = new JSONObject()
            JSONArray addTranscriptFeaturesArray = new JSONArray()
            transcript2.featureLocation.fmin = exon2.featureLocation.fmin
            JSONObject transcript2Object = featureService.convertFeatureToJSON(transcript2,false)
            transcript2Object.put(FeatureStringEnum.NAME.value,gene1.name)
            transcript2Object.remove(FeatureStringEnum.PARENT_ID.value)
            transcript2Object.remove(FeatureStringEnum.UNIQUENAME.value)
            println "transcript2Object ${transcript2Object as JSON}"
            addTranscriptFeaturesArray.add(transcript2Object)
            addSplitTranscriptJSONObject.put(FeatureStringEnum.FEATURES.value, addTranscriptFeaturesArray)
            addSplitTranscriptJSONObject.put("track", inputObject.track)

            // we delete transcripts that belong on the other gene
            for (Transcript t : gene2Transcripts) {
//                featureRelationshipService.removeFeatureRelationship(gene1,t)
                transcriptService.deleteTranscript(gene1, t)
            }
            println "NAME OF TRANSCRIPT 2: ${transcript2.name}"
            transcript2.parentFeatureRelationships.each{ it ->
                it.childFeature.delete()
//                featureRelationshipService.removeFeatureRelationship(transcript2)
            }
            transcript2.delete()
//            featureService.deleteFeature(transcript2)
//            transcriptService.deleteTranscript(gene1, transcript2)


            // we add any other transcripts to the correct gene
            for (Transcript t : gene2Transcripts) {
                if (!t.equals(transcript2)) {
                    JSONObject addTranscriptJSONObject = new JSONObject()
                    addTranscriptFeaturesArray = new JSONArray()
                    addTranscriptFeaturesArray.add(featureService.convertFeatureToJSON(t))
                    addTranscriptJSONObject.put(FeatureStringEnum.FEATURES.value, addTranscriptFeaturesArray)
                    addTranscriptJSONObject.put("track", inputObject.track)
                    addTranscript(addTranscriptJSONObject)
//                    addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(t), track, geneNameAdapter, gene1.isPseudogene());
                }
            }
            

            
//            transcript2.delete()
            addTranscript(addSplitTranscriptJSONObject)

//            splitTranscript = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(splitTranscript), track, geneNameAdapter, gene1.isPseudogene());
//
        }

        JSONObject updateContainer = createJSONFeatureContainer();
        List<Transcript> splitTranscriptSiblings = transcriptService.getTranscripts(transcriptService.getGene(transcript2))
        for (Transcript t : splitTranscriptSiblings) {
            if (!t.getUniqueName().equals(transcript2.getUniqueName())) {
                updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t));
            }
        }

        transcript1.save(flush: true)
//        transcript2.delete(flush: True)
        
//        Transcript exon1Transcript = exonService.getTranscript(exon1)
        Feature topLevelExonFeature = featureService.getTopLevelFeature(transcript1)
        JSONObject returnContainer = createJSONFeatureContainerFromFeatures(topLevelExonFeature)
//        out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(exon1.getTranscript()))).toString());
//
//        JSONObject addContainer = createJSONFeatureContainerFromFeatures(transcript2);
//        JSONObject addContainer =createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(splitTranscript));
        
        List<Transcript> exon1Transcripts = transcriptService.getTranscripts(transcriptService.getGene(transcript1))
        for (Transcript t : exon1Transcripts) {
            updateContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(t));
        }

//
//        fireDataStoreChange(new DataStoreChangeEvent(this, updateContainer, track, DataStoreChangeEvent.Operation.UPDATE), new DataStoreChangeEvent(this, addContainer, track, DataStoreChangeEvent.Operation.ADD));
//        AnnotationEvent addAnnotationEvent = new AnnotationEvent(
//                features: addContainer
//                , sequence: sequence
//                , operation: AnnotationEvent.Operation.ADD
//        )

        AnnotationEvent updateAnnotationEvent = new AnnotationEvent(
                features: updateContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
        )

//        fireAnnotationEvent(addAnnotationEvent, updateAnnotationEvent)
        fireAnnotationEvent( updateAnnotationEvent)


        return returnContainer
    }

    def mergeTranscripts(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject jsonTranscript1 = featuresArray.get(0)
        JSONObject jsonTranscript2 = featuresArray.get(1)
        Transcript transcript1 = Transcript.findByUniqueName(jsonTranscript1.getString(FeatureStringEnum.UNIQUENAME.value))
        Transcript transcript2 = Transcript.findByUniqueName(jsonTranscript2.getString(FeatureStringEnum.UNIQUENAME.value))
//        // cannot merge transcripts from different strands
        if (!transcript1.getStrand().equals(transcript2.getStrand())) {
            throw new AnnotationException("You cannot merge transcripts on opposite strands");
        }
        Gene gene2 = transcriptService.getGene(transcript2)

//        editor.mergeTranscripts(transcript1, transcript2);
        transcriptService.mergeTranscripts(transcript1, transcript2)

        featureService.calculateCDS(transcript1)
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript1)

        Gene gene1 = transcriptService.getGene(transcript1)

        if (gene1 != gene2) {
            Gene.deleteAll(gene2)
        }


        transcript1.name = transcript1.name ?: nameService.generateUniqueName(transcript1)
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        JSONObject deleteFeatureContainer = createJSONFeatureContainer();

        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

//        out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript1))).toString());
        JSONObject returnObject = createJSONFeatureContainerFromFeatures(featureService.getTopLevelFeature(transcript1))

        List<Transcript> gene1Transcripts = transcriptService.getTranscripts(gene1)
        for (Transcript transcript : gene1Transcripts) {
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript));
        }
        deleteFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript2));

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

        fireAnnotationEvent(deleteAnnotationEvent, updateAnnotationEvent)

        return returnObject
    }

    def duplicateTranscript(JSONObject inputObject) {
        Transcript transcript = Transcript.findByUniqueName(inputObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value))

        Transcript duplicateTranscript = transcriptService.duplicateTranscript(transcript)
        duplicateTranscript.save()
        Feature topFeature = featureService.getTopLevelFeature(transcript)
        topFeature.save()
        JSONObject featureContainer = createJSONFeatureContainer(featureService.convertFeatureToJSON(topFeature))
//        out.write(featureContainer.toString());
//        fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
        String trackName = fixTrackHeader(inputObject.track)
        Sequence sequence = Sequence.findByName(trackName)

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: featureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
        )

        fireAnnotationEvent(annotationEvent)

        return featureContainer
    }
}
