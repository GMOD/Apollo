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

    def index() {
        String uuid = UUID.randomUUID().toString()
        Organism.all.each {
            log.info it.commonName
        }
        [userKey:uuid]
    }
    /**
     * updates shallow properties of gene / feature
     * @return
     */
    @Transactional
    def updateFeature() {
        log.info "updating feature ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        log.info "uqnieuname 2: ${data.uniquename}"
        log.info "rendered data ${data as JSON}"
        Feature feature = Feature.findByUniqueName(data.uniquename)
        log.info "foiund feature: "+feature

        feature.name = data.name
        feature.symbol = data.symbol
        feature.description = data.description

        feature.save(flush: true, failOnError: true)

        log.info "saved!! "

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
        log.info "updating exon ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        log.info "uqnieuname 2: ${data.uniquename}"
        log.info "rendered data ${data as JSON}"
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

    def findAnnotationsForSequence(String sequenceName,String request) {
        JSONObject returnObject = createJSONFeatureContainer()
        if(sequenceName && !Sequence.countByName(sequenceName)) return

        if(sequenceName){
            returnObject.track = sequenceName
        }

        Sequence sequence
        Organism organism
        if(returnObject.has("track")){
            sequence = permissionService.checkPermissions(returnObject,PermissionEnum.READ)
            organism = sequence.organism
        }
        else{
            organism = permissionService.checkPermissionsForOrganism(returnObject,PermissionEnum.READ)
        }
        // find all features for current organism

        Integer index = Integer.parseInt(request)

        // TODO: should only be returning the top-level features
        List<Feature> allFeatures
        if(!sequence){
            try {
                allFeatures = Feature.executeQuery("select distinct f from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)",[organism:organism,viewableTypes:requestHandlingService.viewableAnnotationList])
            } catch (e) {
                allFeatures = new ArrayList<>()
                log.error(e)
            }
        }
        else{
            allFeatures = Feature.executeQuery("select distinct f from Feature f left join f.parentFeatureRelationships pfr join f.featureLocations fl join fl.sequence s join s.organism o where s.name = :sequenceName and f.childFeatureRelationships is empty  and o = :organism  and f.class in (:viewableTypes)",[sequenceName: sequenceName,organism:organism,viewableTypes:requestHandlingService.viewableAnnotationList])
        }

        for (Feature feature in allFeatures) {
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false));
        }

        returnObject.put(FeatureStringEnum.REQUEST_INDEX.getValue(),index+1)

        // TODO: do checks here
        render returnObject

    }

    def version(){ }

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def getAppState(){
        render annotatorService.getAppState() as JSON
    }



    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def setCurrentOrganism(Organism organismInstance){
        // set the current organism
        preferenceService.setCurrentOrganism(permissionService.currentUser,organismInstance)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organismInstance.directory)
        render annotatorService.getAppState() as JSON
    }

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    @Transactional
    def setCurrentSequence(Sequence sequenceInstance){
        // set the current organism and sequence Id (if both)
        preferenceService.setCurrentSequence(permissionService.currentUser,sequenceInstance)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,sequenceInstance.organism.directory)

        render annotatorService.getAppState() as JSON
    }

}
