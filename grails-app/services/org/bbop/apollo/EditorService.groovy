package org.bbop.apollo

import grails.transaction.Transactional

/**
 * Moved from AnnotationEditor
 */
@Transactional
class EditorService {


    /**
     * Add a feature to the underlying session.
     *
     * @param feature - AbstractSingleLocationBioFeature to be added
     */
    public void addFeature(Feature feature) {
        Feature topLevelFeature = getTopLevelFeature(feature);

        if (feature instanceof Gene) {
            for (Transcript transcript : ((Gene) feature).getTranscripts()) {
                removeExonOverlapsAndAdjacencies(transcript);
            }
        } else if (feature instanceof Transcript) {
            removeExonOverlapsAndAdjacencies((Transcript) feature);
        }

        getSession().addFeature(feature);

    }

    /**
     * Delete a feature to the underlying session.
     *
     * @param feature - AbstractSingleLocationBioFeature to be deleted
     */
    public void deleteFeature(Feature feature) {
        Feature topLevelFeature = getTopLevelFeature(feature);

        // TODO: flesh this out . .. I think it is delete all features from a
//        FeatureDataPositionComparator comparator = new FeatureDataPositionComparator();
//        int index = Collections.binarySearch(features, new FeatureData(feature), comparator);
//        if (index >= 0) {
//            while (index > 0) {
//                AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index - 1).getUniqueName());
//                if (comparator.compare(indexedFeature, feature) == 0) {
//                    --index;
//                }
//                else {
//                    break;
//                }
//            }
//            while (index < features.size()) {
//                AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index).getUniqueName());
//                if (comparator.compare(indexedFeature, feature) != 0) {
//                    break;
//                }
//                if (indexedFeature.equals(feature)) {
//                    features.remove(index);
//                    unindexFeature(feature);
//                    break;
//                }
//                ++index;
//            }
//        }

//        Feature.deleteAll(topLevelFeature)
//        getSession().deleteFeature(feature);
    }

    /**
     * Add a sequence alteration to the genomic region.
     *
     * @param sequenceAlteration - Sequence alteration to be added
     */
    public void addSequenceAlteration(SequenceAlteration sequenceAlteration) {
        getSession().addSequenceAlteration(sequenceAlteration);
//        for (AbstractSingleLocationBioFeature feature :
//            getSession().getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
//            if (feature instanceof Gene) {
//                for (Transcript transcript : ((Gene)feature).getTranscripts()) {
//                    setLongestORF(transcript);
//                }
//            }
//        }
    }

    /**
     * Delete a sequence alteration to the genomic region.
     *
     * @param sequenceAlteration - Sequence alteration to be deleted
     */
    public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration) {
        getSession().deleteSequenceAlteration(sequenceAlteration);
    }

    private Feature getTopLevelFeature(Feature feature) {
        Collection<? extends Feature> parents = feature.getParents();
        if (parents.size() > 0) {
            return getTopLevelFeature(parents.iterator().next());
        } else {
            return feature;
        }
    }
}
