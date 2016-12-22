package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.alteration.SequenceAlterationInContext
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.StandardTranslationTable
import org.bbop.apollo.sequence.Strand
import org.bbop.apollo.sequence.TranslationTable
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class VariantAnnotationService {

    def featureService
    def transcriptService
    def cdsService
    def exonService
    def overlapperService
    def sequenceService
    def featureRelationshipService
    def configWrapperService

    // sequenceTrace for tests
    HashMap<Allele,ArrayList<String>> sequenceTrace = new HashMap<>()

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

    /**
     * Calculate effect of a variant on all overlapping transcripts
     * @param variant
     */
    def calculateEffectOfVariant(SequenceAlteration variant) {
        log.info "calculate effect of variant ${variant.name}"
        def overlappingTranscripts = featureService.getOverlappingTranscripts(variant.featureLocation, false)
        log.info "Overlapping transcripts: ${overlappingTranscripts.name}"
        // flush sequence trace
        sequenceTrace.clear()
        for (Transcript transcript : overlappingTranscripts) {
            calculateEffectOfVariantOnTranscript(variant, transcript)
        }
        // return sequenceTrace for testing purpose
        return sequenceTrace
    }

    /**
     * Calculate effect of a variant on a given transcript
     * @param variant
     * @param transcript
     * @return
     */
    def calculateEffectOfVariantOnTranscript(SequenceAlteration variant, Transcript transcript) {
        println "@calculateEffectOfVariantOnTranscript"
        int transcriptFmin = transcript.fmin
        int transcriptFmax = transcript.fmax
        int transcriptStrand = transcript.strand
        CDS cds = transcriptService.getCDS(transcript)
        TranslationTable tt = configWrapperService.getTranslationTable()
        boolean readThroughStopCodon = cdsService.getStopCodonReadThrough(cds) ? true : false

        // I. Get the genomic sequence of the transcript
        String originalTranscriptGenomicSequence = sequenceService.getResiduesFromFeature(transcript)
        StringBuilder residueBuilder = new StringBuilder(originalTranscriptGenomicSequence)

        // II. Keep track of global exon fmin, fmax and local fmin, fmax
        def exons = transcriptService.getSortedExons(transcript, true)
        def exonFminArray = []
        def exonFmaxArray = []
        def exonLocalFminArray = []
        def exonLocalFmaxArray = []

        int exonLocalFmin, exonLocalFmax

        if (transcriptStrand == Strand.NEGATIVE.value) {
            for (Exon exon : exons) {
                exonFminArray.add(exon.fmin)
                exonFmaxArray.add(exon.fmax)
                exonLocalFmin = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcriptStrand, exon.fmax) + 1
                exonLocalFmax = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcriptStrand, exon.fmin)
                log.info "[ - ] exonLocalFmin: ${exonLocalFmin} exonLocalFmax: ${exonLocalFmax}"
                exonLocalFminArray.add(exonLocalFmin)
                exonLocalFmaxArray.add(exonLocalFmax)
            }
        }
        else {
            for (Exon exon  : exons) {
                exonFminArray.add(exon.fmin)
                exonFmaxArray.add(exon.fmax)
                exonLocalFmin = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcriptStrand, exon.fmin)
                exonLocalFmax = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcriptStrand, exon.fmax)
                log.info "[ + ] exonLocalFmin: ${exonLocalFmin} exonLocalFmax: ${exonLocalFmax}"
                exonLocalFminArray.add(exonLocalFmin)
                exonLocalFmaxArray.add(exonLocalFmax)
            }
        }

        // III. Incorporate all ASSEMBLY_ERROR_CORRECTIONS while keeping track of the offset introduced
        def sequenceAlterations = featureService.getSequenceAlterationsForFeature(transcript, [FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value])
        ArrayList<SequenceAlterationInContext> sequenceAlterationInContextList = featureService.getSequenceAlterationsInContextForFeature(transcript, sequenceAlterations)
        def orderedSequenceAlterationsInContextList = featureService.sortSequenceAlterationsInContextList(sequenceAlterationInContextList)

        if (transcriptStrand == Strand.NEGATIVE.value) {
            Collections.reverse(orderedSequenceAlterationsInContextList)
        }

        int cumulativeOffset = 0
        for (SequenceAlterationInContext sa : orderedSequenceAlterationsInContextList) {
            int localCoordinate = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcriptStrand, sa.fmin + cumulativeOffset)
            String alterationResidue = sa.alterationResidue
            int genomicOffset = alterationResidue.length()

            if (transcriptStrand == Strand.NEGATIVE.value) {
                alterationResidue = SequenceTranslationHandler.reverseComplementSequence(alterationResidue)
            }

            log.info "Alteration residue: ${alterationResidue} at position ${sa.fmin} with localCoordinate: ${localCoordinate}"
            if (sa.instanceOf == Insertion.canonicalName) {
                // incorporating INESRTION to the genomic sequence
                log.info "[ AEC-INS ] Sequence alteration instanceof INSERTION"
                int insertionPosition
                if (transcriptStrand == Strand.NEGATIVE.value) {
                    insertionPosition = localCoordinate + 1
                }
                else {
                    insertionPosition = localCoordinate
                }
                residueBuilder.insert(insertionPosition, alterationResidue)
                cumulativeOffset += genomicOffset

                for (int i = 0; i < exonLocalFminArray.size(); i++) {
                    if (localCoordinate <= exonLocalFminArray[i]) {
                        // if sequence alteration is before the exon fmin then that means the exon is a downstream exon
                        // and thus will be affected by the incorporated Insertion
                        exonLocalFminArray[i] += genomicOffset
                    }
                    if (localCoordinate <= exonLocalFmaxArray[i]) {
                        // if sequence alteration is before the exon fmax then that means the exon is a downstream exon
                        // and thus will be affected by the incorporated Insertion
                        exonLocalFmaxArray[i] += genomicOffset
                    }

                    // Since the SA is on +, applying offset to all global fmin and fmax downstream of SA
                    if (sa.fmin <= exonFminArray[i]) {
                        exonFminArray[i] += genomicOffset
                    }
                    if (sa.fmin <= exonFmaxArray[i]) {
                        exonFmaxArray[i] += genomicOffset
                    }
                }
                log.info "[ AEC-INS ] Exon Local Fmin Array (with offset): ${exonLocalFminArray}"
                log.info "[ AEC-INS ] Exon Local Fmax Array (with offset): ${exonLocalFmaxArray}"
                log.info "[ AEC-INS ] Exon Global Fmax Array (with offset): ${exonFminArray}"
                log.info "[ AEC-INS ] Exon Global Fmax Array (with offset): ${exonFmaxArray}"

                // Since the SA is on +, applying offset to global transcript fmax
                transcriptFmax += genomicOffset
            }
            else if (sa.instanceOf == Deletion.canonicalName) {
                // incorporating DELETION to the genomic sequence
                log.info "[ AEC-DEL ] Sequence alteration instanceof DELETION"
                int deletionPositionStart, deletionPositionEnd
                if (transcriptStrand == Strand.NEGATIVE.value) {
                    deletionPositionStart = (localCoordinate - genomicOffset) + 1
                    deletionPositionEnd = localCoordinate + 1
                }
                else {
                    deletionPositionStart = localCoordinate
                    deletionPositionEnd = deletionPositionStart + genomicOffset
                }
                residueBuilder.replace(deletionPositionStart, deletionPositionEnd, "")
                cumulativeOffset -= genomicOffset

                for (int i = 0; i < exonLocalFminArray.size(); i++) {
                    if (localCoordinate <= exonLocalFminArray[i]) {
                        // if sequence alteration is before the exon fmin then that means the exon is a downstream exon
                        // and thus will be affected by the incorporated Deletion
                        exonLocalFminArray[i] -= genomicOffset
                    }
                    if (localCoordinate <= exonLocalFmaxArray[i]) {
                        // if sequence alteration is before the exon fmax then that means the exon is a downstream exon
                        // and thus will be affected by the incorporated Deletion
                        exonLocalFmaxArray[i] -= genomicOffset
                    }

                    // Since the SA is on +, applying offset to all global fmin and fmax downstream of SA
                    if (sa.fmin <= exonFminArray[i]) {
                        exonFminArray[i] -= genomicOffset
                    }
                    if (sa.fmin <= exonFmaxArray[i]) {
                        exonFmaxArray[i] -= genomicOffset
                    }
                    // Since the SA is on +, applying offset to global transcript fmax
                    transcriptFmax -= genomicOffset
                }
            }
            else if (sa.instanceOf == Substitution.canonicalName) {
                // incorporating SUBSTITUTION to the genomic sequence
                log.info "[ AEC-SUB ] Sequence alteration is instanceof SUBSTITUTION"
                int start
                if (transcriptStrand == Strand.NEGATIVE.value) {
                    start = (localCoordinate - genomicOffset) + 1
                }
                else {
                    start = localCoordinate
                }
                log.info "[ AEC-SUB ] start: ${start} end: ${start + genomicOffset}"
                residueBuilder.replace(start, start + genomicOffset, alterationResidue)
            }
        }

        String alteredTranscriptGenomicSequence = residueBuilder.toString()
        log.info "Genomic Sequence (before): ${originalTranscriptGenomicSequence}"
        log.info "Genomic Sequence  (after): ${alteredTranscriptGenomicSequence}"

        // IV. Create altered CDNA sequence
        String alteredCdnaSequence = ""
        for (int i = 0; i < exonLocalFminArray.size(); i++) {
            if (transcriptStrand == Strand.NEGATIVE.value) {
                alteredCdnaSequence += alteredTranscriptGenomicSequence.substring(exonLocalFminArray[i], exonLocalFmaxArray[i] + 1)
            }
            else {
                alteredCdnaSequence += alteredTranscriptGenomicSequence.substring(exonLocalFminArray[i], exonLocalFmaxArray[i])
            }
        }

        log.info "Altered cDNA Sequence: ${alteredCdnaSequence}"

        // V. Calculate longest ORF for altered CDNA sequence
        def results = featureService.calculateLongestORF(alteredCdnaSequence, tt, readThroughStopCodon)
        String alteredPeptideSequence = results.get(0)
        int cdsStart = results.get(1)
        int cdsEnd = results.get(2)
        log.info "Altered CDS Start: ${cdsStart}"
        log.info "Altered CDS End: ${cdsEnd}"

        // VI. Get global CDS fmin and fmax
        int cdsFmin, cdsFmax
        if (transcriptStrand == Strand.NEGATIVE.value) {
            cdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, cdsStart) + 1
            cdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, cdsEnd) + 1
        }
        else {
            cdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, cdsStart)
            cdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, cdsEnd)
        }
        log.info "Altered CDS global Fmin: ${cdsFmin}"
        log.info "Altered CDS global Fmax: ${cdsFmax}"
        String alteredCdsSequence = alteredCdnaSequence.substring(cdsStart, cdsEnd)

        // VI. Now for each variant's alternate allele, apply the variant to the genomic sequence
        int correctedVariantFmin, correctedVariantFmax
        (correctedVariantFmin, correctedVariantFmax) = applyOffsetFromAssemblyErrorCorrections(variant, orderedSequenceAlterationsInContextList)
        int localVariantFmin = convertSourceCoordinateToLocalCoordinate(transcriptFmin, transcriptFmax, transcript.strand, correctedVariantFmin)
        int localVariantFminOnCDNA = convertSourceCoordinateToLocalCoordinateForTranscript(exonFminArray, exonFmaxArray, transcript.strand, correctedVariantFmin)
        int localVariantFminOnCDS = convertSourceCoordinateToLocalCoordinateForCDS(cdsFmin, cdsFmax, exonFminArray, exonFmaxArray, transcript.strand, correctedVariantFmin)
        log.info "Local Variant Fmin on genomic: ${localVariantFmin}"
        log.info "Local Variant Fmin on CDNA: ${localVariantFminOnCDNA}"
        log.info "Local Variant Fmin on CDS: ${localVariantFminOnCDS}"

        StringBuilder finalResidueBuilder = new StringBuilder(alteredTranscriptGenomicSequence)

        int cumulativeOffsetFromVariant = 0
        for (Allele allele : variant.alternateAlleles) {
            log.info "Processing variant ${variant.referenceBases} -> ${allele.bases} at position: ${variant.fmin}"
            String alterationResidue = allele.alterationResidue
            if (transcript.strand == Strand.NEGATIVE.value) {
                alterationResidue  = SequenceTranslationHandler.reverseComplementSequence(alterationResidue)
            }
            int genomicOffset = alterationResidue.length()

            if (variant instanceof Insertion) {
                // TODO
            }
            else if (variant instanceof Deletion) {
                // TODO
            }
            else if (variant instanceof Substitution) {
                int start
                if (transcriptStrand == Strand.NEGATIVE.value) {
                    start = (localVariantFmin - genomicOffset) + 1
                }
                else {
                    start = localVariantFmin
                }
                log.info "[ VAR-SUB ] Substitution Position: ${start}"
                finalResidueBuilder.replace(start, start + genomicOffset, alterationResidue)
            }

            String finalAlteredGenomicSequence = finalResidueBuilder.toString()
            log.info "Final Altered Genomic Sequence: ${finalAlteredGenomicSequence}"

            // VII. Track the effect the variant has on the CDNA sequence, CDS sequence and peptide sequence
            String finalAlteredCdnaSequence = ""
            for (int i = 0; i < exonLocalFminArray.size(); i++) {
                if (transcriptStrand == Strand.NEGATIVE.value) {
                    finalAlteredCdnaSequence += finalAlteredGenomicSequence.substring(exonLocalFminArray[i], exonLocalFmaxArray[i] + 1)
                }
                else {
                    finalAlteredCdnaSequence += finalAlteredGenomicSequence.substring(exonLocalFminArray[i], exonLocalFmaxArray[i])
                }
            }

            log.info "Final CDNA Sequence: ${finalAlteredCdnaSequence}"
            def results2 = featureService.calculateLongestORF(finalAlteredCdnaSequence, configWrapperService.getTranslationTable(), readThroughStopCodon)
            String finalPeptideSequence = results2.get(1)
            int finalCdsStart = results2.get(1)
            int finalCdsEnd = results2.get(2)

            log.info "Final CDS start: ${finalCdsStart}"
            log.info "Final CDS end: ${finalCdsEnd}"

            int finalCdsFmin, finalCdsFmax
            if (transcriptStrand == Strand.NEGATIVE.value) {
                finalCdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, finalCdsStart) + 1
                finalCdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, finalCdsEnd) + 1
            }
            else {
                finalCdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, finalCdsStart)
                finalCdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, transcriptStrand, finalCdsEnd)
            }
            log.info "Final CDS global Fmin: ${finalCdsFmin}"
            log.info "Final CDS global Fmax: ${finalCdsFmax}"

            String finalAlteredCdsSequence = finalAlteredCdnaSequence.substring(finalCdsStart, finalCdsEnd)

            // VIII. Create a VariantEffect entity
            String originalCodon, alteredCodon
            String originalAminoAcid, alteredAminoAcid
            int originalAminoAcidPosition, alteredAminoAcidPosition

            if (variant instanceof Insertion) {
                // TODO
            }
            else if (variant instanceof Deletion) {
                // TODO
            }
            else if (variant instanceof Substitution) {
                int finalLocalVariantFminOnCDS = convertSourceCoordinateToLocalCoordinateForCDS(finalCdsFmin, finalCdsFmax, exonFminArray, exonFmaxArray, transcriptStrand, correctedVariantFmin)
                if (correctedVariantFmin <= cdsFmax) {
                    log.info "[ VE-SUB ] Variant Fmin is within original CDS (CDS after accounting for AEC)"
                    (originalCodon, originalAminoAcidPosition) = getCodonFromSequence(localVariantFminOnCDS, alteredCdsSequence)
                    originalAminoAcid = SequenceTranslationHandler.translateSequence(originalCodon, configWrapperService.getTranslationTable(), true, false)
                }
                else {
                    // variant is not within the CDS of tne transcript
                    // thus originalCodon will be '.' and originalAminoAcidPosition will be '.'
                    originalCodon = '.'
                    originalAminoAcidPosition = -1
                    originalAminoAcid = '.'
                }

                if (correctedVariantFmin <= finalCdsFmax) {
                    log.info "[ VE-SUB ] Variant fmin is within final altered CDS"
                    (alteredCodon, alteredAminoAcidPosition) = getCodonFromSequence(finalLocalVariantFminOnCDS, finalAlteredCdsSequence)
                    alteredAminoAcid = SequenceTranslationHandler.translateSequence(alteredCodon, configWrapperService.getTranslationTable(), true, false)
                }
                else {
                    // the presence of this variant has altered the CDS from the original
                    // thus alteredCodon, alteredAminoAcidPosition and alteredAminoAcid will be '.'
                    alteredCodon = '.'
                    alteredAminoAcidPosition = -1
                    alteredAminoAcid = '.'
                }

                log.info "[ VE-SUB ] codon change: ${originalCodon} -> ${alteredCodon}"
                log.info "[ VE-SUB ] aa change: ${originalAminoAcid} -> ${alteredAminoAcid}"
                log.info "[ VE-SUB ] aa pos: ${originalAminoAcidPosition}"
                log.info "[ VE-SUB ] cDNA Position: ${localVariantFminOnCDNA}"
                log.info "[ VE-SUB ] CDS Position: ${finalLocalVariantFminOnCDS}"
                // If finalLocalVariantFminOnCDS is -1 then that means the CDS of the transcript changed
                // as a result of the variant

                VariantEffect variantEffect = new VariantEffect(
                        referenceResidue: originalAminoAcid,
                        alternateResidue: alteredAminoAcid,
                        referenceCodon: originalCodon,
                        alternateCodon: alteredCodon
                )
                if (alteredAminoAcidPosition != -1) variantEffect.proteinPosition = alteredAminoAcidPosition
                if (localVariantFminOnCDNA != -1) variantEffect.cdnaPosition = localVariantFminOnCDNA
                if (localVariantFminOnCDS != -1) variantEffect.cdsPosition = localVariantFminOnCDS
                variantEffect.feature = transcript
                variantEffect.variant = variant
                variantEffect.alternateAllele = allele

                // IX. Create a JSONObject for what the altered transcript is likely to look like
                JSONObject transcriptJsonObject = featureService.convertFeatureToJSON(transcript)
                // TODO: reparse this JSONObject to reflect the modifications as a result of the variant
                // For substitution variant, the only thing that changes is the CDS fmin and fmax
                println "Transcript JSON Object (before): ${transcriptJsonObject.toString()}"
                for (JSONObject child : transcriptJsonObject.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                    if (child.getJSONObject(FeatureStringEnum.TYPE.value).name == FeatureStringEnum.CDS.value) {
                        JSONObject locationObject = child.getJSONObject(FeatureStringEnum.LOCATION.value)
                        locationObject.fmin = finalCdsFmin
                        locationObject.fmax = finalCdsFmax
                        child.put(FeatureStringEnum.LOCATION.value, locationObject)
                    }
                }
                println "Transcript JSON Object (after): ${transcriptJsonObject.toString()}"

                variantEffect.metadata = transcriptJsonObject.toString()
                variantEffect.save()
                allele.addToVariantEffects(variantEffect)
            }

            def sequenceArray = [alteredTranscriptGenomicSequence, alteredCdnaSequence, alteredCdsSequence,
                                 finalAlteredGenomicSequence, finalAlteredCdnaSequence, finalAlteredCdsSequence]
            sequenceTrace.put(allele, sequenceArray)
        }
        // for testing purposes
        return sequenceTrace
    }

    /**
     * Assign SO Type to Variant Effect
     * @param variant
     * @param variantEffect
     * @param transcript
     */
    def assignSOTypeToVariantEffect(SequenceAlteration variant, VariantEffect variantEffect, Transcript transcript) {

        variantEffect.addToEffects(new SequenceVariant())
        variantEffect.save()
        // TODO: Get SO Terms based on variantEffect
    }

    /**
     * Apply offsets of all the sequence alterations that are upstream of the given variant
     * @param variant
     * @param sequenceAlterationInContextList
     * @return
     */
    def applyOffsetFromAssemblyErrorCorrections(SequenceAlteration variant, Collection<SequenceAlterationInContext> sequenceAlterationInContextList) {
        int variantFmin = variant.fmin
        int variantFmax = variant.fmax

        for (SequenceAlterationInContext sa : sequenceAlterationInContextList) {
            if (variant.fmin >= sa.fmin) {
                // variant is downstream of the alteration
                if (sa.instanceOf == Insertion.class.canonicalName) {
                    variantFmin += sa.alterationResidue.length()
                    variantFmax += sa.alterationResidue.length()
                }
                else if (sa.instanceOf == Deletion.class.canonicalName) {
                    variantFmin -= sa.alterationResidue.length()
                    variantFmax -= sa.alterationResidue.length()
                }
                else if (sa.instanceOf == Substitution.class.canonicalName) {
                    // do nothing since AEC of tpye Substitution don't introduce any frameshifts
                }
            }
        }
        return [variantFmin, variantFmax]
    }

    /**
     * Given a source coordinate, transform the coordinate in the context of a given feature.
     * @param fmin
     * @param fmax
     * @param strand
     * @param sourceCoordinate
     * @return
     */
    def convertSourceCoordinateToLocalCoordinate(int fmin, int fmax, int strand, int sourceCoordinate) {
        log.info "convertSourceCoordinateToLocalCoordinate: ${fmin} ${fmax} ${strand} ${sourceCoordinate}"
        // fmin and fmax are in global context
        if (sourceCoordinate < fmin || sourceCoordinate > fmax) {
            // sourceCoordinate never falls within the feature
            log.info "sourceCoordinate never falls within the feature; return -1"
            return -1
        }
        if (strand == Strand.NEGATIVE.value) {
            return fmax - 1 - sourceCoordinate
        }
        else {
            return sourceCoordinate - fmin
        }
    }

    /**
     * Given a local coordinate, transform the coordinate into global context
     * @param fmin
     * @param fmax
     * @param strand
     * @param localCoordinate
     * @return
     */
    def convertLocalCoordinateToSourceCoordinate(int fmin, int fmax, int strand, int localCoordinate) {
        log.info "convertLocalCoordinateToSourceCoordinate: ${fmin} ${fmax} ${strand} ${localCoordinate}"
        // fmin and fmax are in global context
        if (localCoordinate < 0 || localCoordinate > (fmax - fmin)) {
            log.info "localCoordinate never falls within the feature"
            // local coordinate never falls within the feature
            return -1
        }
        if (strand == Strand.NEGATIVE.value) {
            return fmax - localCoordinate - 1
        }
        else {
            return fmin + localCoordinate
        }
    }

    /**
     * Given a source coordinate, transform the coordinate in the context of a given transcript's exons
     * @param exonFminArray
     * @param exonFmaxArray
     * @param strand
     * @param sourceCoordinate
     * @return
     */
    def convertSourceCoordinateToLocalCoordinateForTranscript(List exonFminArray, List exonFmaxArray, int strand, int sourceCoordinate) {
        log.info "convertSourceCoordinateToLocalCoordinateForTranscript: ${exonFminArray} ${exonFmaxArray} ${strand} ${sourceCoordinate}"
        // exon fmin and fmax array are in global context
        int localCoordinate = -1
        int currentCoordinate = 0

        // Ensuring that the arrays have coordinates sorted in the right order
        if (strand == Strand.NEGATIVE.value) {
            exonFminArray.sort(true, {a, b -> b <=> a})
            exonFmaxArray.sort(true, {a, b -> b <=> a})
        }
        else {
            exonFminArray.sort(true, {a, b -> a <=> b})
            exonFmaxArray.sort(true, {a, b -> a <=> b})
        }

        for (int i = 0; i < exonFminArray.size(); i++) {
            int exonFmin = exonFminArray.get(i)
            int exonFmax = exonFmaxArray.get(i)

            if (sourceCoordinate >= exonFmin && sourceCoordinate <= exonFmax) {
                // sourceCoordinate falls within an exon
                log.info "sourceCoordinate falls within exon ${exonFmin} - ${exonFmax}"
                if (strand == Strand.NEGATIVE.value) {
                    localCoordinate = currentCoordinate + (exonFmax - sourceCoordinate) - 1
                }
                else {
                    localCoordinate = currentCoordinate + (sourceCoordinate - exonFmin)
                }
                break
            }
            else {
                currentCoordinate += (exonFmax - exonFmin)
            }

        }
        return localCoordinate
    }

    /**
     * Given a local coordinate, transforms the coordinate into global context, accounting for exons
     * @param exonFminArray
     * @param exonFmaxArray
     * @param strand
     * @param localCoordinate
     * @return sourceCoordinate
     */
    def convertLocalCoordinateToSourceCoordinateForTranscript(List exonFminArray, List exonFmaxArray, int strand, int localCoordinate) {
        log.info "convertLocalCoordinateToSourceCoordinateForTranscript: ${exonFminArray} ${exonFmaxArray} ${strand} ${localCoordinate}"
        int sourceCoordinate = -1
        int currentLength = 0
        int currentCoordinate = localCoordinate

        // Ensuring that the arrays have coordinates sorted in the right order
        if (strand == Strand.NEGATIVE.value) {
            exonFminArray.sort(true, {a, b -> b <=> a})
            exonFmaxArray.sort(true, {a, b -> b <=> a})
        }
        else {
            exonFminArray.sort(true, {a, b -> a <=> b})
            exonFmaxArray.sort(true, {a, b -> a <=> b})
        }

        for (int i = 0; i < exonFminArray.size(); i++) {
            int exonFmin = exonFminArray.get(i)
            int exonFmax = exonFmaxArray.get(i)
            int exonLength = exonFmax - exonFmin

            if (currentLength + exonLength >= localCoordinate) {
                log.info "localCoordinate falls within exon ${exonFmin} - ${exonFmax}"
                if (strand == Strand.NEGATIVE.value) {
                    sourceCoordinate = (exonFmax - currentCoordinate) - 1
                }
                else {
                    sourceCoordinate = exonFmin + currentCoordinate
                }
                break
            }
            else {
                currentLength += exonLength
                currentCoordinate -= exonLength
            }
        }
        return sourceCoordinate
    }

    /**
     * Given a source coordinate, transforms the coordinate into the context of a given transcript's CDS
     * @param cdsFmin
     * @param cdsFmax
     * @param exonFminArray
     * @param exonFmaxArray
     * @param strand
     * @param sourceCoordinate
     * @return
     */
    def convertSourceCoordinateToLocalCoordinateForCDS(int cdsFmin, int cdsFmax, List exonFminArray, List exonFmaxArray, int strand, int sourceCoordinate) {
        int localCoordinate = 0

        if (!(cdsFmin <= sourceCoordinate && cdsFmax >= sourceCoordinate)) {
            return -1
        }

        int x,y = 0

        if (strand == Strand.POSITIVE.value) {
            for (int i = 0; i < exonFminArray.size(); i++) {
                int exonFmin = exonFminArray.get(i)
                int exonFmax = exonFmaxArray.get(i)
                if (overlaps(exonFmin, exonFmax, cdsFmin, cdsFmax) && exonFmin >= cdsFmin && exonFmax <= cdsFmax) {
                    // complete overlap
                    log.info "Complete overlap"
                    x = exonFmin
                    y = exonFmax
                }
                else if (overlaps(exonFmin, exonFmax, cdsFmin, cdsFmax)) {
                    // partial overlap
                    log.info "Partial overlap"
                    if (exonFmin < cdsFmin && exonFmax < cdsFmax) {
                        x = cdsFmin
                        y = exonFmax
                    }
                    else {
                        x = exonFmin
                        y = cdsFmax
                    }
                }
                else {
                    // no overlap
                    log.info "No overlap"
                    continue
                }

                if (x <= sourceCoordinate && y >= sourceCoordinate) {
                    localCoordinate += sourceCoordinate - x
                    return localCoordinate
                }
                else {
                    localCoordinate += y - x
                }
            }
        }
        else {
            // Ensuring that the arrays have coordinates sorted in the right order
            // without having to assume that its being provided in the right order
            if (strand == Strand.NEGATIVE.value) {
                exonFminArray.sort(true, {a, b -> b <=> a})
                exonFmaxArray.sort(true, {a, b -> b <=> a})
            }
            else {
                exonFminArray.sort(true, {a, b -> a <=> b})
                exonFmaxArray.sort(true, {a, b -> a <=> b})
            }

            for (int i = 0; i < exonFminArray.size(); i++) {
                int exonFmin = exonFminArray.get(i)
                int exonFmax = exonFmaxArray.get(i)
                if (overlaps(exonFmin, exonFmax, cdsFmin, cdsFmax) && exonFmin >= cdsFmin && exonFmax <= cdsFmax) {
                    // complete overlap
                    log.info "Complete overlap"
                    x = exonFmax
                    y = exonFmin
                }
                else if (overlaps(exonFmin, exonFmax, cdsFmin, cdsFmax)) {
                    // partial overlap
                    log.info "Partial overlap"
                    if (exonFmin <= cdsFmin && exonFmax <= cdsFmax) {
                        x = exonFmax
                        y = cdsFmin
                    }
                    else {
                        x = cdsFmax
                        y = exonFmin
                    }
                }
                else {
                    log.info "No overlap"
                    continue
                }
                if (y <= sourceCoordinate && x >= sourceCoordinate) {
                    localCoordinate += (x - sourceCoordinate) - 1
                    return localCoordinate
                }
                else {
                    localCoordinate += (x - y)
                }
            }
        }
        // if it gets here, that means the coordinate was in an intron
        // TODO: better way to do this
        return -1
    }

    /**
     * Given a local coordinate, transforms the coordinate into global context, accounting for CDS
     * @param cdsFmin
     * @param cdsFmax
     * @param exonFminArray
     * @param exonFmaxArray
     * @param strand
     * @param localCoordinate
     * @return
     */
    def convertLocalCoordinateToSourceCoordinateForCDS(int cdsFmin, int cdsFmax, List exonFminArray, List exonFmaxArray, int strand, int localCoordinate) {
        int sourceCoordinate = -1
        int currentLength = 0
        int currentCoordinate = localCoordinate

        // Ensuring that the arrays have coordinates sorted in the right order
        if (strand == Strand.NEGATIVE.value) {
            exonFminArray.sort(true, {a, b -> b <=> a})
            exonFmaxArray.sort(true, {a, b -> b <=> a})
        }
        else {
            exonFminArray.sort(true, {a, b -> a <=> b})
            exonFmaxArray.sort(true, {a, b -> a <=> b})
        }

        int x, y =  0
        for (int i = 0; i < exonFminArray.size(); i++) {
            int exonFmin = exonFminArray.get(i)
            int exonFmax = exonFmaxArray.get(i)

            x = Math.max(exonFmin, cdsFmin)
            y = Math.min(exonFmax, cdsFmax)
            int segmentLength = y - x
            if (currentLength + segmentLength >= localCoordinate) {
                log.info "LocalCoordinate falls within segment ${x} - ${y}"
                if (strand == Strand.NEGATIVE.value) {
                    sourceCoordinate = (y - currentCoordinate) - 1
                }
                else {
                    sourceCoordinate = x + currentCoordinate
                }
                break
            }
            else {
                currentLength += segmentLength
                currentCoordinate -= segmentLength
            }
        }
        return sourceCoordinate
    }

    /**
     *
     * @param pos
     * @param sequence
     * @return
     */
    def getCodonFromSequence(int pos, String sequence) {
        String codon
        int aaPosition = Math.ceil(pos / 3)
        if (pos % 3 == 0) {
            // [X]YZ
            log.info "mod 0"
            codon = sequence.substring(pos, pos + 3)
        }
        else if (pos % 3 == 1) {
            // X[Y]Z
            log.info "mod 1"
            codon = sequence.substring(pos - 1, pos + 2)
        }
        else if (pos % 3 == 2) {
            // XY[Z]
            log.info "mod 2"
            codon = sequence.substring(pos - 2, pos + 1)
        }
        return [codon, aaPosition]
    }

    /**
     *
     * @param leftFmin
     * @param leftFmax
     * @param rightFmin
     * @param rightFmax
     * @return
     */
    boolean overlaps(int leftFmin, int leftFmax,int rightFmin,int rightFmax) {
        return (leftFmin <= rightFmin && leftFmax > rightFmin ||
                leftFmin >= rightFmin && leftFmin < rightFmax)
    }

}
