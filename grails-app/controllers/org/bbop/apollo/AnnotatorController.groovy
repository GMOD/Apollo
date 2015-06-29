package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotatorController {

    def featureService
    def requestHandlingService
    def permissionService
    def annotatorService
    def preferenceService

    /**
     * Loads the shared link and moves over:
     * http://localhost:8080/apollo/annotator/loadLink?loc=chrII:302089..337445&organism=23357&highlight=0&tracklist=0&tracks=Reference%20sequence,User-created%20Annotations
     * @return
     */
    def loadLink(){
        try {
            Organism organism = Organism.findById(params.organism as Long)
            log.debug "loading organism: ${organism}"
            preferenceService.setCurrentOrganism(permissionService.currentUser,organism)
            if(params.loc) {
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

                preferenceService.setCurrentSequenceLocation(sequence.name,fmin,fmax)
            }

        } catch (e) {
            log.error "problem parsing the string ${e}"
        }

        redirect uri: "/annotator/index"
    }

    def index() {
        log.debug "loading the index"
        String uuid = UUID.randomUUID().toString()
        Organism.all.each {
            log.info it.commonName
        }
        [userKey: uuid]
    }


    def adminPanel(){
        if(permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)){
            def administativePanel = grailsApplication.config.apollo.administrativePanel
            [links:administativePanel]
        }
        else{
            render text:"Unauthorized"
        }
    }

    /**
     * updates shallow properties of gene / feature
     * @return
     */
    @Transactional
    def updateFeature() {
        log.debug "updateFeature ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        Feature feature = Feature.findByUniqueName(data.uniquename)

        feature.name = data.name
        feature.symbol = data.symbol
        feature.description = data.description

        feature.save(flush: true, failOnError: true)

        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        if (feature instanceof Gene) {
            List<Feature> childFeatures = feature.parentFeatureRelationships*.childFeature
            for (childFeature in childFeatures) {
                JSONObject jsonFeature = featureService.convertFeatureToJSON(childFeature, false)
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
            }
        } else {
            JSONObject jsonFeature = featureService.convertFeatureToJSON(feature, false)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
        }

        Sequence sequence = feature?.featureLocation?.sequence

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
                , sequenceAlterationEvent: false
        )
        requestHandlingService.fireAnnotationEvent(annotationEvent)

        render updateFeatureContainer
    }


    def updateFeatureLocation() {
        log.info "updateFeatureLocation ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        Feature exon = Feature.findByUniqueName(data.uniquename)
        exon.featureLocation.fmin = data.fmin
        exon.featureLocation.fmax = data.fmax
        exon.featureLocation.strand = data.strand
        exon.save(flush: true, failOnError: true)

        // need to grant the parent feature to force a redraw
        Feature parentFeature = exon.childFeatureRelationships*.parentFeature.first()

        JSONObject jsonFeature = featureService.convertFeatureToJSON(parentFeature, false)
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)

        Sequence sequence = exon?.featureLocation?.sequence
        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
                , sequenceAlterationEvent: false
        )
        requestHandlingService.fireAnnotationEvent(annotationEvent)

        render updateFeatureContainer
    }

    private JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    def findAnnotationsForSequence(String sequenceName, String request) {
        try {
            JSONObject returnObject = createJSONFeatureContainer()
            if (sequenceName && !Sequence.countByName(sequenceName)) return

            if (sequenceName) {
                returnObject.track = sequenceName
            }

            Sequence sequence
            Organism organism
            if (returnObject.has("track")) {
                sequence = permissionService.checkPermissions(returnObject, PermissionEnum.READ)
                organism = sequence.organism
            } else {
                organism = permissionService.checkPermissionsForOrganism(returnObject, PermissionEnum.READ)
            }
            // find all features for current organism

            Integer index = Integer.parseInt(request)

            List<Feature> allFeatures
            if (organism) {
                if (!sequence) {
                    try {
                        final long start = System.currentTimeMillis();

                        allFeatures = Feature.executeQuery("select distinct f from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)", [organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])
                        final long durationInMilliseconds = System.currentTimeMillis()-start;

                        log.debug "selecting features all ${durationInMilliseconds}"
                    } catch (e) {
                        allFeatures = new ArrayList<>()
                        log.error(e)
                    }
                } else {
                    final long start = System.currentTimeMillis();

                    allFeatures = Feature.executeQuery("select distinct f from Feature f left join f.parentFeatureRelationships pfr join f.featureLocations fl join fl.sequence s join s.organism o where s.name = :sequenceName and f.childFeatureRelationships is empty  and o = :organism  and f.class in (:viewableTypes)", [sequenceName: sequenceName, organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])
                    final long durationInMilliseconds = System.currentTimeMillis()-start;

                    log.debug "selecting features ${durationInMilliseconds}"
                }

                for (Feature feature in allFeatures) {
                    returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false));
                }
            }

            returnObject.put(FeatureStringEnum.REQUEST_INDEX.getValue(), index + 1)

            // TODO: do checks here
            render returnObject
        }
        catch(Exception e) {
            def error=[error: e.message]
            log.error e.message
            e.printStackTrace()
            render e as JSON
        }

    }

    def version() {}

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def getAppState() {
        render annotatorService.getAppState() as JSON
    }

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def setCurrentOrganism(Organism organismInstance) {
        // set the current organism
        preferenceService.setCurrentOrganism(permissionService.currentUser, organismInstance)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organismInstance.directory)
        render annotatorService.getAppState() as JSON
    }

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def setCurrentSequence(Sequence sequenceInstance) {
        // set the current organism and sequence Id (if both)
        preferenceService.setCurrentSequence(permissionService.currentUser, sequenceInstance)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, sequenceInstance.organism.directory)

        render annotatorService.getAppState() as JSON
    }

    def notAuthorized(){
        log.error "not authorized"
    }

}
