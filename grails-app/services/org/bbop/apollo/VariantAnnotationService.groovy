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
    HashMap<Allele,ArrayList<String>> sequenceTrace = new HashMap<>()

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

//    def calculateEffectOfVariantOnTranscriptNew(SequenceAlteration variant, Transcript transcript) {
//        println "@calculateEffectOfVariantOnTranscriptNew"
//        def sequenceAlterations = featureService.getSequenceAlterationsForFeature(transcript, [FeatureStringEnum.ASSEMBLY_ERROR_CORRECTION.value])
//        // add variant into the list
//        sequenceAlterations.add(variant)
//        // sort by fmin
//        sequenceAlterations.sort({a, b -> a.fmin <=> b.fmin})
//        println "Sequence alterations with Variant included: ${sequenceAlterations.fmin}"
//        println "Sequence alterations with Variant included: ${sequenceAlterations.alterationType}"
//
//        def alterationNodes = []
//        // create an AlterationNode for each sequence alteration
//        for (SequenceAlteration sa : sequenceAlterations) {
//            AlterationNode alterationNode = new AlterationNode(sa)
//            if (sa.alterationType == FeatureStringEnum.VARIANT.value) {
//                alterationNode.alterationResidue = sa.alternateAlleles.first().alterationResidue
//            }
//            println "AlterationNode: ${alterationNode.toString()}"
//            alterationNode.overlapInfo.add(new OverlapInfo(transcript))
//        }
//
//
//    }

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
        jsonObject.feature = feature.uniqueName
        jsonObject.variant = variant.uniqueName
        jsonObject.alternate_allele = allele.bases
        jsonObject.type = new JSONArray()
        variantEffect.effects.each {
            jsonObject.getJSONArray("type").add(it.cvTerm)
        }
        jsonObject.put("type", variantEffect.effects.first().cvTerm)
    }

    def getOverlappingFeatures(Sequence sequence, int fmin, int fmax, int strand, boolean compareStrands = false, boolean includeVariants = false) {
        if (includeVariants) {
            if (compareStrands) {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))",
                        [fmin: fmin, fmax: fmax, strand: strand, sequence: sequence]
                )
            }
            else {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))",
                        [fmin: fmin, fmax: fmax, sequence: sequence]
                )
            }
        }
        else {
            if (compareStrands) {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax) and f.class not in :featureTypes)",
                        [fmin: fmin, fmax: fmax, strand: strand, sequence: sequence, featureTypes: [Insertion.class.name, Deletion.class.name, Substitution.class.name]]
                )
            }
            else {
                Feature.executeQuery(
                        "select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax)) and f.class not in :featureTypes",
                        [fmin: fmin, fmax: fmax, sequence: sequence, featureTypes: [Insertion.class.name, Deletion.class.name, Substitution.class.name]]
                )
            }
        }
    }




    def calculateEffectOfVariant(SequenceAlteration variant) {
        int flank_size = 1000
        def overlappingAndNearbyFeatures = getOverlappingFeatures(variant.featureLocation.sequence, variant.fmin - 1000, variant.fmax + 1000, variant.strand)
        println "[ calculateEffectOfVariant ] overlapping and nearby features: ${overlappingAndNearbyFeatures.cvTerm}"
        def overlappingAndNearbyTranscriptFeatures = []
        overlappingAndNearbyFeatures.each {
            if (it instanceof Transcript) {
                overlappingAndNearbyTranscriptFeatures.add(it)
            }
        }
        println "[ calculateEffectOfVariant ] overlapping and nearby transcript features: ${overlappingAndNearbyTranscriptFeatures.cvTerm}"
        predictEffectOfVariantOnTranscripts(overlappingAndNearbyTranscriptFeatures, variant)
    }

    def predictEffectOfVariantOnTranscripts(def transcripts, SequenceAlteration variant) {
        println "[ predictEffectOfVariantOnTranscripts ] transcripts: ${transcripts.size()} variant: ${variant.name}"
        def alterationNodes = []
        for (Allele allele : variant.alternateAlleles) {
            AlterationNode alterationNode = createAlterationRepresentation(transcripts, variant, allele)
            println "[ predictEffectOfVariantOnTranscripts  ] ${alterationNode.toString()}"
            alterationNodes.add(alterationNode)
            testAlterationNode(alterationNode)
        }
        return alterationNodes
    }

    def testAlterationNode(AlterationNode alterationNode) {
        boolean pass = false
        for (OverlapInfo overlapInfo : alterationNode.overlapInfo) {
            Feature feature = Feature.findByUniqueName(overlapInfo.uniquename)
            String apolloCdnaSeq = sequenceService.getSequenceForFeature(feature, FeatureStringEnum.TYPE_CDNA.value)
            StringBuilder genomicSeq = new StringBuilder(overlapInfo.modLocationSeq)
            println "[ testAlterationNode ] GenomicSeq: ${genomicSeq}"
            String testCdnaSeq = ""
            String finalCdnaSeq = ""
            for (OverlapInfo child : overlapInfo.children) {
                println "[ testAlterationNode ] Fetching substr ${child.modLocalLocation.fmin} - ${child.modLocalLocation.fmax}"
                testCdnaSeq += genomicSeq.substring(child.modLocalLocation.fmin, child.modLocalLocation.fmax)
            }
            if (feature.strand == Strand.NEGATIVE.value) {
                finalCdnaSeq = SequenceTranslationHandler.reverseComplementSequence(testCdnaSeq)
            }
            else {
                finalCdnaSeq = testCdnaSeq
            }
            println "[ testAlterationNode ] Apollo CDNA Seq: ${apolloCdnaSeq}"
            println "[ testAlterationNode ] Final  CDNA Seq: ${finalCdnaSeq}"
            if (apolloCdnaSeq == finalCdnaSeq) {
                println "[ testAlterationNode ] Apollo CDNA seq matches final CDNA seq generated from AlterationNode"
                pass = true
            }
            else {
                println "[ testAlterationNode ] Apollo CDNA seq DOESN'T match final CDNA seq generated from AlterationNode"
                pass = false
            }
        }
    }

    def createAlterationRepresentation(def features, SequenceAlteration sequenceAlteration, Allele allele) {
        AlterationNode alterationNode = new AlterationNode(sequenceAlteration, allele)
        def overlapInfoList = []
        for (Feature feature : features) {
            OverlapInfo overlapInfo = createOverlapInfoWithVariant(feature, sequenceAlteration, allele)
            def exons = transcriptService.getSortedExons(feature)

            def exonOverlapInfoList = []
            for (Exon exon : exons) {
                OverlapInfo exonOverlapInfo = createOverlapInfoWithVariant(exon, sequenceAlteration, allele, feature, false)
                exonOverlapInfoList.add(exonOverlapInfo)
            }
            overlapInfo.children = exonOverlapInfoList
            overlapInfoList.add(overlapInfo)
        }
        alterationNode.overlapInfo = overlapInfoList
        return alterationNode
    }


    def createAlterationRepresentation(Feature feature, def sequenceAlterations) {
        println "@createAlterationRepresentation for more than one SA"
        def alterationNodeList = []
        int cumulativeOffset = 0
        AlterationNode PREV = null
        for (SequenceAlteration sa : sequenceAlterations) {
            AlterationNode alterationNode
            if (PREV) {
                alterationNode = createAlterationRepresentation(feature, sa, PREV)
            }
            else {
                alterationNode = createAlterationRepresentation(feature, sa)
            }
            alterationNode.cumulativeOffset = cumulativeOffset
            cumulativeOffset = cumulativeOffset + alterationNode.offset
            alterationNodeList.add(alterationNode)
            PREV = alterationNode
        }
        println "${alterationNodeList.toString()}"

        // TEST
        testAlterationNode(alterationNodeList.last())
    }


    def createAlterationRepresentation(Feature feature, SequenceAlteration sequenceAlteration, AlterationNode PREV) {
        println "@createAlterationRepresentation with PREV"
        def gene
        def exons = []

        if (feature instanceof Transcript) {
            println "feature instanceof Transcript"
            gene = transcriptService.getGene(feature)
            exons = transcriptService.getSortedExons(feature, false)
        }
        else {
            println "feature instanceof ${feature.class}"
        }

        AlterationNode alterationNode = new AlterationNode(sequenceAlteration)
        def currentOverlapInfoList = []
        def previousOverlapInfoList = PREV.overlapInfo
        for (int i = 0; i < previousOverlapInfoList.size(); i++) {
            // top level
            OverlapInfo previousOverlapInfo = previousOverlapInfoList.get(i)
            OverlapInfo overlapInfo = previousOverlapInfo.generateClone()
            overlapInfo = updateOverlapInfoWithAssemblyErrorCorrection(overlapInfo, feature, sequenceAlteration, true)

            def currentChildOverlapInfo = []
            if (previousOverlapInfo.children) {
                for (int j = 0; j < previousOverlapInfo.children.size(); j++) {
                    OverlapInfo previousChildOverlapInfo = previousOverlapInfo.children.get(j)
                    OverlapInfo childOverlapInfo = previousChildOverlapInfo.generateClone()
                    childOverlapInfo = updateOverlapInfoWithAssemblyErrorCorrection(childOverlapInfo, Feature.findByUniqueName(childOverlapInfo.uniquename), sequenceAlteration, false)
                    currentChildOverlapInfo.add(childOverlapInfo)
                }
                overlapInfo.children = currentChildOverlapInfo
            }
            currentOverlapInfoList.add(overlapInfo)
        }
        alterationNode.overlapInfo = currentOverlapInfoList

        return alterationNode
    }

    def createAlterationRepresentation(Feature feature, SequenceAlteration sequenceAlteration) {
        println "@createAlterationRepresentation"
        def gene
        def exons = []
        def cds // ignore CDS since is a calculated property

        if (feature instanceof Transcript) {
            println "feature instanceof Transcript"
            gene = transcriptService.getGene(feature)
            exons = transcriptService.getSortedExons(feature, false)
        }
        else {
            println "feature instanceof ${feature.class}"
        }

        AlterationNode alterationNode = new AlterationNode(sequenceAlteration)
        println "Alteration Node: ${alterationNode.toString()}"
        OverlapInfo overlapInfo = createOverlapInfoWithAssemblyErrorCorrection(feature, sequenceAlteration)

        def exonOverlapInfos = []
        for (Exon exon : exons) {
            OverlapInfo exonOverlapInfo = createOverlapInfoWithAssemblyErrorCorrection(exon, sequenceAlteration, feature, false)
            exonOverlapInfos.add(exonOverlapInfo)
        }
        overlapInfo.children = exonOverlapInfos
        println "Overlap Info: ${overlapInfo.toString()}"
        alterationNode.overlapInfo = [overlapInfo]

        // TEST
        testAlterationNode(alterationNode)

        return alterationNode
    }

    // TODO: move the block shared by createOverlap* And updateOverlap* to a separate common function
    def createOverlapInfoWithAssemblyErrorCorrection(Feature feature, SequenceAlteration sequenceAlteration, Feature parentFeature = null, boolean getSequence = true) {
        // TODO: inference
        int alterationOffset = getAlterationOffset(sequenceAlteration)
        String alterationResidue = sequenceAlteration.alterationResidue
        println "ALTERATION OFFSET: ${alterationOffset}"
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
        println "ALTERATION OFFSET: ${alterationOffset}"
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

    def updateOverlapInfoWithAssemblyErrorCorrection(OverlapInfo overlapInfo, Feature feature, SequenceAlteration sequenceAlteration, boolean getSequence = true) {
        // TODO: inference
        int alterationOffset = getAlterationOffset(sequenceAlteration)
        String alterationResidue = sequenceAlteration.alterationResidue
        println "ALTERATION OFFSET: ${alterationOffset}"
        boolean isModified, isUpstream, isDownstream, overlaps
        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax
        int localFmin = overlapInfo.localLocation.fmin
        int localFmax = overlapInfo.localLocation.fmax

        if (sequenceAlteration.fmin < feature.fmin) {
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
        else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmin < feature.fmax) {
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
        else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmin > feature.fmax) {
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

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            StringBuilder builder = new StringBuilder(overlapInfo.locationSeq)

            if (sequenceAlteration.fmin < feature.fmin) {
                // SA is upstream of feature
            }
            else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmax < feature.fmax) {
                // SA is within feature
                int alterationLocalFmin = convertSourceCoordinateToLocalCoordinateNew(feature.fmin, feature.fmax, feature.strand, sequenceAlteration.fmin)
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
            else if (sequenceAlteration.fmin > feature.fmin && sequenceAlteration.fmax > feature.fmax) {
                // SA is downstream of feature
                // no change
            }
            overlapInfo.modLocationSeq = builder.toString()
        }

        return overlapInfo
    }

    def updateOverlapInfoWithVariant(OverlapInfo overlapInfo, Feature feature, SequenceAlteration variant, Allele allele, boolean getSequence = true) {
        // TODO: inference
        int alterationOffset = getAlterationOffset(variant, allele)
        String alterationResidue = allele.bases
        println "[ updateOverlapInfoWithVariant ] ALTERATION OFFSET: ${alterationOffset}"
        boolean isModified, isUpstream, isDownstream, overlaps
        int modifiedFmin, modifiedFmax
        int modifiedLocalFmin, modifiedLocalFmax
        int localFmin = overlapInfo.localLocation.fmin
        int localFmax = overlapInfo.localLocation.fmax

        if (variant.fmin < feature.fmin) {
            // SA is upstream of feature
            println "[ updateOverlapInfoWithVariant ] SA is upstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
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
        else if (variant.fmin > feature.fmin && variant.fmin < feature.fmax) {
            // SA is within feature
            println "[ updateOverlapInfoWithVariant ] SA is within feature ${feature.uniqueName} ${feature.class.simpleName}"
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
            println "[ updateOverlapInfoWithVariant ] SA is downstream of feature ${feature.uniqueName} ${feature.class.simpleName}"
            isModified = false
            isDownstream = true
            modifiedFmin = feature.fmin
            modifiedFmax = feature.fmax
            modifiedLocalFmin = localFmin
            modifiedLocalFmax = localFmax
        }
        else {
            // TODO
            println "[ updateOverlapInfoWithVariant ] TODO: sequenceAlteration is right on the boundary"
        }

        overlapInfo.modLocation = new LocationInfo(modifiedFmin, modifiedFmax)
        overlapInfo.modLocalLocation = new LocationInfo(modifiedLocalFmin, modifiedLocalFmax)

        if (getSequence) {
            StringBuilder builder = new StringBuilder(overlapInfo.locationSeq)

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

    def getAlterationOffset(SequenceAlteration sequenceAlteration, Allele allele) {
        int offset
        if (sequenceAlteration instanceof Insertion) {
            offset = allele.alterationResidue.length()
        }
        else if (sequenceAlteration instanceof Deletion) {
            offset = sequenceAlteration.fmax - sequenceAlteration.fmin
        }
        else if (sequenceAlteration instanceof Deletion) {
            offset = 0
        }

        return offset
    }
}
