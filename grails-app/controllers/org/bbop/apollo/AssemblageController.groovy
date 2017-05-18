package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject


@Transactional(readOnly = true)
class AssemblageController {

//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def preferenceService
    def projectionService
    def assemblageService
    def featureProjectionService

    @Transactional
    list() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)
        if (Organism.count > 0) {
            Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            render assemblageService.getAssemblagesForUserAndOrganism(user, currentOrganism).sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }

    }

    private static List<Feature> extractFeaturesFromRequest(JSONObject inputObject) {
        JSONArray featuresArray = JSON.parse(inputObject.getString(FeatureStringEnum.FEATURES.value)) as JSONArray
        println "featuresArray: ${featuresArray as JSON}"
        List<String> featureList = []
        for (JSONObject featureObject in featuresArray) {
            featureList.add(featureObject.getString(FeatureStringEnum.UNIQUENAME.value))
        }
        println "featuresList : ${featureList}"
        List<Feature> features = Feature.findAllByUniqueNameInList(featureList)
        println "features : ${features.name}"
        return features
    }

    @Transactional
    def projectFeatures() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        List<Feature> features = extractFeaturesFromRequest(inputObject)
        Assemblage assemblage = assemblageService.generateAssemblageForFeatureRegions(features)
        render assemblageService.convertAssemblageToJson(assemblage) as JSON
    }


    @Transactional
    def foldTranscripts() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        println "folding transcript ${inputObject as JSON}"
        List<Feature> features = extractFeaturesFromRequest(inputObject)

        // in the projection, add "collapsed=true" for the features in question and expand
        JSONObject projectionSequenceObject = inputObject.getJSONObject(FeatureStringEnum.SEQUENCE.value)
        println "proj sequence object ${projectionSequenceObject as JSON}"
        MultiSequenceProjection projection = projectionService.convertToProjectionFromJson(projectionSequenceObject)
        for (feature in features) {
            projection = featureProjectionService.addLocationsForFeature(feature, projection)
        }
        render projectionService.convertToJsonFromProjection(projection) as JSON
    }

    @Transactional
    def removeFolds() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        println "folding transcript ${inputObject as JSON}"
        List<Feature> features = extractFeaturesFromRequest(inputObject)

        // in the projection, add "collapsed=true" for the features in question and expand
        JSONObject projectionSequenceObject = inputObject.getJSONObject(FeatureStringEnum.SEQUENCE.value)
        println "proj sequence object ${projectionSequenceObject as JSON}"
        MultiSequenceProjection projection = projectionService.convertToProjectionFromJson(projectionSequenceObject)
        for (feature in features) {
            projection = featureProjectionService.clearLocationForCoordinateForFeature(projection, feature)
        }
        render projectionService.convertToJsonFromProjection(projection) as JSON
//        render getAssemblage() as JSON
    }

    @Transactional
    def foldBetweenExons() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        println "folding exons ${inputObject as JSON}"
        List<Feature> features = extractFeaturesFromRequest(inputObject)
        assert features.size() == 2
        assert features[0] instanceof Exon
        assert features[1] instanceof Exon

        // in the projection, add "collapsed=true" for the features in question and expand
        JSONObject projectionSequenceObject = inputObject.getJSONObject(FeatureStringEnum.SEQUENCE.value)
        println "proj sequence object ${projectionSequenceObject as JSON}"
        MultiSequenceProjection projection = projectionService.convertToProjectionFromJson(projectionSequenceObject)
        projection = featureProjectionService.foldBetweenExons(features[0] as Exon, features[1] as Exon, projection)
        render projectionService.convertToJsonFromProjection(projection) as JSON
//        render getAssemblage() as JSON
    }

    def getAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        inputObject = featureProjectionService.expandProjectionJson(inputObject)


        User user = permissionService.currentUser
        Organism organism = preferenceService.getCurrentOrganismPreference(user, inputObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value).toString(), inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))?.organism

        // creates a projection based on the Assemblages and caches them
        inputObject.organism = organism.commonName
        // this generates the projection

        render inputObject as JSON
    }

    @Transactional
    addAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Assemblage assemblage = assemblageService.convertJsonToAssemblage(inputObject)
        // this will save a new assemblage
        User user = permissionService.currentUser
        user.addToAssemblages(assemblage)
        user.save(flush: true)
        render list() as JSON
    }

    @Transactional
    addAssemblageAndReturn() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Assemblage assemblage = assemblageService.convertJsonToAssemblage(inputObject)
        render assemblageService.convertAssemblageToJson(assemblage) as JSON
    }

    @Transactional
    saveAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Assemblage storedAssemblage = assemblageService.convertJsonToAssemblage(inputObject)
        // this will save a new assemblage
        render assemblageService.convertAssemblageToJson(storedAssemblage) as JSON
    }

    @Transactional
    deleteAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)

        inputObject.id.each {
            assemblageService.removeAssemblageById(it, user)
        }

        render list() as JSON
    }

    def searchAssemblage(String searchQuery, String filter) {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject);

        ArrayList<Assemblage> assemblages = new ArrayList<Assemblage>();
        Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        for (Assemblage assemblage : assemblageService.getAssemblagesForUserAndOrganism(user, currentOrganism)) {
            if (assemblage.sequenceList.toLowerCase().contains(searchQuery)) {
                if (filter) {
                    JSONArray jsonArray = JSON.parse(assemblage.sequenceList) as JSONArray
                    Integer numberSequences = jsonArray.size()
                    Boolean leftEdge
                    Boolean rightEdge

                    leftEdge = jsonArray.getJSONObject(0).start > 0
                    rightEdge = jsonArray.getJSONObject(0).end < jsonArray.getJSONObject(0).length

                    switch (filter) {
                        case "Feature":
                            if (leftEdge || rightEdge) {
                                assemblages.add(assemblage);
                            }
                            break
                        case "Combined":
                            if (numberSequences > 1 && !leftEdge && !rightEdge) {
                                assemblages.add(assemblage);
                            }
                            break
                        case "Scaffold":
                            if (numberSequences == 1 && !leftEdge && !rightEdge) {
                                assemblages.add(assemblage);
                            }
                            break
                        default:
                            assemblages.add(assemblage);
                    }
                } else {
                    assemblages.add(assemblage);
                }
            }
        }

        if (assemblages.size() > 0) {
            render assemblages.sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }
    }

}
