package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class FeatureProjectionService {

    def projectionService
    def assemblageService


    JSONArray projectTrack(JSONArray inputFeaturesArray, Assemblage assemblage, Boolean reverseProjection = false) {
        MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
        return projectFeaturesArray(inputFeaturesArray, projection, reverseProjection,0)
    }


    /**
     * Anything in this space is assumed to be visible
     * @param sequence
     * @param referenceTrackName
     * @param inputFeaturesArray
     * @return
     */
    private
    static JSONObject projectFeature(JSONObject inputFeature, MultiSequenceProjection projection, Boolean reverseProjection, Integer offset) {
        if (!inputFeature.has(FeatureStringEnum.LOCATION.value)) return inputFeature


        JSONObject locationObject = inputFeature.getJSONObject(FeatureStringEnum.LOCATION.value)

        Integer fmin = locationObject.has(FeatureStringEnum.FMIN.value) ? locationObject.getInt(FeatureStringEnum.FMIN.value) : null
        Integer fmax = locationObject.has(FeatureStringEnum.FMAX.value) ? locationObject.getInt(FeatureStringEnum.FMAX.value) : null
        ProjectionSequence projectionSequence1 = reverseProjection ? projection.getReverseProjectionSequence(fmin) : projection.getProjectionSequence(fmin+offset)
        ProjectionSequence projectionSequence2 = reverseProjection ? projection.getReverseProjectionSequence(fmax) : projection.getProjectionSequence(fmax+offset)

        if (reverseProjection) {
            // TODO: add reverse offset?
            fmin = fmin ? projection.projectReverseValue(fmin)  : null

            // we are projecting a REVERSE, exclusive value
            fmax = fmax ? projection.projectReverseValue(fmax) : null
        } else {
            fmin = fmin ? projection.projectValue(fmin + offset) : null

            // we are projecting an exclusive value
            fmax = fmax ? projection.projectValue(fmax + offset) : null
        }

        if (fmin != null) {
            locationObject.put(FeatureStringEnum.FMIN.value, fmin)
        }
        if (fmax) {
            locationObject.put(FeatureStringEnum.FMAX.value, fmax)
        }
        // if we don't have a sequence .. need to assign one
        if (!locationObject.containsKey(FeatureStringEnum.SEQUENCE.value)) {
            if(projectionSequence1 && projectionSequence2) {
                // case 1, projectionSequence1 exists and is equals to projectionSequence2
                if (projectionSequence1.name == projectionSequence2.name) {
                    locationObject.put(FeatureStringEnum.SEQUENCE.value, projectionSequence1.name)
                } else if (projectionSequence1.name != projectionSequence2.name) {
                    locationObject.put(FeatureStringEnum.SEQUENCE.value, "[{\"name\":\""+projectionSequence1.name + "\"},{\"name\":\"" + projectionSequence2.name+"\"}]")
                }
            }
            else{
                locationObject.put(FeatureStringEnum.SEQUENCE.value, projectionSequence1 ? projectionSequence1?.name : projectionSequence2?.name)
            }
        }
        return inputFeature
    }

    private JSONArray projectFeaturesArray(JSONArray inputFeaturesArray, MultiSequenceProjection projection, Boolean reverseProjection, Integer offset) {
        for (int i = 0; i < inputFeaturesArray.size(); i++) {
            JSONObject inputFeature = inputFeaturesArray.getJSONObject(i)

            if (inputFeature.containsKey(FeatureStringEnum.SEQUENCE.value)) {
                String sequenceName = inputFeature.getString(FeatureStringEnum.SEQUENCE.value)
                offset = projection.getOffsetForSequence(sequenceName)

            } else {
                // no offset to calculate??
            }

            projectFeature(inputFeature, projection, reverseProjection, offset)

            if (inputFeature.has(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childFeatures = inputFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                projectFeaturesArray(childFeatures, projection, reverseProjection, offset)
            }
        }
        return inputFeaturesArray
    }

    /**
     * This method calculates a new set of feature locations based on projection, removes the old one and adds the new one.
     *
     * Spefically this method allows us to calculate MULTIPLE project sequences.
     *
     * @param multiSequenceProjection Projection context
     * @param feature Feature to set feature locations on
     * @param min fmin provided as a PROJECTED coordinate
     * @param max fmax provided as a PROJECTED coordinate
     * @return
     */
    def setFeatureLocationsForProjection(MultiSequenceProjection multiSequenceProjection, Feature feature, Integer min, Integer max) {
        // TODO: optimize for feature locations belonging to the same sequence (the most common case)
        def featureLocationList = FeatureLocation.findAllByFeature(feature)
        Integer oldStrand = featureLocationList.first().strand
        feature.featureLocations.clear()

        // this will only return valid projection sequences
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.getReverseProjectionSequences(min, max)

        // they should be ordered, right?
        int rank = 0
        int firstIndex = 0
        int lastIndex = projectionSequenceList.size() - 1
//        for(projectionSequence in projectionSequenceList){
        projectionSequenceList.eachWithIndex { ProjectionSequence projectionSequence, int i ->
            int calculatedMin = projectionSequence.offset  // this is the MINimum within the current scope, since this is the PROJECTED offset
            int calculatedMax = projectionSequence.length + projectionSequence.offset  // this is the MAXimum within the current scope
            boolean calculatedMinPartial = true
            boolean calculatedMaxPartial = true

            // if first index, then we calculate the min
            if (i == firstIndex) {
//                if(min > calculatedMin){
                calculatedMin = min + projectionSequence.start - projectionSequence.offset
                calculatedMinPartial = false
//                }
            }
            // if the min is in the middle, then it must be 0
            // if the min is the last, then it must be 0
            else {
                calculatedMin = 0
            }

            // if the max if the last, then we calculate it properly
            if (i == lastIndex) {
//                if(max < calculatedMax){
                calculatedMax = max + projectionSequence.start - projectionSequence.offset
                calculatedMaxPartial = false
//                }
            } else {
                // if the max is in the middle, then it must be the sequence.unprojectedLength
                // if the max is in the first of many, then it must be sequence.unprojectedLength
//                if(max > projectionSequence.length - projectionSequence.start ){
                calculatedMax = projectionSequence.unprojectedLength
//                }
            }
//            if(max > projectionSequence.offset)

            Organism organism = Organism.findByCommonName(projectionSequence.organism)
            Sequence sequence = Sequence.findByNameAndOrganism(projectionSequence.name, organism)

//            int newFmin = calculatedMin + projectionSequence.start - projectionSequence.offset
//            int newFmax = calculatedMax + projectionSequence.start - projectionSequence.offset
            int newFmin = calculatedMin
            int newFmax = calculatedMax

            FeatureLocation featureLocation = new FeatureLocation(
                    fmin: newFmin,
                    fmax: newFmax,
                    isFmaxPartial: calculatedMaxPartial,
                    isFminPartial: calculatedMinPartial,
                    sequence: sequence,
                    feature: feature,
                    rank: rank,
                    strand: oldStrand
            ).save(insert: true, failOnError: true, flush: true)
            feature.addToFeatureLocations(featureLocation)
            ++rank
        }
        feature.save(flush: true, insert: false)
        return feature
    }

}
