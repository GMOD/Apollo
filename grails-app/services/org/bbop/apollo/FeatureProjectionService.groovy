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
    def bookmarkService



    JSONArray projectTrack(JSONArray inputFeaturesArray, Bookmark bookmark, Boolean reverseProjection = false) {
        MultiSequenceProjection projection = projectionService.getProjection(bookmark)
        return projectTrack(inputFeaturesArray, projection, reverseProjection)
    }

    JSONArray projectTrack(JSONArray inputFeaturesArray, MultiSequenceProjection projection,Boolean reverseProjection = false) {


        if (projection) {
            // process location . . .
            projectFeaturesArray(inputFeaturesArray, projection, reverseProjection,0)

        } else {

        }
        return inputFeaturesArray
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
        ProjectionSequence projectionSequence1 = reverseProjection ? projection.getReverseProjectionSequence(fmin) : projection.getProjectionSequence(fmin)
        ProjectionSequence projectionSequence2 = reverseProjection ? projection.getReverseProjectionSequence(fmax) : projection.getProjectionSequence(fmax)

        if (reverseProjection) {
            // TODO: add reverse offset?
            fmin = fmin ? projection.projectReverseValue(fmin) : null

            // we are projecting a REVERSE, exclusive value
            fmax = fmax ? projection.projectReverseValue(fmax) : null
        } else {
            fmin = fmin ? projection.projectValue(fmin + offset) : null

            // we are projecting an exclusive value
            fmax = fmax ? projection.projectValue(fmax + offset) : null
        }

        if (fmin!=null) {
            locationObject.put(FeatureStringEnum.FMIN.value, fmin)
        }
        if (fmax) {
            locationObject.put(FeatureStringEnum.FMAX.value, fmax)
        }
        // if we don't have a sequence .. need to assign one
        if ( !locationObject.containsKey(FeatureStringEnum.SEQUENCE.value) ){
//        assert projectionSequence1==projectionSequence2
            locationObject.put(FeatureStringEnum.SEQUENCE.value,projectionSequence1 ? projectionSequence1?.name : projectionSequence2?.name)
        }
        return inputFeature
    }

    private JSONArray projectFeaturesArray(JSONArray inputFeaturesArray, MultiSequenceProjection projection, Boolean reverseProjection,Integer offset) {
        for (int i = 0; i < inputFeaturesArray.size(); i++) {
            JSONObject inputFeature = inputFeaturesArray.getJSONObject(i)

            if (inputFeature.containsKey(FeatureStringEnum.SEQUENCE.value)) {
                String sequenceName = inputFeature.getString(FeatureStringEnum.SEQUENCE.value)
                offset = projection.getOffsetForSequence(sequenceName)

            } else {
               // no offset to calculate??
            }

            projectFeature(inputFeature, projection, reverseProjection,offset)

            if (inputFeature.has(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childFeatures = inputFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                projectFeaturesArray(childFeatures, projection, reverseProjection,offset)
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
     * @param feature  Feature to set feature locations on
     * @param min fmin provided as a PROJECTED coordinate
     * @param max fmax provided as a PROJECTED coordinate
     * @return
     */
    def setFeatureLocationsForProjection(MultiSequenceProjection multiSequenceProjection, Feature feature,Integer min,Integer max) {
        // TODO: optimize for feature locations belonging to the same sequence (the most common case)
        def featureLocationList = FeatureLocation.findAllByFeature(feature)
        Integer oldStrand = featureLocationList.first().strand
        feature.featureLocations.clear()

        // this will only return valid projection sequences
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.getReverseProjectionSequences(min,max)


        // they should be ordered, right?
        int rank = 0
        for(projectionSequence in projectionSequenceList){
            int calculatedMin = projectionSequence.offset  // this is the MINimum within the current scope, since this is the PROJECTED offset
            int calculatedMax = projectionSequence.length + projectionSequence.offset  // this is the MAXimum within the current scope
            boolean calculatedMinPartial = true
            boolean calculatedMaxPartial = true
            if(min > calculatedMin){
                calculatedMin = min
                calculatedMinPartial = false
            }
            if(max < calculatedMax){
                calculatedMax = max
                calculatedMaxPartial = false
            }

            Organism organism = Organism.findByCommonName(projectionSequence.organism)
            Sequence sequence = Sequence.findByNameAndOrganism(projectionSequence.name,organism)

            int newFmin = calculatedMin + projectionSequence.start - projectionSequence.offset
            int newFmax = calculatedMax + projectionSequence.start - projectionSequence.offset

            FeatureLocation featureLocation = new FeatureLocation(
                    fmin: newFmin,
                    fmax: newFmax,
                    isFmaxPartial: calculatedMaxPartial,
                    isFminPartial: calculatedMinPartial,
                    sequence: sequence,
                    feature: feature,
                    rank: rank,
                    strand: oldStrand
            ).save(insert:true,failOnError: true,flush:true)
            feature.addToFeatureLocations(featureLocation)
            ++rank
        }
        feature.save(flush: true,insert:false)
        return feature
    }

}
