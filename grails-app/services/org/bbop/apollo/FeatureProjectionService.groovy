package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.Coordinate
import org.bbop.apollo.gwt.shared.projection.DiscontinuousProjection
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class FeatureProjectionService {

    def projectionService
    def assemblageService
    def transcriptService
    def featureService

    // TODO: make this configurable somehow
    private final Integer DEFAULT_FOLDING_BUFFER = 20

    JSONArray projectTrack(JSONArray inputFeaturesArray, Assemblage assemblage, Boolean unProject = false) {
        MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
        return projectFeaturesArray(inputFeaturesArray, projection, unProject, 0)
    }

    /**
     * Anything in this space is assumed to be visible
     * @param sequence
     * @param referenceTrackName
     * @param inputFeaturesArray
     * @return
     */
    @NotTransactional
    private JSONObject projectFeature(JSONObject inputFeature, MultiSequenceProjection projection, Boolean unProject, Integer offset) {

        if (!inputFeature.has(FeatureStringEnum.LOCATION.value)) {
            return inputFeature
        }


        JSONObject locationObject = inputFeature.getJSONObject(FeatureStringEnum.LOCATION.value)

        Integer fmin = locationObject.has(FeatureStringEnum.FMIN.value) ? locationObject.getInt(FeatureStringEnum.FMIN.value) : null
        Integer fmax = locationObject.has(FeatureStringEnum.FMAX.value) ? locationObject.getInt(FeatureStringEnum.FMAX.value) : null
        ProjectionSequence projectionSequence1 = unProject ? projection.getUnProjectedSequence(fmin) : projection.getProjectionSequence(fmin + offset)
        ProjectionSequence projectionSequence2 = unProject ? projection.getUnProjectedSequence(fmax) : projection.getProjectionSequence(fmax + offset)

        if (unProject) {
            // TODO: add reverse offset?
            fmin = fmin ? projection.unProjectValue(fmin) : null

            // we are projecting a REVERSE, exclusive value
            fmax = fmax ? projection.unProjectValue(fmax) : null
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
        if (projectionSequence1 && projectionSequence2) {
            // case 1, projectionSequence1 exists and is equals to projectionSequence2
            if (projectionSequence1.name == projectionSequence2.name) {
                if (!locationObject.sequence) {
                    locationObject.put(FeatureStringEnum.SEQUENCE.value, projectionSequence1.name)
                }
                projectionService.evaluateReverseLocation(projectionSequence1, locationObject)
            } else if (projectionSequence1.name != projectionSequence2.name) {
                locationObject.put(FeatureStringEnum.SEQUENCE.value, createJSONStringFromProjectionSequences([projectionSequence1, projectionSequence2]))
                // TODO: not sure how to handle this case
                assert projectionSequence1.reverse == projectionSequence2.reverse
            }
        } else if (projectionSequence1) {
            if (!locationObject.sequence) {
                locationObject.put(FeatureStringEnum.SEQUENCE.value, projectionSequence1.name)
            }
            projectionService.evaluateReverseLocation(projectionSequence1, locationObject)
        } else if (projectionSequence2) {
            if (!locationObject.sequence) {
                locationObject.put(FeatureStringEnum.SEQUENCE.value, projectionSequence2.name)
            }
            projectionService.evaluateReverseLocation(projectionSequence2, locationObject)
        } else {
            log.debug("Neither projection is valid, so ignoring")
//            throw new AnnotationException("Neither projection sequence seems to be valid")
        }
        return inputFeature
    }

    def createJSONStringFromProjectionSequences(def projectionSequences) {
        JSONArray returnArray = new JSONArray()
        projectionSequences.each {
            JSONObject projectionSequenceJsonObject = new JSONObject()
            projectionSequenceJsonObject.put(FeatureStringEnum.ID.value, it.id)
            projectionSequenceJsonObject.put(FeatureStringEnum.NAME.value, it.name)
            projectionSequenceJsonObject.put(FeatureStringEnum.LENGTH.value, it.unprojectedLength)
            projectionSequenceJsonObject.put(FeatureStringEnum.START.value, it.start)
            projectionSequenceJsonObject.put(FeatureStringEnum.END.value, it.end)
            projectionSequenceJsonObject.put(FeatureStringEnum.REVERSE.value, it.reverse)
            returnArray.add(projectionSequenceJsonObject)
        }
        return returnArray.toString()
    }

    private JSONArray projectFeaturesArray(JSONArray inputFeaturesArray, MultiSequenceProjection projection, Boolean unProject, Integer offset) {
        for (int i = 0; i < inputFeaturesArray.size(); i++) {
            JSONObject inputFeature = inputFeaturesArray.getJSONObject(i)

            if (inputFeature.containsKey(FeatureStringEnum.SEQUENCE.value)) {
                String sequenceName = inputFeature.getString(FeatureStringEnum.SEQUENCE.value)
                offset = projection.getOffsetForSequence(sequenceName)

            } else {
                // no offset to calculate??
            }

            projectFeature(inputFeature, projection, unProject, offset)

            if (inputFeature.has(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childFeatures = inputFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                projectFeaturesArray(childFeatures, projection, unProject, offset)
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
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.getUnProjectedSequences(min, max)

        // they should be ordered, right?
        int rank = 0
        int firstIndex = 0
        int lastIndex = projectionSequenceList.size() - 1
        projectionSequenceList.eachWithIndex { ProjectionSequence projectionSequence, int i ->

            int calculatedMin
            int calculatedMax
            boolean calculatedMinPartial = true
            boolean calculatedMaxPartial = true

            Organism organism = Organism.findByCommonName(projectionSequence.organism)
            Sequence sequence = Sequence.findByNameAndOrganism(projectionSequence.name, organism)

            if (projectionSequence.reverse) {
                // if first index, then we calculate the min
                if (i == firstIndex) {
                    calculatedMin = projectionSequence.end - min - projectionSequence.projectedOffset
                    calculatedMinPartial = false
                }
                // if the min is in the middle, then it must be 0
                // if the min is the last, then it must be 0
                else {
                    calculatedMin = projectionSequence.end
                }

                // if the max if the last, then we calculate it properly
                if (i == lastIndex) {
                    calculatedMax = projectionSequence.end - max - projectionSequence.projectedOffset
                    calculatedMaxPartial = false
                } else {
                    // if the max is in the middle, then it must be the sequence.unprojectedLength
                    // if the max is in the first of many, then it must be sequence.unprojectedLength
//                    calculatedMax = projectionSequence.unprojectedLength
                    calculatedMax = 0
                }

                // swap values
                int temp = calculatedMin
                calculatedMin = calculatedMax
                calculatedMax = temp

            } else {
                // if first index, then we calculate the min
                if (i == firstIndex) {
                    calculatedMin = min + projectionSequence.start - projectionSequence.projectedOffset
                    calculatedMinPartial = false
                }
                // if the min is in the middle, then it must be 0
                // if the min is the last, then it must be 0
                else {
                    calculatedMin = 0
                }

                // if the max if the last, then we calculate it properly
                if (i == lastIndex) {
                    calculatedMax = max + projectionSequence.start - projectionSequence.projectedOffset
                    calculatedMaxPartial = false
                } else {
                    // if the max is in the middle, then it must be the sequence.unprojectedLength
                    // if the max is in the first of many, then it must be sequence.unprojectedLength
                    calculatedMax = projectionSequence.unprojectedLength
                }
            }

            FeatureLocation featureLocation = new FeatureLocation(
                    fmin: calculatedMin,
                    fmax: calculatedMax,
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
        featureService.populatePartialDataForFeature(feature)
        return feature
    }

    /**
     * This method generates projections for a feature.
     *
     * For each exon in the trancript, add a location to the projection
     *
     * @param feature
     */
    def addLocationsForFeature(Feature feature, MultiSequenceProjection projection) {

        splitRegionForCoordinates(projection, feature)

        if (feature instanceof Gene) {
            addLocationsForGene((Gene) feature, projection)
        } else if (feature instanceof Transcript) {
            addLocationsForTranscript((Transcript) feature, projection)
        } else {
            // here, we are essentially clearing it
            addLocationForCoordinate(projection, feature.fmin, feature.fmax)
        }

        return projection
    }

    /**
     * Here, we want to create two regions,
     * @param feature
     * @param multiSequenceProjection
     * @return
     */
    @NotTransactional
    def splitRegionForCoordinates(MultiSequenceProjection projection, Feature feature) {
        // TODO: this does not work if we cross the sequence boundary, but good enough for now
        println "getting sequence for ${feature.fmin}"

        Integer fmin = projectionService.getMinForFeatureInProjection(feature,projection)
        println "but actual fmin, with offsets ${fmin}"
        ProjectionSequence projectionSequence = projection.getUnProjectedSequence(fmin)
        // first we have to clear out all of the projections for that region
        println "sequence: ${projectionSequence}"
        println "size: ${projection.getSequenceDiscontinuousProjectionMap()?.size()}"
        println "contains: ${projection.getSequenceDiscontinuousProjectionMap().containsKey(projectionSequence)}"
        DiscontinuousProjection discontinuousProjection = projection.getSequenceDiscontinuousProjectionMap().get(projectionSequence)
        discontinuousProjection.clear()
        if (feature.name) {
            discontinuousProjection.metadata = new JSONObject(name: feature.name).toString()
        }

        int count = 0
        // project on the LHS
        if (feature.fmin > projectionSequence.start) {
            Coordinate locationLeft = new Coordinate(
                    min: projectionSequence.start,
                    max: feature.fmin,
                    sequence: projectionSequence
            )
            // if it already has this then it won't matter
            projection.addCoordinate(locationLeft)
            ++count
        }

        println "feature ${feature}"
        println "projectionsequence ${projectionSequence}"

        if (feature.fmax < projectionSequence.end) {
            // project on the RHS
            Coordinate locationRight = new Coordinate(
                    min: feature.fmax,
                    max: projectionSequence.end,
                    sequence: projectionSequence
            )
            // if it already has this then it won't matter
            projection.addCoordinate(locationRight)
            ++count
        }
        assert count == discontinuousProjection.size()
        return projection

    }

    def addLocationsForGene(Gene gene, MultiSequenceProjection projection) {
        for (Transcript transcript in transcriptService.getTranscripts(gene)) {
            addLocationsForTranscript(transcript, projection)
        }
        return projection
    }

    def addLocationsForTranscript(Transcript transcript, MultiSequenceProjection projection) {
        for (Exon exon in transcriptService.getExons(transcript)) {
            addLocationForCoordinate(projection, exon.fmin, exon.fmax)
        }
    }

    def addLocationForCoordinate(MultiSequenceProjection projection, int fmin, int fmax) {
        // TODO: this does not work if we cross th sequence boundary, but good enough for now
        ProjectionSequence projectionSequence = projection.getProjectionSequence(fmin)
        Coordinate coordinate = new Coordinate(
                min: fmin - DEFAULT_FOLDING_BUFFER,
                max: fmax + DEFAULT_FOLDING_BUFFER,
                sequence: projectionSequence
        )
        // if it already has this then it won't matter
        projection.addCoordinate(coordinate)
        return projection
    }

    /**
     * The goal here is to expand the JSONObject passed in by collapsing all of the subfeatures of any features labeled but not actually expanded.
     *
     *
     *
     * 1 - Create a "Discontinuous Projection" for any collapsed features in the JSONObject
     * @param jsonObject
     * @return
     */
    JSONObject expandProjectionJson(JSONObject jsonObject) {

        JSONArray sequenceList = jsonObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(jsonObject)

        for (JSONObject sequenceObject in sequenceList) {
            JSONObject featureObject = sequenceObject.feature
            if (featureObject) {
                // if collapsed, but NO PROJECTION at the sequenceobject level then add one
                Feature feature = featureObject.uniquename ? Feature.findByUniqueName(featureObject.uniquename) : Feature.findByName(featureObject.name)
                // TODO: should use scaffold and organism as well in a criteria query
                if (featureObject.collapsed) {
                    multiSequenceProjection = addLocationsForFeature(feature, multiSequenceProjection)
                }
                // remove the locations for that region.  Adding a single overlap will do this automatically.
                else {
                    // TODO: we need a proper method for doing this.
//                    multiSequenceProjection.clear()
                    clearLocationForCoordinateForFeature(multiSequenceProjection, feature)
                }
            }
        }

        JSONArray generatedSequenceArray = projectionService.generateSequenceListFromProjection(multiSequenceProjection)

        // merge the two:
        for (int i = 0; i < sequenceList.size(); i++) {
            JSONObject sequenceObject = sequenceList.getJSONObject(i)
            JSONObject generatedSequenceObject = generatedSequenceArray.getJSONObject(i)
            // copy all non-null, non-empty features from the generate to te non-generated
            generatedSequenceObject.entrySet().each {
                if (it.value) {
                    sequenceObject.put(it.key, it.value)
                }
            }

        }


        jsonObject.put(FeatureStringEnum.SEQUENCE_LIST.value, sequenceList)


        return jsonObject
    }

    /**
     * TODO: handle crossing scaffold case
     * @param projection
     * @param fmin
     * @param fmax
     * @return
     */
    def clearLocationForCoordinateForFeature(MultiSequenceProjection projection, Feature feature) {
        ProjectionSequence projectionSequence = projection.getProjectionSequence(feature.fmin)

        Coordinate location = new Coordinate(
                projectionSequence.start,
                projectionSequence.end,
                projectionSequence
        )
        DiscontinuousProjection discontinuousProjection = projection.addCoordinate(location)
        discontinuousProjection.metadata = (new JSONObject(name: feature.name)).toString()
        return projection
    }

    /**
     * TODO: handle crossing scaffold case
     * @param featureLeft
     * @param featureRight
     * @param multiSequenceProjection
     * @return
     */
    MultiSequenceProjection foldBetweenExons(Exon featureLeft, Exon featureRight, MultiSequenceProjection projection) {

//        projectionService.splitProjection(projection,rightMax,leftMin)
        return projectionService.splitProjection(projection,featureLeft.fmax,featureRight.fmin)

    }
}
