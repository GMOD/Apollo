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
    def getAppState(){

        JSONObject appStateObject = new JSONObject()
        def organismList = permissionService.getOrganismsForCurrentUser()
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.currentUser, true)
        Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null


//        request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value) == organism.directory

        log.debug "organism list: ${organismList}"

        log.debug "finding all organisms: ${Organism.count}"

        JSONArray jsonArray = new JSONArray()
        for (def organism in organismList) {
            Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)",[organism:organism,viewableTypes:requestHandlingService.viewableAnnotationList])[0] as Integer
            JSONObject jsonObject = [
                    id             : organism.id,
                    commonName     : organism.commonName,
                    blatdb         : organism.blatdb,
                    directory      : organism.directory,
                    annotationCount: annotationCount,
                    sequences      : organism.sequences?.size(),
                    genus          : organism.genus,
                    species        : organism.species,
                    valid          : organism.valid,
                    currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false
            ] as JSONObject
            jsonArray.add(jsonObject)
        }
        appStateObject.put("organismList",jsonArray)
        appStateObject.put("organismList",jsonArray)


        render appStateObject as JSON
    }



    /**
     * TODO: return an AnnotatorStateInfo object
     */
    def setCurrentOrganism(Long organismId){
        // set the current organism

        getAppState()
    }

    /**
     * TODO: return an AnnotatorStateInfo object
     */
    def setCurrentOrganismAndSequence(Long organismId,Long sequenceId){
        // set the current organism and sequence Id (if both)

        getAppState()
    }

}
