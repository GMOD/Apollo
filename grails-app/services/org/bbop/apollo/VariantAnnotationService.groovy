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
    ArrayList<LocationInfo> cdsLocationInfoTrace = []
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
        log.info "convertLocalCoordinateToSourceCoordinateForTranscript: post sort: ${exonFminArray} ${exonFmaxArray}"

        for (int i = 0; i < exonFminArray.size(); i++) {
            int exonFmin = exonFminArray.get(i)
            int exonFmax = exonFmaxArray.get(i)
            int exonLength = exonFmax - exonFmin

            if (currentLength + exonLength >= localCoordinate) {
                log.info "localCoordinate falls within exon ${exonFmin} - ${exonFmax}"
                if (strand == Strand.NEGATIVE.value) {
                    log.info "strand is NEGATIVE"
                    //sourceCoordinate = (exonFmax - currentCoordinate) - 1
                    sourceCoordinate = (exonFmax - currentCoordinate)
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
        println "[ createVariantEffectMetadataJSON ] JSONObject: ${jsonObject.toString()}"
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
        println "[ predictEffectOfVariantOnTranscripts ][ Fs-SAs-V ] ${allAlterations.cvTerm}"
        def alterationNodeList2 = createAlterationRepresentation(transcripts, allAlterations)
        println "[ predictEffectOfVariantOnTranscripts ] [ Fs-SAs-V ] ALTERATION NODE LIST 2: ${alterationNodeList2.toString()}"
        testAlterationNodeForVariant(alterationNodeList2.last())
        inferVariantEffects(variant.uniqueName, alterationNodeList1, alterationNodeList2)
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
            println "[ createAlterationRepresentationForVariant ] [ 1V-1P ] before: ${alterationNode.fmin} ${alterationNode.fmax}"
            alterationNode.fmin = alterationNode.fmin + alterationNode.cumulativeOffset
            alterationNode.fmax = alterationNode.fmax + alterationNode.cumulativeOffset
            println "[ createAlterationRepresentationForVariant ] [ 1V-1P ] after: ${alterationNode.fmin} ${alterationNode.fmax}"
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
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ 1SA-1P ] [DEBUG] offsets: ${PREV.cumulativeOffset} ${PREV.offset}"
        alterationNode.cumulativeOffset = PREV.cumulativeOffset + PREV.offset
        if (PREV.type == Deletion.cvTerm && PREV.alterationType == FeatureStringEnum.VARIANT.value) {
            // a hack
            alterationNode.cumulativeOffset += 1
        }
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ 1SA-1P ] [DEBUG] before: ${alterationNode.fmin} ${alterationNode.fmax}"
        alterationNode.fmin = alterationNode.fmin + alterationNode.cumulativeOffset
        alterationNode.fmax = alterationNode.fmax + alterationNode.cumulativeOffset
        println "[ createAlterationRepresentationForAssemblyErrorCorrection ] [ 1SA-1P ] [DEBUG] after: ${alterationNode.fmin} ${alterationNode.fmax}"

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

        if (alterationNode.fmin < overlapInfo.location.fmin) {
            // SA is upstream of feature
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = overlapInfo.location.fmin + alterationOffset
                modifiedFmax = overlapInfo.location.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = overlapInfo.location.fmin - alterationOffset
                modifiedFmax = overlapInfo.location.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmin
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
        else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmin < overlapInfo.location.fmax) {
            // SA is within feature
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (sequenceAlteration instanceof Insertion) {
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax + alterationOffset
            }
            else if (sequenceAlteration instanceof Deletion) {
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax - alterationOffset
            }
            else if (sequenceAlteration instanceof Substitution) {
                // no change
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax
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
        else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmin > overlapInfo.location.fmax) {
            // SA is downstream of feature
            // no change
            println "[ updateOverlapInfoWithAssemblyErrorCorrection ] SA is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = overlapInfo.location.fmin
            modifiedFmax = overlapInfo.location.fmax
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

            if (alterationNode.fmin < overlapInfo.location.fmin) {
                // SA is upstream of feature
            }
            else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmax < overlapInfo.location.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(overlapInfo.location.fmin, overlapInfo.location.fmax, overlapInfo.strand, alterationNode.fmin)
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
            else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmax > overlapInfo.location.fmax) {
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

        if (alterationNode.fmin < overlapInfo.location.fmin) {
            // SA is upstream of feature
            println "[ updateOverlapInfoWithVariant ] variant is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            isUpstream = true
            if (variant instanceof Insertion) {
                modifiedFmin = overlapInfo.location.fmin + alterationOffset
                modifiedFmax = overlapInfo.location.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = overlapInfo.location.fmin - alterationOffset
                modifiedFmax = overlapInfo.location.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax
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
        else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmin < overlapInfo.location.fmax) {
            // SA is within feature
            println "[ updateOverlapInfoWithVariant ] variant is within feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = true
            overlaps = true
            if (variant instanceof Insertion) {
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax + alterationOffset
            }
            else if (variant instanceof Deletion) {
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax - alterationOffset
            }
            else if (variant instanceof Substitution) {
                // no change
                modifiedFmin = overlapInfo.location.fmin
                modifiedFmax = overlapInfo.location.fmax
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
        else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmin > overlapInfo.location.fmax) {
            // SA is downstream of feature
            // no change
            println "[ updateOverlapInfoWithVariant ] variant is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = overlapInfo.location.fmin
            modifiedFmax = overlapInfo.location.fmax
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

            if (alterationNode.fmin < overlapInfo.location.fmin) {
                // SA is upstream of feature
            }
            else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmax < overlapInfo.location.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(overlapInfo.location.fmin, overlapInfo.location.fmax, overlapInfo.strand, alterationNode.fmin)
                int alterationLocalFmax = convertSourceCoordinateToLocalCoordinateNew(overlapInfo.location.fmin, overlapInfo.location.fmax, overlapInfo.strand, alterationNode.fmax)
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
            else if (alterationNode.fmin > overlapInfo.location.fmin && alterationNode.fmax > overlapInfo.location.fmax) {
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
        println "[ getAlterationOffset ][ SA ]  returning with offset: ${offset}"
        return offset
    }

    def getAlterationOffset(SequenceAlteration variant, Allele allele) {
        int offset
        if (variant instanceof Insertion) {
            offset = allele.alterationResidue.length()
        }
        else if (variant instanceof Deletion) {
            println "%%%% VARIANT REF BASE: ${variant.referenceBases}"
            println "%%%% ALLELE ALTERATION RESIDUE: ${allele.alterationResidue}"
            println "%%%% ALLELE ALT BASE: ${allele.bases}"
            offset = allele.alterationResidue.length()
        }
        else if (variant instanceof Deletion) {
            offset = 0
        }

        println "[ getAlterationOffset ][ VARIANT ]  returning with offset: ${offset}"
        return offset
    }

    def inferVariantEffects(String variantUniqueName, def originalState, def finalState) {
        AlterationNode originalStateNode
        boolean originalStateAvailable = false
        if (originalState.size() > 0) {
            originalStateAvailable = true
            originalStateNode = originalState.last()
        }

        AlterationNode finalStateNode = finalState.last()
        AlterationNode variantAlterationNode

        for (AlterationNode alterationNode : finalState) {
            if (alterationNode.uniquename == variantUniqueName) {
                variantAlterationNode = alterationNode
            }
        }

        /*
        originalStateNode - contains information of the states of transcripts with only sequence alterations
        variantAlterationNode - contains information of the states of transcripts when incorporating the variant,
        but this node is only used for variant specific properties
        finalStateNode - contains information of the states of transcripts after incorporating all
        sequence alterations and the variant itself
         */

        for (int i = 0; i < finalStateNode.overlapInfo.size(); i++) {
            // transcript
            OverlapInfo originalStateOverlapInfo
            if (originalStateAvailable) originalStateOverlapInfo = originalStateNode.overlapInfo.get(i)
            OverlapInfo finalStateOverlapInfo = finalStateNode.overlapInfo.get(i)
            OverlapInfo variantOverlapInfo = variantAlterationNode.overlapInfo.get(i)

            Feature feature = Feature.findByUniqueName(finalStateOverlapInfo.uniquename)
            int variantLocalFmin = convertSourceCoordinateToLocalCoordinateNew(finalStateOverlapInfo.modLocation.fmin, finalStateOverlapInfo.modLocation.fmax, finalStateOverlapInfo.strand, variantAlterationNode.fmin)
            int variantLocalFmax = convertSourceCoordinateToLocalCoordinateNew(finalStateOverlapInfo.modLocation.fmin, finalStateOverlapInfo.modLocation.fmax, finalStateOverlapInfo.strand, variantAlterationNode.fmax)
            println "[ inferVariantEffects ] Variant local fmin: ${variantLocalFmin} local fmax: ${variantLocalFmax}"
            String originalStateGenomicSeq
            if (originalStateAvailable) {
                originalStateGenomicSeq = originalStateOverlapInfo.modLocationSeq
            }
            else {
                originalStateGenomicSeq = sequenceService.getSequenceForFeature(feature, FeatureStringEnum.TYPE_GENOMIC.value)
            }
            String finalStateGenomicSeq = finalStateOverlapInfo.modLocationSeq

            String originalStateCdnaSeq = ""
            String finalStateCdnaSeq = ""
            boolean overlapsExon = false

            def finalStateExonFminArray = []
            def finalStateExonFmaxArray = []

            if (variantOverlapInfo.isUpstream) {
                // variant is upstream of transcript
                println "[ inferVariantEffects ] Variant is UPSTREAM"
                VariantEffect variantEffect = new VariantEffect(
                        feature: feature,
                        variant: SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename),
                        alternateAllele: Allele.findById(variantAlterationNode.alleleId)
                ).save()
                variantEffect.addToEffects(new UpstreamGeneVariant(variantEffect: variantEffect).save())
            }

            if (variantOverlapInfo.isDownstream) {
                // variant is downstream of transcript
                println "[ inferVariantEffects ] Variant is DOWNSTREAM"
                VariantEffect variantEffect = new VariantEffect(
                        feature: feature,
                        variant: SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename),
                        alternateAllele: Allele.findById(variantAlterationNode.alleleId)
                ).save()
                variantEffect.addToEffects(new DownstreamGeneVariant(variantEffect: variantEffect).save())
            }

            if (variantOverlapInfo.overlaps) {
                // variant overlaps transcript
                println "[ inferVariantEffects ] Variant OVERLAPS transcript"
                for (int j = 0; j < variantOverlapInfo.children.size(); j++) {
                    // scan through the child OverlapInfo (representing exons)
                    OverlapInfo originalStateChildOverlapInfo
                    if (originalStateAvailable) originalStateChildOverlapInfo = originalStateOverlapInfo.children.get(j)
                    OverlapInfo finalStateChildOverlapInfo = finalStateOverlapInfo.children.get(j)
                    OverlapInfo variantChildOverlapInfo = variantOverlapInfo.children.get(j)
                    println "[ inferVariantEffects ] ChildOverlapInfo for exon: ${variantChildOverlapInfo.uniquename}"
                    if (originalStateAvailable) {
                        originalStateCdnaSeq += originalStateGenomicSeq.substring(originalStateChildOverlapInfo.modLocalLocation.fmin, originalStateChildOverlapInfo.modLocalLocation.fmax)
                    }
                    else {
                        originalStateCdnaSeq = sequenceService.getSequenceForFeature(feature, FeatureStringEnum.TYPE_CDNA.value)
                    }
                    finalStateCdnaSeq += finalStateGenomicSeq.substring(finalStateChildOverlapInfo.modLocalLocation.fmin, finalStateChildOverlapInfo.modLocalLocation.fmax)
                    finalStateExonFminArray.add(finalStateChildOverlapInfo.modLocation.fmin)
                    finalStateExonFmaxArray.add(finalStateChildOverlapInfo.modLocation.fmax)

                    if (variantChildOverlapInfo.overlaps) {
                        // variant overlaps current exon
                        overlapsExon = true
                    }
                }
            }

            println "[ inferVariantEffects ] original CDNA seq: ${originalStateCdnaSeq}"
            println "[ inferVariantEffects ] final CDNA seq: ${finalStateCdnaSeq}"

            if (overlapsExon) {
                SequenceAlteration variant = SequenceAlteration.findByUniqueName(variantAlterationNode.uniquename)
                Allele allele = Allele.findByIdAndVariant(variantAlterationNode.alleleId, variant)

                String finalCdnaSeq
                if (variantOverlapInfo.strand == Strand.NEGATIVE.value) {
                    finalCdnaSeq = SequenceTranslationHandler.reverseComplementSequence(finalStateCdnaSeq)
                }
                else {
                    finalCdnaSeq = finalStateCdnaSeq
                }

                println "[ inferVariantEffects ] variantAlterationNode of type: ${variantAlterationNode.type}"
                println "[ inferVariantEffects ] Variant substitutes to ${variantAlterationNode.alterationResidue} at position ${variantLocalFmin} - ${variantLocalFmax}"
                println "[ inferVariantEffects ] Alteration residues: ${variantAlterationNode.alterationResidue}"
                println "[ inferVariantEffects ] Alteration offset: ${variantAlterationNode.offset}"

                CDS originalCds = transcriptService.getCDS(feature)
                boolean stopCodonReadThrough = cdsService.getStopCodonReadThrough(originalCds) ? true : false
                int finalCdsStart, finalCdsEnd

                def results = featureService.calculateLongestORF(finalCdnaSeq, configWrapperService.getTranslationTable(), stopCodonReadThrough)
                finalCdsStart = results.get(1)
                finalCdsEnd = results.get(2)
                println "[ inferVariantEffects ] CDS start: ${finalCdsStart} end: ${finalCdsEnd}"

                def finalCdsSeq = finalCdnaSeq.substring(finalCdsStart, finalCdsEnd)

                int finalCdsFmin, finalCdsFmax
                if (variantOverlapInfo.strand == Strand.NEGATIVE.value) {
                    finalCdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(finalStateExonFminArray, finalStateExonFmaxArray, variantOverlapInfo.strand, finalCdsEnd)
                    finalCdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(finalStateExonFminArray, finalStateExonFmaxArray, variantOverlapInfo.strand, finalCdsStart)
                }
                else {
                    finalCdsFmin = convertLocalCoordinateToSourceCoordinateForTranscript(finalStateExonFminArray, finalStateExonFmaxArray, variantOverlapInfo.strand, finalCdsStart)
                    finalCdsFmax = convertLocalCoordinateToSourceCoordinateForTranscript(finalStateExonFminArray, finalStateExonFmaxArray, variantOverlapInfo.strand, finalCdsEnd)
                }

                println "[ inferVariantEffects ] CDS fmin: ${finalCdsFmin} CDS fmax: ${finalCdsFmax}"
                println "[ inferVariantEffects ] variant alteration cumulativeOffset: ${variantAlterationNode.cumulativeOffset} and offset: ${variantAlterationNode.offset}"

                // for visualization
                int adjustedFinalCdsFmin, adjustedFinalCdsFmax
                (adjustedFinalCdsFmin, adjustedFinalCdsFmax) = applyOffsetForVisualRepresentation(finalState, finalCdsFmin, finalCdsFmax, finalStateOverlapInfo.strand)
                println "[ inferVariantEffects ][INS] Adjusted CDS fmin: ${adjustedFinalCdsFmin} fmax: ${adjustedFinalCdsFmax}"
                //
                cdsLocationInfoTrace.add(new LocationInfo(adjustedFinalCdsFmin, adjustedFinalCdsFmax))

                JSONObject transcriptJsonObject = featureService.convertFeatureToJSON(feature)

                if (originalCds.fmin != adjustedFinalCdsFmin || originalCds.fmax != adjustedFinalCdsFmax) {
                    // TODO
                    compareCdsSequences(sequenceService.getSequenceForFeature(feature, FeatureStringEnum.TYPE_CDS.value), finalCdsSeq)
                    VariantEffect variantEffect = new VariantEffect(
                            feature: feature,
                            variant: variant,
                            alternateAllele: allele
                    ).save()
                    variantEffect.addToEffects(new ProteinAlteringVariant(VariantEffect: variantEffect))
                    updateCdsInTranscriptJson(adjustedFinalCdsFmin, adjustedFinalCdsFmax, transcriptJsonObject)
                    variantEffect.metadata = transcriptJsonObject.toString()
                }
                else {
                    // creating a generic Variant Effect
                    println "[ inferVariantEffects ] creating a generic Variant Effect"
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

    /**
     * Experimental but works and can be extended further for additional edge cases
     */
    def applyOffsetForVisualRepresentation(def state, int cdsFmin, int cdsFmax, int strand) {

        int previousOffset = 0
        int adjustedFinalCdsFmin = cdsFmin
        int adjustedFinalCdsFmax = cdsFmax

        for (AlterationNode alterationNode : state) {
            int alterationFmin = alterationNode.fmin
            int alterationFmax = alterationNode.fmax

            int offset = alterationNode.offset
            if (alterationNode.alterationType == FeatureStringEnum.VARIANT.value) {
                if (alterationNode.type == Insertion.cvTerm) {
                    alterationFmin += 1
                    alterationFmax += 1
                }
                else if (alterationNode.type == Deletion.cvTerm) {
                    alterationFmin += 1
                    offset = alterationFmax - alterationFmin
                }
            }
            else {
                if (alterationNode.type == Deletion.cvTerm) {
                    offset = alterationFmax - alterationFmin
                }
            }

            println "[ applyOffsetForVisualRepresentation ] ALTERATION FMIN: ${alterationFmin} FMAX: ${alterationFmax} OFFSET: ${offset}"
            println "[ applyOffsetForVisualRepresentation ]  CDS FMIN: ${adjustedFinalCdsFmin} FMAX: ${adjustedFinalCdsFmax}"
            if (strand == Strand.POSITIVE.value) {
                if (alterationNode.type == Insertion.cvTerm) {
                    alterationFmin = alterationFmin - previousOffset
                    alterationFmax = alterationFmax - previousOffset

                    if (adjustedFinalCdsFmin == alterationFmin) {
                        // cdsFmin is right at the insertion start
                        // Ex: ATG insertion and CDS fmin starts from this codon
                        // do nothing
                        println "[ applyOffsetForVisualRepresentation ][+][INS][A1] no need to adjust CDS fmin"
                    }
                    else if (adjustedFinalCdsFmin > alterationFmin && adjustedFinalCdsFmin <= alterationFmax + offset) {
                        // cdsFmin is between insertion start and end
                        // Ex: AATG insertion and CDS fmin starts from the 2nd base of the insertion
                        println "[ applyOffsetForVisualRepresentation ][+][INS][A2] adjusting CDS fmin"
                        adjustedFinalCdsFmin = alterationFmin
                    }
                    else if (adjustedFinalCdsFmin > alterationFmax + offset) {
                        // cdsFmin starts after the insertion
                        println "[ applyOffsetForVisualRepresentation ][+][INS][A3] adjusting CDS fmin"
                        adjustedFinalCdsFmin += offset

                    }

                    if (adjustedFinalCdsFmax == alterationFmin) {
                        // cdsFmax is right at the insertion start
                        // do nothing
                        println "[+][INS][B1] no need to adjust CDS fmax"
                    }
                    else if (adjustedFinalCdsFmax > alterationFmin && adjustedFinalCdsFmax <= alterationFmax + offset) {
                        // cdsFmax is between insertion start and end
                        println "[ applyOffsetForVisualRepresentation ][+][INS][B2] adjusting CDS fmax"
                        adjustedFinalCdsFmax = alterationFmax
                    }
                    else if (adjustedFinalCdsFmax > alterationFmax + offset) {
                        // cdsFmax starts after the insertion
                        println "[ applyOffsetForVisualRepresentation ][+][INS][B3] adjusting CDS fmax"
                        adjustedFinalCdsFmax -= offset

                    }
                }
                else if (alterationNode.type == Deletion.cvTerm) {
                    alterationFmin = alterationFmin - previousOffset
                    alterationFmax = alterationFmax - previousOffset

                    if (adjustedFinalCdsFmin == alterationFmin) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][A1] no need to adjust CDS fmin"
                    }
                    else if (adjustedFinalCdsFmin > alterationFmin && adjustedFinalCdsFmin <= alterationFmax - offset) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][A2] no need to adjust CDS fmin"
                        adjustedFinalCdsFmin = alterationFmin
                    }
                    else if (adjustedFinalCdsFmin > alterationFmax + offset) {
                        // cdsFmin starts after the deletion
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][A3] adjusting CDS fmin"
                        adjustedFinalCdsFmin += offset
                    }

                    if (adjustedFinalCdsFmax == alterationFmin) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][B1] no need to adjust CDS fmax"
                    }
                    else if (adjustedFinalCdsFmax > alterationFmin && adjustedFinalCdsFmax <= alterationFmax + offset) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][B2] no need to adjust CDS fmax"
                        adjustedFinalCdsFmax = alterationFmax + offset
                    }
                    else if (adjustedFinalCdsFmax > alterationFmax + offset) {
                        // cdsFmax starts after the deletion
                        println "[ applyOffsetForVisualRepresentation ][+][DEL][B3] adjusting CDS fmax"
                        adjustedFinalCdsFmax += offset
                    }
                }
                else if (alterationNode.type == Substitution.cvTerm) {
                    // no need for adjusting
                }
            }
            else {
                // strand is negative
                if (alterationNode.type == Insertion.cvTerm) {
                    alterationFmin = alterationFmin - previousOffset
                    alterationFmax = alterationFmax - previousOffset
                    if (adjustedFinalCdsFmin == alterationFmin) {
                        // cdsFmin is right at the insertion start
                        // Ex: ATG insertion and CDS fmin starts from this codon
                        // do nothing
                        println "[ applyOffsetForVisualRepresentation ][-][INS][A1] no need to adjust CDS fmin"
                    }
                    else if (adjustedFinalCdsFmin > alterationFmin && adjustedFinalCdsFmin <= alterationFmax + offset) {
                        // cdsFmin is between insertion start and end
                        // Ex: AATG insertion and CDS fmin starts from the 2nd base of the insertion
                        println "[ applyOffsetForVisualRepresentation ][-][INS][A2] adjusting CDS fmin"
                        adjustedFinalCdsFmin = alterationFmin
                    }
                    else if (adjustedFinalCdsFmin > alterationFmax + offset) {
                        // cdsFmin starts after the insertion
                        println "[ applyOffsetForVisualRepresentation ][-][INS][A3] adjusting CDS fmin"
                        adjustedFinalCdsFmin -= offset

                    }

                    if (adjustedFinalCdsFmax == alterationFmin) {
                        // cdsFmax is right at the insertion start
                        // do nothing
                        println "[ applyOffsetForVisualRepresentation ][-][INS][B1] no need to adjust CDS fmax"
                    }
                    else if (adjustedFinalCdsFmax > alterationFmin && adjustedFinalCdsFmax <= alterationFmax + offset) {
                        // cdsFmax is between insertion start and end
                        println "[ applyOffsetForVisualRepresentation ][-][INS][B2] adjusting CDS fmax"
                        adjustedFinalCdsFmax = alterationFmax + offset
                    }
                    else if (adjustedFinalCdsFmax > alterationFmax + offset) {
                        // cdsFmax starts after the insertion
                        println "[ applyOffsetForVisualRepresentation ][-][INS][B3] adjusting CDS fmax"
                        adjustedFinalCdsFmax -= offset

                    }
                }
                else if (alterationNode.type == Deletion.cvTerm) {
                    alterationFmin = alterationFmin - previousOffset
                    alterationFmax = alterationFmax - previousOffset
                    if (adjustedFinalCdsFmin == alterationFmin) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][A1] no need to adjust CDS fmin"
                    }
                    else if (adjustedFinalCdsFmin > alterationFmin && adjustedFinalCdsFmin <= alterationFmax - offset) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][A2] no need to adjust CDS fmin"
                        adjustedFinalCdsFmin = alterationFmin
                    }
                    else if (adjustedFinalCdsFmin > alterationFmax + offset) {
                        // cdsFmin starts after the deletion
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][A3] adjusting CDS fmin"
                        adjustedFinalCdsFmin += offset
                    }

                    if (adjustedFinalCdsFmax == alterationFmin) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][B1] no need to adjust CDS fmax"
                    }
                    else if (adjustedFinalCdsFmax > alterationFmin && adjustedFinalCdsFmax <= alterationFmax - offset) {
                        // will this ever happen?
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][B2] no need to adjust CDS fmax"
                        adjustedFinalCdsFmax = alterationFmax
                    }
                    else if (adjustedFinalCdsFmax > alterationFmax + offset) {
                        // cdsFmax starts after the deletion
                        println "[ applyOffsetForVisualRepresentation ][-][DEL][B3] adjusting CDS fmax"
                        adjustedFinalCdsFmax += offset
                    }
                }
            }

            if (alterationNode.type == Insertion.cvTerm) {
                previousOffset += offset
            }
            else if (alterationNode.type == Deletion.cvTerm) {
                previousOffset -= offset
            }
            println "[ applyOffsetForVisualRepresentation ] ADJUSTED CDS FMIN: ${adjustedFinalCdsFmin} FMAX: ${adjustedFinalCdsFmax}"
        }
        return [adjustedFinalCdsFmin, adjustedFinalCdsFmax]
    }

    def compareCdsSequences(String originalCds, String variantCds) {

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
