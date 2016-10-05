package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.Strand

@Transactional
class VariantAnnotationService {

    def featureService
    def transcriptService
    def cdsService
    def exonService
    def sequenceService
    def featureRelationshipService

    def calculateVariantEffects(SequenceAlteration variant) {
        log.info "@calculateVariantEffects"
        Set<Feature> variants = new HashSet<>()
        variants.add(variant)
        def overlappingTranscripts = featureService.getOverlappingTranscripts(variant.featureLocation, false)
        for (Transcript transcript : overlappingTranscripts) {
            variants.addAll(getOverlappingVariants(transcript))
        }
        if (overlappingTranscripts.size() > 0) {
            calculateEffectOfVariantsOnTranscripts(variants, overlappingTranscripts)
        }
    }

    def getOverlappingVariants(Transcript transcript) {
        println "@getOverlappingVariants"
        Set<Feature> variantsList = new HashSet<>()
        def overlappingFeaturesList = featureService.getOverlappingFeatures(transcript.featureLocation, false)

        for (Feature feature : overlappingFeaturesList) {
            if (feature instanceof SequenceAlteration && feature.alterationType == FeatureStringEnum.VARIANT.value) {
                // perform instanceof Variant check to avoid SequenceAlterations that are Assembly Error Corrections
                variantsList.add(feature)
            }
        }
        return variantsList
    }

    def calculateEffectOfVariantsOnTranscripts(def variants, def transcripts) {
        println "@calculateEffectOfVariantsOnTranscripts"
        println "Variants: ${variants}"
        println "Transcripts: ${transcripts}"
        for (Transcript transcript : transcripts) {
            String original = featureService.getResiduesWithAlterationsAndFrameshifts(transcript, [FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value])
            String altered = featureService.getResiduesWithAlterationsAndFrameshifts(transcript, [FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value, FeatureStringEnum.VARIANT.value])
            println "Original String: ${original}"
            println "Altered String: ${altered}"
        }
    }

}
