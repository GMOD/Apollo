package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.alteration.AlterationNode
import org.bbop.apollo.alteration.LocationInfo
import org.bbop.apollo.alteration.OverlapInfo
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.StandardTranslationTable
import org.bbop.apollo.sequence.Strand
import org.bbop.apollo.sequence.TranslationTable
import org.codehaus.groovy.grails.web.json.JSONArray
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
    def sequenceTrace = []
    //def sequenceTrace = [{FeatureStringEnum.TYPE_GENOMIC.value = []}, {FeatureStringEnum.TYPE_CDNA.value = []}, {FeatureStringEnum.TYPE_CDS.value = []}]

    /**
     * Given a source coordinate, transform the coordinate in the context of a given feature.
     * @param fmin
     * @param fmax
     * @param strand
     * @param sourceCoordinate
     * @return
     */
    def convertSourceCoordinateToLocalCoordinateNew(int fmin, int fmax, int strand, int sourceCoordinate) {
        log.info "convertSourceCoordinateToLocalCoordinate: ${fmin} ${fmax} ${strand} ${sourceCoordinate}"
        // fmin and fmax are in global context
        if (sourceCoordinate < fmin || sourceCoordinate > fmax) {
            // sourceCoordinate never falls within the feature
            log.info "sourceCoordinate never falls within the feature; return -1"
            return -1
        }

        return sourceCoordinate - fmin
    }

    /**
     * Given a local coordinate, transform the coordinate into global context
     * TODO: remove
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

    def convertLocalCoordinateToSourceCoordinateForTranscript(List exonFminArray, List exonFmaxArray, int localCoordinate) {
        println "[ convertLocalCoordinateToSourceCoordinateForTranscript ] ${exonFminArray} ${exonFmaxArray} ${localCoordinate}"
        int sourceCoordinate = -1
        int currentLength = 0
        int currentCoordinate = localCoordinate

        // Ensuring that the arrays have coordinates sorted in the right order
        exonFminArray.sort(true, {a, b -> a <=> b})
        exonFmaxArray.sort(true, {a, b -> a <=> b})

        for (int i = 0; i < exonFminArray.size(); i++) {
            int exonFmin = exonFminArray.get(i)
            int exonFmax = exonFmaxArray.get(i)
            int exonLength = exonFmax - exonFmin

            if (currentLength + exonLength >= localCoordinate) {
                println "[ convertLocalCoordinateToSourceCoordinateForTranscript ] localCoordinate falls within exon ${exonFmin} - ${exonFmax}"
                sourceCoordinate = exonFmin + currentCoordinate
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

    def isVariantWithinExons(SequenceAlteration variant, def exons) {
        for (Exon exon : exons) {
            if ((variant.fmin >= exon.fmin && variant.fmin <= exon.fmax) || variant.fmax >= exon.fmin && variant.fmin <= exon.fmax) {
                // variant either falls completely within the exon OR partially within the exon
                return true
            }
            return false
        }
    }

    def createVariantEffectMetadataJSON(Feature feature, SequenceAlteration variant, Allele allele, VariantEffect variantEffect) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.feature = featureService.convertFeatureToJSON(feature)
        jsonObject.variant = featureService.convertFeatureToJSON(variant)
        jsonObject.alternate_allele = allele.bases
        jsonObject.type = new JSONArray()
        variantEffect.effects.each {
            jsonObject.getJSONArray("type").add(it.cvTerm)
        }
        jsonObject.put("type", variantEffect.effects.first().cvTerm)
        println ">>>> [ createVariantEffectMetadataJSON ] JSONObject: ${jsonObject.toString()}"
        return jsonObject.toString()
    }

    def getOverlappingFeatures(String uniqueName, Sequence sequence, int fmin, int fmax, int strand, boolean compareStrands = false, boolean includeSequenceAlterations = false) {
        if (includeSequenceAlterations) {
            if (compareStrands) {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and f.uniqueName != :featureUniqueName",
                        [fmin: fmin, fmax: fmax, strand: strand, sequence: sequence, featureUniqueName: uniqueName]
                )
            }
            else {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and f.uniqueName != :featureUniqueName",
                        [fmin: fmin, fmax: fmax, sequence: sequence, featureUniqueName: uniqueName]
                )
            }
        }
        else {
            if (compareStrands) {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax) and f.class not in :featureTypes)  and f.uniqueName != :featureUniqueName",
                        [fmin: fmin, fmax: fmax, strand: strand, sequence: sequence, featureUniqueName: uniqueName, featureTypes: [Insertion.class.name, Deletion.class.name, Substitution.class.name]]
                )
            }
            else {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and f.class not in :featureTypes  and f.uniqueName != :featureUniqueName",
                        [fmin: fmin, fmax: fmax, sequence: sequence, featureUniqueName: uniqueName, featureTypes: [Insertion.class.name, Deletion.class.name, Substitution.class.name]]
                )
            }
        }
    }

    def getSequenceAlterations(String uniqueName, Sequence sequence, int fmin, int fmax, boolean includeVariants) {
        if (includeVariants) {
            SequenceAlteration.executeQuery(
                    "select distinct s from SequenceAlteration s join s.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and s.uniqueName != :featureUniqueName",
                    [fmin: fmin, fmax: fmax, sequence: sequence, featureUniqueName: uniqueName]
            )
        }
        else {
            SequenceAlteration.executeQuery(
                    "select distinct s from SequenceAlteration s join s.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and s.alterationType not in :featureTypes  and s.uniqueName != :featureUniqueName",
                    [fmin: fmin, fmax: fmax, sequence: sequence, featureUniqueName: uniqueName, featureTypes: [FeatureStringEnum.VARIANT.value]]
            )
        }
    }

    def calculateEffectOfVariant(SequenceAlteration variant) {
        println "[ calculateEffectOfVariant ] [ SA ]"
        int FLANKSIZE = 1000
        def overlappingFeatures = getOverlappingFeatures(variant.uniqueName, variant.featureLocation.sequence, variant.fmin - FLANKSIZE, variant.fmax + FLANKSIZE, variant.strand, false, false)
        def nearbyFeatures = []
        if (overlappingFeatures.size() == 0) {
            nearbyFeatures = getOverlappingFeatures(variant.uniqueName, variant.featureLocation.sequence, variant.fmin - FLANKSIZE, variant.fmax + FLANKSIZE, variant.strand, false, false)
        }

        println "[ calculateEffectOfVariant ] [ SA ] overlapping features: ${overlappingFeatures.cvTerm}"
        println "[ calculateEffectOfVariant ] [ SA ] nearby features: ${nearbyFeatures.cvTerm}"
        def overlappingAndNearbyFeatures = overlappingFeatures + nearbyFeatures
        overlappingAndNearbyFeatures.sort{ a,b -> a.fmin <=> b.fmin }

        def overlappingAndNearbyTranscriptFeatures = []
        overlappingAndNearbyFeatures.each {
            if (it instanceof Transcript) overlappingAndNearbyTranscriptFeatures.add(it)
        }

        if (overlappingAndNearbyTranscriptFeatures) {
            int regionFmin = overlappingAndNearbyTranscriptFeatures.first().fmin
            int regionFmax = overlappingAndNearbyTranscriptFeatures.last().fmax

            def sequenceAlterations = getSequenceAlterations(variant.uniqueName, variant.featureLocation.sequence, regionFmin, regionFmax, false)

            sequenceAlterations.sort{ a,b -> a.fmin <=> b.fmin }
            println "[ calculateEffectOfVariant ] overlapping and nearby transcript features: ${overlappingAndNearbyTranscriptFeatures.cvTerm}"
            println "[ calculateEffectOfVariant ] assembly error corrections: ${sequenceAlterations.cvTerm}"
            predictEffectOfVariantOnTranscripts(overlappingAndNearbyTranscriptFeatures, sequenceAlterations, variant)
        }
    }

    def predictEffectOfVariantOnTranscripts(def transcripts, def sequenceAlterations, SequenceAlteration variant) {
        println "[ predictEffectOfVariantOnTranscripts ] [ Fs-SAs-V ]"

        // first create an alteration representation with ONLY AECs
        def alterationNodeList1 = createAlterationRepresentation(transcripts, sequenceAlterations)
        //println "[ predictEffectOfVariantOnTranscripts ] [ Fs-SAs-V ] ALTERATION NODE LIST 1: ${alterationNodeList1.toString()}"
        if (alterationNodeList1) testAlterationNodeForAssemblyErrorCorrection(alterationNodeList1.last())

        // then create an alteration representation with AECs + variant
        def allAlterations = sequenceAlterations + variant
        allAlterations.sort { a,b -> a.fmin <=> b.fmin }
        def alterationNodeList2 = createAlterationRepresentation(transcripts, allAlterations)
        //println "[ predictEffectOfVariantOnTranscripts ] [ Fs-SAs-V ] ALTERATION NODE LIST 2: ${alterationNodeList2.toString()}"
        testAlterationNodeForVariant(alterationNodeList2.last())
        inferVariantEffects(variant.uniqueName, alterationNodeList2)
    }

    def createAlterationRepresentation(ArrayList<Feature> features, ArrayList<SequenceAlteration> sequenceAlterations) {
        println "[ createAlterationRepresentation ][ Fs-SAs ] ${sequenceAlterations.alternateCvTerm}"
        def alterationNodeList = []
        AlterationNode PREV = null
        for (SequenceAlteration sequenceAlteration : sequenceAlterations) {
            AlterationNode alterationNode
            if (PREV) {
                if (sequenceAlteration.alterationType == FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value) {
                    println "[ createAlterationRepresentation ][ Fs-SAs ] 1.1"
                    alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(sequenceAlteration, PREV)
                }
                else {
                    // TODO: This is a hack
                    println "[ createAlterationRepresentation ][ Fs-SAs ] 1.2"
                    alterationNode = createAlterationRepresentationForVariant(sequenceAlteration, PREV).first()
                }
            }
            else {
                if (sequenceAlteration.alterationType == FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value) {
                    println "[ createAlterationRepresentation ][ Fs-SAs ] 2.1"
                    alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(features, sequenceAlteration)
                }
                else {
                    println "[ createAlterationRepresentation ][ Fs-SAs ] 2.2"
                    // TODO: This is a hack
                    alterationNode = createAlterationRepresentationForVariant(features, sequenceAlteration).first()
                }
            }
            PREV = alterationNode
            alterationNodeList.add(alterationNode)
        }
        return alterationNodeList
    }

    def testAlterationNodeForAssemblyErrorCorrection(AlterationNode alterationNode) {
        for (OverlapInfo overlapInfo : alterationNode.overlapInfo) {
            Feature feature = Feature.findByUniqueName(overlapInfo.uniquename)
            String apolloCdnaSeq = sequenceService.getSequenceForFeature(feature, FeatureStringEnum.TYPE_CDNA.value)
            StringBuilder genomicSeq = new StringBuilder(overlapInfo.modLocationSeq)
            println "[ testAlterationNodeForAssemblyErrorCorrection ] GenomicSeq: ${genomicSeq}"
            String testCdnaSeq = ""
            String finalCdnaSeq = ""
            for (OverlapInfo child : overlapInfo.children) {
                println "[ testAlterationNodeForAssemblyErrorCorrection ] Fetching substr ${child.modLocalLocation.fmin} - ${child.modLocalLocation.fmax}"
                testCdnaSeq += genomicSeq.substring(child.modLocalLocation.fmin, child.modLocalLocation.fmax)
            }
            if (feature.strand == Strand.NEGATIVE.value) {
                finalCdnaSeq = SequenceTranslationHandler.reverseComplementSequence(testCdnaSeq)
            }
            else {
                finalCdnaSeq = testCdnaSeq
            }
            println "[ testAlterationNodeForAssemblyErrorCorrection ] Apollo CDNA Seq: ${apolloCdnaSeq}"
            println "[ testAlterationNodeForAssemblyErrorCorrection ] Final  CDNA Seq: ${finalCdnaSeq}"
            sequenceTrace.add(finalCdnaSeq)
        }
    }

    def testAlterationNodeForVariant(AlterationNode alterationNode) {
        boolean pass = false
        for (OverlapInfo overlapInfo : alterationNode.overlapInfo) {
            Feature feature = Feature.findByUniqueName(overlapInfo.uniquename)
            StringBuilder genomicSeq = new StringBuilder(overlapInfo.modLocationSeq)
            String testCdnaSeq = ""
            String finalCdnaSeq = ""
            for (OverlapInfo child : overlapInfo.children) {
                println "[ testAlterationNodeForVariant ] Fetching substr ${child.modLocalLocation.fmin} - ${child.modLocalLocation.fmax}"
                testCdnaSeq += genomicSeq.substring(child.modLocalLocation.fmin, child.modLocalLocation.fmax)
            }
            if (feature.strand == Strand.NEGATIVE.value) {
                finalCdnaSeq = SequenceTranslationHandler.reverseComplementSequence(testCdnaSeq)
            }
            else {
                finalCdnaSeq = testCdnaSeq
            }
            println "[ testAlterationNodeForVariant ] Final CDNA Seq with variant: ${finalCdnaSeq}"
            sequenceTrace.add(finalCdnaSeq)
        }
    }

    def createAlterationRepresentationForVariant(SequenceAlteration variant, AlterationNode PREV) {
        println "[ createAlterationRepresentationForVariant ] [ 1V-1P ]"
        def alternateAlleles = variant.alternateAlleles
        def alterationNodeList = []

        for (Allele allele : alternateAlleles) {
            AlterationNode alterationNode = new AlterationNode(variant, allele)
            alterationNode.cumulativeOffset = PREV.cumulativeOffset + PREV.offset
            alterationNode.fmin = alterationNode.fmin + alterationNode.cumulativeOffset
            alterationNode.fmax = alterationNode.fmax + alterationNode.cumulativeOffset
            def currentOverlapInfoList = []
            def previousOverlapInfoList = PREV.overlapInfo
            for (int i = 0; i < previousOverlapInfoList.size(); i++) {
                // top level
                OverlapInfo previousOverlapInfo = previousOverlapInfoList.get(i)
                OverlapInfo overlapInfo = previousOverlapInfo.generateClone()
                overlapInfo = updateOverlapInfoWithVariant(alterationNode, overlapInfo, Feature.findByUniqueName(overlapInfo.uniquename), variant, allele, true)

                def currentChildOverlapInfoList = []
                if (previousOverlapInfo.children) {
                    for (int j = 0; j < previousOverlapInfo.children.size(); j++) {
                        OverlapInfo previousChildOverlapInfo = previousOverlapInfo.children.get(j)
                        OverlapInfo childOverlapInfo = previousChildOverlapInfo.generateClone()
                        childOverlapInfo = updateOverlapInfoWithVariant(alterationNode, childOverlapInfo, Feature.findByUniqueName(childOverlapInfo.uniquename), variant, allele, false)
                        currentChildOverlapInfoList.add(childOverlapInfo)
                    }
                    overlapInfo.children = currentChildOverlapInfoList
                }
                currentOverlapInfoList.add(overlapInfo)
            }
            alterationNode.overlapInfo = currentOverlapInfoList
            alterationNodeList.add(alterationNode)
        }

        return alterationNodeList

    }

    def createAlterationRepresentationForVariant(ArrayList<Feature> features, SequenceAlteration variant) {
        println "[ createAlterationRepresentationForVariant ][ FAs-1V ]"
        def alternateAlleles = variant.alternateAlleles
        def alterationNodeList = []

        for (Allele allele : alternateAlleles) {
            AlterationNode alterationNode = new AlterationNode(variant, allele)
            def overlapInfoList = []
            for (Feature feature : features) {
                OverlapInfo overlapInfo = createOverlapInfoWithVariant(feature, variant, allele)
                def exons = transcriptService.getSortedExons(feature, false)

                def exonOverlapInfoList = []
                for (Exon exon : exons) {
                    OverlapInfo exonOverlapInfo = createOverlapInfoWithVariant(exon, variant, allele, feature, false)
                    exonOverlapInfoList.add(exonOverlapInfo)
                }
                overlapInfo.children = exonOverlapInfoList
                overlapInfoList.add(overlapInfo)
            }
            alterationNode.overlapInfo = overlapInfoList
            alterationNodeList.add(alterationNode)
        }

        // alterationNodeList will commonly be of size 1 and in few cases of size 'n', where is n(alternateAlleles)
        return alterationNodeList
    }

//    def createAlterationRepresentationForVariant(Feature feature, def variants) {
//        println "[ createAlterationRepresentationForVariant ] ${feature.uniqueName} ${variants.cvTerm}"
//        // the assumption is that variants list is sorted by fmin and there are no overlapping variants
//        def alterationNodeList = []
//        int cumulativeOffset = 0
//        AlterationNode PREV = null
//        for (SequenceAlteration variant : variants) {
//            // for each variant
//            for (Allele allele : variant.alternateAlleles) {
//                // for each allele of a variant
//                // TODO: revisit this later after a dealing with single variant
//            }
//        }
//    }


    def createAlterationRepresentationForAssemblyErrorCorrection(ArrayList<Feature> features, ArrayList<SequenceAlteration> sequenceAlterations) {
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ Fs-SAs ]"
        def alterationNodeList = []
        AlterationNode PREV = null
        for (SequenceAlteration sequenceAlteration : sequenceAlterations) {
            println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ Fs-SAs ] processing SequenceAlteration: ${sequenceAlteration.fmin} ${sequenceAlteration.class}"
            AlterationNode alterationNode
            if (PREV) {
                alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(sequenceAlteration, PREV)
            }
            else {
                alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(features, sequenceAlteration)
            }
            alterationNodeList.add(alterationNode)
            PREV = alterationNode
        }

        return alterationNodeList
    }

    def createAlterationRepresentationForAssemblyErrorCorrection(ArrayList<Feature> features, SequenceAlteration sequenceAlteration) {
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ Fs-1SA ]"

        AlterationNode alterationNode = new AlterationNode(sequenceAlteration)
        def overlapInfoList = []
        for (Feature feature : features) {
            OverlapInfo overlapInfo = createOverlapInfoWithAssemblyErrorCorrection(feature, sequenceAlteration)
            def exons = transcriptService.getSortedExons(feature, false)
            def exonOverlapInfoList = []
            for (Exon exon : exons) {
                OverlapInfo exonOverlapInfo = createOverlapInfoWithAssemblyErrorCorrection(exon, sequenceAlteration, feature, false)
                exonOverlapInfoList.add(exonOverlapInfo)
            }
            overlapInfo.children = exonOverlapInfoList
            overlapInfoList.add(overlapInfo)
        }
        alterationNode.overlapInfo = overlapInfoList

        return alterationNode
    }

    def createAlterationRepresentationForAssemblyErrorCorrection(Feature feature, def sequenceAlterations) {
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ][ 1F-SAs ]"
        def alterationNodeList = []
        AlterationNode PREV = null
        for (SequenceAlteration sa : sequenceAlterations) {
            AlterationNode alterationNode
            if (PREV) {
                alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(sa, PREV)
            }
            else {
                alterationNode = createAlterationRepresentationForAssemblyErrorCorrection(feature, sa)
            }
            alterationNodeList.add(alterationNode)
            PREV = alterationNode
        }

        testAlterationNodeForAssemblyErrorCorrection(alterationNodeList.last())
    }

    def createAlterationRepresentationForAssemblyErrorCorrection(SequenceAlteration sequenceAlteration, AlterationNode PREV) {
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ][ 1SA-1P ]"

        AlterationNode alterationNode = new AlterationNode(sequenceAlteration)
        alterationNode.cumulativeOffset = PREV.cumulativeOffset + PREV.offset
        alterationNode.fmin = alterationNode.fmin + alterationNode.cumulativeOffset
        alterationNode.fmax = alterationNode.fmax + alterationNode.cumulativeOffset

        def currentOverlapInfoList = []
        def previousOverlapInfoList = PREV.overlapInfo
        for (int i = 0; i < previousOverlapInfoList.size(); i++) {
            // top level
            OverlapInfo previousOverlapInfo = previousOverlapInfoList.get(i)
            OverlapInfo overlapInfo = previousOverlapInfo.generateClone()
            overlapInfo = updateOverlapInfoWithAssemblyErrorCorrection(alterationNode, overlapInfo, Feature.findByUniqueName(overlapInfo.uniquename), sequenceAlteration, true)

            def currentChildOverlapInfo = []
            if (previousOverlapInfo.children) {
                for (int j = 0; j < previousOverlapInfo.children.size(); j++) {
                    OverlapInfo previousChildOverlapInfo = previousOverlapInfo.children.get(j)
                    OverlapInfo childOverlapInfo = previousChildOverlapInfo.generateClone()
                    childOverlapInfo = updateOverlapInfoWithAssemblyErrorCorrection(alterationNode, childOverlapInfo, Feature.findByUniqueName(childOverlapInfo.uniquename), sequenceAlteration, false)
                    currentChildOverlapInfo.add(childOverlapInfo)
                }
                overlapInfo.children = currentChildOverlapInfo
            }
            currentOverlapInfoList.add(overlapInfo)
        }
        alterationNode.overlapInfo = currentOverlapInfoList

        return alterationNode
    }

    def createAlterationRepresentationForAssemblyErrorCorrection(Feature feature, SequenceAlteration sequenceAlteration) {
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ][ 1F-1SA ]"

        def exons = []
        if (feature instanceof Transcript) {
            exons = transcriptService.getSortedExons(feature, false)
        }
        else {
            println "feature instanceof ${feature.class}"
        }

        AlterationNode alterationNode = new AlterationNode(sequenceAlteration)
        alterationNode.cumulativeOffset = 0
        OverlapInfo overlapInfo = createOverlapInfoWithAssemblyErrorCorrection(feature, sequenceAlteration)

        def exonOverlapInfos = []
        for (Exon exon : exons) {
            OverlapInfo exonOverlapInfo = createOverlapInfoWithAssemblyErrorCorrection(exon, sequenceAlteration, feature, false)
            exonOverlapInfos.add(exonOverlapInfo)
        }
        overlapInfo.children = exonOverlapInfos
        alterationNode.overlapInfo = [overlapInfo]

        // TEST
        testAlterationNodeForAssemblyErrorCorrection(alterationNode)

        return alterationNode
    }

    def createAlterationRepresentationForVariant(Feature feature, SequenceAlteration variant) {
        println "[ createAlterationRepresentationForVariant ] [ 1F-1SA ]"
        def alterationNodeList = []
        def exons = []
        if (feature instanceof Transcript) {
            exons = transcriptService.getSortedExons(feature, false)
        }
        else {
            println "[ createAlterationRepresentationForVariant ] [ 1F-1SA ] feature instanceof ${feature.class}"
        }

        for (Allele allele : variant.alternateAlleles) {
            println "[ createAlterationRepresentationForVariant ] [ 1F-1SA ] Creating AlterationNode for Allele ${allele.bases}"
            AlterationNode alterationNode = new AlterationNode(variant, allele)
            OverlapInfo overlapInfo = createOverlapInfoWithVariant(feature, variant, allele)

            def exonOverlapInfoList = []
            for (Exon exon : exons) {
                OverlapInfo exonOverlapInfo = createOverlapInfoWithVariant(exon, variant, allele, feature, false)
                exonOverlapInfoList.add(exonOverlapInfo)
            }
            overlapInfo.children = exonOverlapInfoList
            alterationNode.overlapInfo = [overlapInfo]
            alterationNodeList.add(alterationNode)
        }
        return alterationNodeList
    }


    // TODO: move the block shared by createOverlap* And updateOverlap* to a separate common function
    def createOverlapInfoWithAssemblyErrorCorrection(Feature feature, SequenceAlteration sequenceAlteration, Feature parentFeature = null, boolean getSequence = true) {
        println "[ createOverlapInfoWithAssemblyErrorCorrection ]"
        // TODO: inference
        int alterationOffset = getAlterationOffset(sequenceAlteration)
        String alterationResidue = sequenceAlteration.alterationResidue
        println "[ createOverlapInfoWithAssemblyErrorCorrection ] ALTERATION OFFSET: ${alterationOffset}"
        Boolean isModified = false
        Boolean overlaps = false
        Boolean isUpstream = false
        Boolean isDownstream = false

        OverlapInfo overlapInfo = new OverlapInfo()
        overlapInfo.uniquename = feature.uniqueName
        overlapInfo.strand = feature.strand
        overlapInfo.location = new LocationInfo(feature.fmin, feature.fmax)

        int localFmin, localFmax
        if (parentFeature) {
            localFmin = convertSourceCoordinateToLocalCoordinateNew(parentFeature.fmin, parentFeature.fmax, feature.strand, feature.fmin)
            localFmax = convertSourceCoordinateToLocalCoordinateNew(parentFeature.fmin, parentFeature.fmax, feature.strand, feature.fmax)
        }
        else {
            localFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, feature.fmin)
            localFmax = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, feature.fmax)
        }

        println "[ createOverlapInfoWithAssemblyErrorCorrection ] localFmin: ${localFmin} localFmax: ${localFmax}"
        overlapInfo.localLocation = new LocationInfo(localFmin, localFmax)

        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax

        if (sequenceAlteration.fmin < feature.fmin) {
            // SA is upstream of feature
            println "[ createOverlapInfoWithAssemblyErrorCorrection ] SA is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = feature.fmin + alterationOffset
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = feature.fmin - alterationOffset
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (feature.strand == Strand.POSITIVE.value) {
                if (sequenceAlteration instanceof Insertion) {
                    modifiedLocalFmin = localFmin + alterationOffset
                    modifiedLocalFmax = localFmax + alterationOffset
                }
                else if (sequenceAlteration instanceof Deletion) {
                    modifiedLocalFmin = localFmin - alterationOffset
                    modifiedLocalFmax = localFmax - alterationOffset
                }
                else if (sequenceAlteration instanceof Substitution) {
                    // no change
                    modifiedLocalFmin = localFmin
                    modifiedLocalFmax = localFmax
                }
            }
            else {
                // SA is actually downstream in local context
                if (sequenceAlteration instanceof Insertion) {
                    modifiedLocalFmin = localFmin + alterationOffset
                    modifiedLocalFmax = localFmax + alterationOffset
                }
                else if (sequenceAlteration instanceof Deletion) {
                    modifiedLocalFmin = localFmin - alterationOffset
                    modifiedLocalFmax = localFmax - alterationOffset
                }
                else if (sequenceAlteration instanceof Substitution) {
                    // no change
                    modifiedLocalFmin = localFmin
                    modifiedLocalFmax = localFmax
                }
            }


        }
        else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmin < feature.fmax) {
            // SA is within feature
            println "[ createOverlapInfoWithAssemblyErrorCorrection ] SA is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (sequenceAlteration instanceof Insertion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmin > feature.fmax) {
            // SA is downstream of feature
            // no change
            println "[ createOverlapInfoWithAssemblyErrorCorrection ] SA is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = feature.fmin
            modifiedFmax = feature.fmax
            modifiedLocalFmin = localFmin
            modifiedLocalFmax = localFmax
        }
        else {
            // TODO
            println "[ createOverlapInfoWithAssemblyErrorCorrection ] TODO: sequenceAlteration is right on the boundary"
        }

        overlapInfo.overlaps = overlaps
        overlapInfo.isUpstream = isUpstream
        overlapInfo.isDownstream = isDownstream
        overlapInfo.isModified = isModified

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            // getSequence is to be true only for top level transcript feature
            String locationSeq = sequenceService.getRawResiduesFromSequence(feature.featureLocation.sequence, feature.fmin, feature.fmax)
            overlapInfo.locationSeq = locationSeq
            StringBuilder builder = new StringBuilder(locationSeq)

            if (sequenceAlteration.fmin < feature.fmin) {
                // SA is upstream of feature
            }
            else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmax < feature.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, sequenceAlteration.fmin)
                println "[ createOverlapInfoWithAssemblyErrorCorrection ] ALTERATION LOCAL FMIN: ${alterationLocalFmin}"
                if (sequenceAlteration instanceof Insertion) {
                    builder.insert(alterationLocalFmin, alterationResidue)
                }
                else if (sequenceAlteration instanceof  Deletion) {
                    builder.delete(alterationLocalFmin, alterationLocalFmin + alterationOffset)
                }
                else if (sequenceAlteration instanceof Substitution) {
                    builder.replace(alterationLocalFmin, alterationLocalFmin + alterationResidue.length(), alterationResidue)
                }
            }
            else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmax > feature.fmax) {
                // SA is downstream of feature
                // no change
            }

            String modLocationSeq = builder.toString()
            overlapInfo.modLocationSeq = modLocationSeq

        }

        return overlapInfo
    }

    def createOverlapInfoWithVariant(Feature feature, SequenceAlteration variant, Allele allele, Feature parentFeature = null, boolean getSequence = true) {
        // TODO: inference
        int alterationOffset = getAlterationOffset(variant, allele)
        String alterationResidue = allele.bases // allele.bases includes the anchor
        println "[ createOverlapInfoWithVariant ] ALTERATION OFFSET: ${alterationOffset}"
        Boolean isModified = false
        Boolean overlaps = false
        Boolean isUpstream = false
        Boolean isDownstream = false

        OverlapInfo overlapInfo = new OverlapInfo()
        overlapInfo.uniquename = feature.uniqueName
        overlapInfo.strand = feature.strand
        overlapInfo.location = new LocationInfo(feature.fmin, feature.fmax)

        int localFmin, localFmax
        if (parentFeature) {
            localFmin = convertSourceCoordinateToLocalCoordinateNew(parentFeature.fmin, parentFeature.fmax, feature.strand, feature.fmin)
            localFmax = convertSourceCoordinateToLocalCoordinateNew(parentFeature.fmin, parentFeature.fmax, feature.strand, feature.fmax)
        }
        else {
            localFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, feature.fmin)
            localFmax = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, feature.fmax)
        }

        println "[ createOverlapInfoWithVariant ] localFmin: ${localFmin} localFmax: ${localFmax}"
        overlapInfo.localLocation = new LocationInfo(localFmin, localFmax)

        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax

        if (variant.fmin < feature.fmin) {
            // SA is upstream of feature
            println "[ createOverlapInfoWithVariant ] SA is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (variant instanceof Insertion) {
                modifiedFmin = feature.fmin + alterationOffset
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = feature.fmin - alterationOffset
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (feature.strand == Strand.POSITIVE.value) {
                if (variant instanceof Insertion) {
                    modifiedLocalFmin = localFmin + alterationOffset
                    modifiedLocalFmax = localFmax + alterationOffset
                }
                else if (variant instanceof Deletion) {
                    modifiedLocalFmin = localFmin - alterationOffset
                    modifiedLocalFmax = localFmax - alterationOffset
                }
                else if (variant instanceof Substitution) {
                    // no change
                    modifiedLocalFmin = localFmin
                    modifiedLocalFmax = localFmax
                }
            }
            else {
                // SA is actually downstream in local context
                if (variant instanceof Insertion) {
                    modifiedLocalFmin = localFmin + alterationOffset
                    modifiedLocalFmax = localFmax + alterationOffset
                }
                else if (variant instanceof Deletion) {
                    modifiedLocalFmin = localFmin - alterationOffset
                    modifiedLocalFmax = localFmax - alterationOffset
                }
                else if (variant instanceof Substitution) {
                    // no change
                    modifiedLocalFmin = localFmin
                    modifiedLocalFmax = localFmax
                }
            }


        }
        else if (variant.fmin > feature.fmin && variant.fmin < feature.fmax) {
            // SA is within feature
            println "[ createOverlapInfoWithVariant ] SA is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (variant instanceof Insertion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (variant instanceof Insertion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (variant.fmin > feature.fmin && variant.fmin > feature.fmax) {
            // SA is downstream of feature
            // no change
            println "[ createOverlapInfoWithVariant ] SA is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = feature.fmin
            modifiedFmax = feature.fmax
            modifiedLocalFmin = localFmin
            modifiedLocalFmax = localFmax
        }
        else {
            // TODO
            println "[ createOverlapInfoWithVariant ] TODO: sequenceAlteration is right on the boundary"
        }

        overlapInfo.overlaps = overlaps
        overlapInfo.isUpstream = isUpstream
        overlapInfo.isDownstream = isDownstream
        overlapInfo.isModified = isModified

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            // getSequence is to be true only for top level transcript feature
            String locationSeq = sequenceService.getRawResiduesFromSequence(feature.featureLocation.sequence, feature.fmin, feature.fmax)
            overlapInfo.locationSeq = locationSeq
            StringBuilder builder = new StringBuilder(locationSeq)

            if (variant.fmin < feature.fmin) {
                // SA is upstream of feature
            }
            else if (variant.fmin > feature.fmin && variant.fmax < feature.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, variant.fmin)
                int alterationLocalFmax = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, variant.fmax)
                println "[ createOverlapInfoWithVariant ] ALTERATION LOCAL FMIN: ${alterationLocalFmin}"
                if (variant instanceof Insertion) {
                    println "[ createOverlapInfoWithVariant ][ INS ] at index ${alterationLocalFmin}-${alterationLocalFmin + 1}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmin + 1, alterationResidue)
                }
                else if (variant instanceof  Deletion) {
                    println "[ createOverlapInfoWithVariant ][ DEL ] at index ${alterationLocalFmin}-${alterationLocalFmax}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmax, alterationResidue)
                }
                else if (variant instanceof Substitution) {
                    println "[ createOverlapInfoWithVariant ][ SUB ] at index ${alterationLocalFmin}-${alterationLocalFmin + alterationResidue.length()}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmin + alterationResidue.length(), alterationResidue)
                }
            }
            else if (variant.fmin > feature.fmin && variant.fmax > feature.fmax) {
                // SA is downstream of feature
                // no change
            }

            String modLocationSeq = builder.toString()
            overlapInfo.modLocationSeq = modLocationSeq

        }

        return overlapInfo
    }

    def updateOverlapInfoWithAssemblyErrorCorrection(AlterationNode alterationNode, OverlapInfo overlapInfo, Feature feature, SequenceAlteration sequenceAlteration, boolean getSequence = true) {
        // TODO: inference
        println "[ updateOverlapInfoWithAssemblyErrorCorrection ]"
        int alterationOffset = getAlterationOffset(sequenceAlteration)
        String alterationResidue = sequenceAlteration.alterationResidue
        println "[ updateOverlapInfoWithAssemblyErrorCorrection ] ALTERATION OFFSET: ${alterationOffset}"
        boolean isModified, isUpstream, isDownstream, overlaps
        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax
        int localFmin = overlapInfo.localLocation.fmin
        int localFmax = overlapInfo.localLocation.fmax

        if (alterationNode.fmin < feature.fmin) {
            // SA is upstream of feature
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = feature.fmin + alterationOffset
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = feature.fmin - alterationOffset
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (sequenceAlteration instanceof Insertion) {
                modifiedLocalFmin = localFmin + alterationOffset
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedLocalFmin = localFmin - alterationOffset
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (alterationNode.fmin > feature.fmin && alterationNode.fmin < feature.fmax) {
            // SA is within feature
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (sequenceAlteration instanceof Insertion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (alterationNode.fmin > feature.fmin && alterationNode.fmin > feature.fmax) {
            // SA is downstream of feature
            // no change
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = feature.fmin
            modifiedFmax = feature.fmax
            modifiedLocalFmin = localFmin
            modifiedLocalFmax = localFmax
        }
        else {
            // TODO
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] TODO: sequenceAlteration is right on the boundary"
        }

        overlapInfo.overlaps = overlaps
        overlapInfo.isUpstream = isUpstream
        overlapInfo.isDownstream = isDownstream

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            StringBuilder builder = new StringBuilder(overlapInfo.locationSeq)

            if (alterationNode.fmin < feature.fmin) {
                // SA is upstream of feature
            }
            else if (alterationNode.fmin > feature.fmin && alterationNode.fmax < feature.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, alterationNode.fmin)
                if (sequenceAlteration instanceof Insertion) {
                    builder.insert(alterationLocalFmin, alterationResidue)
                }
                else if (sequenceAlteration instanceof  Deletion) {
                    builder.delete(alterationLocalFmin, alterationLocalFmin + alterationOffset)
                }
                else {
                    builder.replace(alterationLocalFmin, alterationLocalFmin + alterationResidue.length(), alterationResidue)
                }
            }
            else if (alterationNode.fmin > feature.fmin && alterationNode.fmax > feature.fmax) {
                // SA is downstream of feature
                // no change
            }
            overlapInfo.modLocationSeq = builder.toString()
        }

        return overlapInfo
    }

    def updateOverlapInfoWithVariant(AlterationNode alterationNode, OverlapInfo overlapInfo, Feature feature, SequenceAlteration variant, Allele allele, boolean getSequence = true) {
        // TODO: inference
        println "[ updateOverlapInfoWithVariant ]"
        int alterationOffset = getAlterationOffset(variant, allele)
        String alterationResidue = allele.bases
        println "[ updateOverlapInfoWithVariant ] ALTERATION OFFSET: ${alterationOffset}"
        boolean isModified, isUpstream, isDownstream, overlaps
        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax
        int localFmin = overlapInfo.localLocation.fmin
        int localFmax = overlapInfo.localLocation.fmax

        if (alterationNode.fmin < feature.fmin) {
            // SA is upstream of feature
            println "[ updateOverlapInfoWithVariant ] variant is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (variant instanceof Insertion) {
                modifiedFmin = feature.fmin + alterationOffset
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = feature.fmin - alterationOffset
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (variant instanceof Insertion) {
                modifiedLocalFmin = localFmin + alterationOffset
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedLocalFmin = localFmin - alterationOffset
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (alterationNode.fmin > feature.fmin && alterationNode.fmin < feature.fmax) {
            // SA is within feature
            println "[ updateOverlapInfoWithVariant ] variant is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (variant instanceof Insertion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = feature.fmin
                modifiedFmax = feature.fmax
            }

            if (variant instanceof Insertion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedLocalFmin = localFmin
                modifiedLocalFmax = localFmax
            }
        }
        else if (alterationNode.fmin > feature.fmin && alterationNode.fmin > feature.fmax) {
            // SA is downstream of feature
            // no change
            println "[ updateOverlapInfoWithVariant ] variant is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = feature.fmin
            modifiedFmax = feature.fmax
            modifiedLocalFmin = localFmin
            modifiedLocalFmax = localFmax
        }
        else {
            // TODO
            println "[ updateOverlapInfoWithVariant ] TODO: variant is right on the boundary"
        }

        overlapInfo.overlaps = overlaps
        overlapInfo.isUpstream = isUpstream
        overlapInfo.isDownstream = isDownstream

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            StringBuilder builder = new StringBuilder(overlapInfo.locationSeq)

            if (alterationNode.fmin < feature.fmin) {
                // SA is upstream of feature
            }
            else if (alterationNode.fmin > feature.fmin && alterationNode.fmax < feature.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, alterationNode.fmin)
                int alterationLocalFmax = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, alterationNode.fmax)
                println "[ createOverlapInfoWithVariant ] ALTERATION LOCAL FMIN: ${alterationLocalFmin}"
                if (variant instanceof Insertion) {
                    println "[ createOverlapInfoWithVariant ][ INS ] at index ${alterationLocalFmin}-${alterationLocalFmin + 1}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmin + 1, alterationResidue)
                }
                else if (variant instanceof  Deletion) {
                    println "[ createOverlapInfoWithVariant ][ DEL ] at index ${alterationLocalFmin}-${alterationLocalFmax}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmax, alterationResidue)
                }
                else if (variant instanceof Substitution) {
                    println "[ createOverlapInfoWithVariant ][ SUB ] at index ${alterationLocalFmin}-${alterationLocalFmin + alterationResidue.length()}, replacing with ${alterationResidue}"
                    builder.replace(alterationLocalFmin, alterationLocalFmin + alterationResidue.length(), alterationResidue)
                }
            }
            else if (alterationNode.fmin > feature.fmin && alterationNode.fmax > feature.fmax) {
                // SA is downstream of feature
                // no change
            }
            overlapInfo.modLocationSeq = builder.toString()
        }

        return overlapInfo
    }

    def getAlterationOffset(SequenceAlteration sequenceAlteration) {
        int offset
        if (sequenceAlteration instanceof Insertion) {
            offset = sequenceAlteration.alterationResidue.length()
        }
        else if (sequenceAlteration instanceof Deletion) {
            offset = sequenceAlteration.fmax - sequenceAlteration.fmin
        }
        else if (sequenceAlteration instanceof Deletion) {
            offset = 0
        }

        return offset
    }

    def getAlterationOffset(SequenceAlteration variant, Allele allele) {
        int offset
        if (variant instanceof Insertion) {
            offset = allele.alterationResidue.length()
        }
        else if (variant instanceof Deletion) {
            offset = allele.alterationResidue.length()
        }
        else if (variant instanceof Deletion) {
            offset = 0
        }

        return offset
    }

//    def inferVariantEffects(String variantUniqueName, def alterationNodeList) {
//        // temporary method that creates a generic VariantEffect
//        AlterationNode variantAlterationNode
//        for (AlterationNode alterationNode : alterationNodeList) {
//            if (alterationNode.uniquename == variantUniqueName) {
//                variantAlterationNode = alterationNode
//                break
//            }
//        }
//        AlterationNode finalAlterationNode = alterationNodeList.last()
//        createVariantEffects(variantAlterationNode, finalAlterationNode)
//    }


    def inferVariantEffects(String variantUniqueName, def alterationNodeList) {
        AlterationNode variantAlterationNode
        for (AlterationNode alterationNode : alterationNodeList) {
            if (alterationNode.uniquename == variantUniqueName) {
                variantAlterationNode = alterationNode
                break
            }
        }
        AlterationNode finalAlterationNode = alterationNodeList.last()

        for (OverlapInfo overlapInfo : finalAlterationNode.overlapInfo) {
            // transcript
            Feature feature = Feature.findByUniqueName(overlapInfo.uniquename)
            int variantLocalFmin = convertSourceCoordinateToLocalCoordinateNew(overlapInfo.modLocation.fmin, overlapInfo.modLocation.fmax, overlapInfo.strand, variantAlterationNode.fmin)
            int variantLocalFmax = convertSourceCoordinateToLocalCoordinateNew(overlapInfo.modLocation.fmin, overlapInfo.modLocation.fmax, overlapInfo.strand, variantAlterationNode.fmax)
            println ">>>> [ inferVariantEffectsNew ] Variant Local Fmin: ${variantLocalFmin} Variant Local Fmax:${variantLocalFmax}"
            String genomicSeq = overlapInfo.modLocationSeq
            String cdnaSeq = ""
            String finalCdnaSeq = ""
            boolean overlapsExon = false

            if (overlapInfo.isUpstream) {
                // variant is upstream of transcript
                println ">>>> [ inferVariantEffectsNew ] Variant is UPSTREAM"
                VariantEffect variantEffect = new VariantEffect(
                        feature: Feature.findByUniqueName(overlapInfo.uniquename),
                        variant: SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename),
                        alternateAllele: Allele.findById(variantAlterationNode.alleleId)
                ).save()
                variantEffect.addToEffects(new UpstreamGeneVariant(variantEffect: variantEffect).save())
            }

            if (overlapInfo.isDownstream) {
                // variant is downstream of transcript
                println ">>>> [ inferVariantEffectsNew ] Variant is DOWNSTREAM"
                VariantEffect variantEffect = new VariantEffect(
                        feature: Feature.findByUniqueName(overlapInfo.uniquename),
                        variant: SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename),
                        alternateAllele: Allele.findById(variantAlterationNode.alleleId)
                ).save()
                variantEffect.addToEffects(new DownstreamGeneVariant(variantEffect: variantEffect).save())
            }

            if (overlapInfo.overlaps) {
                // variant overlaps transcript
                println ">>>> [ inferVariantEffectsNew ] Variant OVERLAPS transcript"
                for (OverlapInfo childOverlapInfo : overlapInfo.children) {
                    // scan through child OverlapInfo (exons)
                    cdnaSeq += genomicSeq.substring(childOverlapInfo.modLocalLocation.fmin, childOverlapInfo.modLocalLocation.fmax)
                    println ">>>> [ inferVariantEffectsNew ] ChildOverlapInfo for exon: ${childOverlapInfo.uniquename}"
                    if (childOverlapInfo.overlaps) {
                        // variant overlaps current exon
                        println ">>>> [ inferVariantEffectsNew ] Variant has an insertion of ${variantAlterationNode.alterationResidue} at position ${variantLocalFmin} - ${variantLocalFmax} of exon ${childOverlapInfo.uniquename}"
                        println ">>>> [ inferVariantEffectsNew ] Alteration residues: ${variantAlterationNode.alterationResidue}"
                        println ">>>> [ inferVariantEffectsNew ] Alteration offset: ${variantAlterationNode.offset}"
                        overlapsExon = true
                    }
                }
            }

            if (overlapsExon) {
                SequenceAlteration variant = SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename)
                Allele allele = Allele.findById(variantAlterationNode.alleleId)

                if (overlapInfo.strand == Strand.NEGATIVE.value) {
                    finalCdnaSeq = SequenceTranslationHandler.reverseComplementSequence(cdnaSeq)
                }
                else {
                    finalCdnaSeq = cdnaSeq
                }

                int cdsStart, cdsEnd
                def results = featureService.calculateLongestORF(finalCdnaSeq, configWrapperService.getTranslationTable(), false)
                cdsStart = results.get(1)
                cdsEnd = results.get(2)
                println ">>>> [ inferVariantEffectsNew ] CDS Start: ${cdsStart} CDS End: ${cdsEnd}"

                def exonFminArray = []
                def exonFmaxArray = []
                overlapInfo.children.each { child ->
                    exonFminArray.add(child.modLocation.fmin)
                    exonFmaxArray.add(child.modLocation.fmax)
                }

                int cdsFmin, cdsFmax
                cdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, cdsStart)
                cdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(exonFminArray, exonFmaxArray, cdsEnd)

                println ">>>> [ inferVariantEffectsNew ] CDS fmin: ${cdsFmin} CDS fmax: ${cdsFmax}"
                println ">>>> [ inferVariantEffectsNew ] Final alteration offset: ${finalAlterationNode.cumulativeOffset} and ${finalAlterationNode.offset}"
                int adjustedCdsFmin, adjustedCdsFmax
                adjustedCdsFmin = cdsFmin
                adjustedCdsFmax = cdsFmax - (finalAlterationNode.cumulativeOffset + finalAlterationNode.offset)

                println ">>>> [ inferVariantEffectsNew ] Adjusted CDS fmin: ${adjustedCdsFmin} fmax: ${adjustedCdsFmax}"

                if (variantAlterationNode.type == Insertion.cvTerm) {
                    // Insertion variant
                    println ">>>> [ inferVariantEffectsNew ][ INS ] SEQ: ${cdnaSeq}"
                    println ">>>> [ inferVariantEffectsNew ][ INS ] is this right?: ${genomicSeq.substring(variantLocalFmin + 1, variantLocalFmin + 1 + variantAlterationNode.alterationResidue.length())}"

                }
                else if (variantAlterationNode.type == Deletion.cvTerm) {
                    // Deletion variant
                    println ">>>> [ inferVariantEffectsNew ][ DEL ] SEQ: ${cdnaSeq}"
                    println ">>>> [ inferVariantEffectsNew ][ DEL ] is this right?: ${genomicSeq.substring(variantLocalFmin + 1, variantLocalFmax)}"
                }
                else if (variantAlterationNode.type == Substitution.cvTerm) {
                    // Substitution variant
                    println ">>>> [ inferVariantEffectsNew ][ SUB ] SEQ: ${cdnaSeq}"
                    println ">>>> [ inferVariantEffectsNew ][ SUB ] is this right?: ${genomicSeq.substring(variantLocalFmin + 1, variantLocalFmax)}"

                }

                CDS cds = transcriptService.getCDS(feature)
                JSONObject transcriptJsonObject = featureService.convertFeatureToJSON(feature)

                if (adjustedCdsFmin != cds.fmin || adjustedCdsFmax != cds.fmax) {
                    println ">>>> [ inferVariantEffectsNew ] CDS has been altered; creating a Protein Altering Variant Effect"
                    // CDS has been altered
                    VariantEffect variantEffect = new VariantEffect(
                            feature: feature,
                            variant: variant,
                            alternateAllele: allele
                    ).save()
                    variantEffect.addToEffects(new ProteinAlteringVariant(VariantEffect: variantEffect))
                    updateCdsInTranscriptJson(adjustedCdsFmin, adjustedCdsFmax, transcriptJsonObject)
                    variantEffect.metadata = transcriptJsonObject.toString()

                }
                else {
                    // creating a generic Variant Effect
                    println ">>>> [ inferVariantEffectsNew ] creating a generic Variant Effect"
                    VariantEffect variantEffect = new VariantEffect(
                            feature: feature,
                            variant: variant,
                            alternateAllele: allele
                    ).save()
                    variantEffect.addToEffects(new SequenceVariant(VariantEffect: variantEffect).save())
                    variantEffect.metadata = transcriptJsonObject.toString()
                }


            }


        }

    }

    def createVariantEffects(AlterationNode variantAlterationNode, AlterationNode finalAlterationNode) {
        JSONArray transcriptJsonArray = new JSONArray()
        for (OverlapInfo overlapInfo : finalAlterationNode.overlapInfo) {
            Transcript transcript = Feature.findByUniqueName(overlapInfo.uniquename)
            SequenceAlteration variant = SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename)
            Allele allele = Allele.findById(variantAlterationNode.alleleId)
            VariantEffect variantEffect = new VariantEffect(
                    feature: transcript,
                    variant: variant,
                    alternateAllele: allele
            ).save()
            variantEffect.addToEffects(new SequenceVariant(VariantEffect: variantEffect).save())
            JSONObject transcriptJsonObject = featureService.convertFeatureToJSON(transcript)
            updateTranscriptJsonFromOverlapInfo(transcriptJsonObject, overlapInfo)
            variantEffect.metadata = transcriptJsonObject.toString()
            transcriptJsonArray.add(transcriptJsonObject)
        }

        return transcriptJsonArray
    }

    def updateCdsInTranscriptJson(int cdsFmin, int cdsFmax, JSONObject transcriptJsonObject) {
        for (JSONObject children : transcriptJsonObject.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
            if (children.type.name == FeatureStringEnum.CDS.value) {
                children.location.fmin = cdsFmin
                children.location.fmax = cdsFmax
            }
        }
        println "RETURNING WITH: ${transcriptJsonObject.toString()}"
        return transcriptJsonObject
    }

    def updateTranscriptJsonFromOverlapInfo(JSONObject transcriptJsonObject, OverlapInfo overlapInfo) {
        println "[ updateTranscriptJsonFromOverlapInfo ] Transcript JSON Object: ${transcriptJsonObject.toString()}"
        // TODO
        return transcriptJsonObject
    }
}
