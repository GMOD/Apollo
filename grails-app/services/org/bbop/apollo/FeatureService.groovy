package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.alteration.SequenceAlterationInContext
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.Strand
import org.bbop.apollo.sequence.TranslationTable
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed
import org.hibernate.FlushMode

@Transactional(readOnly = true)
class FeatureService {


    def nameService
    def configWrapperService
    def featureService
    def transcriptService
    def exonService
    def cdsService
    def nonCanonicalSplitSiteService
    def featureRelationshipService
    def featurePropertyService
    def sequenceService
    def permissionService
    def overlapperService
    def sessionFactory

    public static final def rnaFeatureTypes = [MRNA.alternateCvTerm,MiRNA.alternateCvTerm,NcRNA.alternateCvTerm, RRNA.alternateCvTerm, SnRNA.alternateCvTerm, SnoRNA.alternateCvTerm, TRNA.alternateCvTerm, Transcript.alternateCvTerm]
    public static final def singletonFeatureTypes = [RepeatRegion.alternateCvTerm, TransposableElement.alternateCvTerm]
    @Timed
    @Transactional
    FeatureLocation convertJSONToFeatureLocation(JSONObject jsonLocation, Sequence sequence, int defaultStrand = Strand.POSITIVE.value) throws JSONException {
        FeatureLocation gsolLocation = new FeatureLocation();
        if (jsonLocation.has(FeatureStringEnum.ID.value)) {
            gsolLocation.setId(jsonLocation.getLong(FeatureStringEnum.ID.value));
        }
        gsolLocation.setFmin(jsonLocation.getInt(FeatureStringEnum.FMIN.value));
        gsolLocation.setFmax(jsonLocation.getInt(FeatureStringEnum.FMAX.value));
        if (jsonLocation.getInt(FeatureStringEnum.STRAND.value) == Strand.POSITIVE.value || jsonLocation.getInt(FeatureStringEnum.STRAND.value) == Strand.NEGATIVE.value) {
            gsolLocation.setStrand(jsonLocation.getInt(FeatureStringEnum.STRAND.value));
        }
        else {
            gsolLocation.setStrand(defaultStrand)
        }
        gsolLocation.setSequence(sequence)
        return gsolLocation;
    }

    /** Get features that overlap a given location.
     *
     * @param location - FeatureLocation that the features overlap
     * @param compareStrands - Whether to compare strands in overlap
     * @return Collection of Feature objects that overlap the FeatureLocation
     */
    Collection<Transcript> getOverlappingTranscripts(FeatureLocation location, boolean compareStrands = true) {
        List<Transcript> transcriptList = new ArrayList<>()
        List<Transcript> overlappingFeaturesList = getOverlappingFeatures(location, compareStrands)

        for (Feature eachFeature : overlappingFeaturesList) {
            Feature feature = Feature.get(eachFeature.id)
            if (feature instanceof Transcript) {
                transcriptList.add((Transcript) feature)
            }
        }

        return transcriptList
    }

    /** Get features that overlap a given location.
     *
     * @param location - FeatureLocation that the features overlap
     * @param compareStrands - Whether to compare strands in overlap
     * @return Collection of Feature objects that overlap the FeatureLocation
     */
    Collection<Feature> getOverlappingFeatures(FeatureLocation location, boolean compareStrands = true) {
        if (compareStrands) {
            //Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax ))",[fmin:location.fmin,fmax:location.fmax,strand:location.strand,sequence:location.sequence])
            Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))", [fmin: location.fmin, fmax: location.fmax, strand: location.strand, sequence: location.sequence])
        } else {
            //Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax ))",[fmin:location.fmin,fmax:location.fmax,sequence:location.sequence])
            Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))", [fmin: location.fmin, fmax: location.fmax, sequence: location.sequence])
        }
    }

    @Transactional
    void updateNewGsolFeatureAttributes(Feature gsolFeature, Sequence sequence = null) {

        gsolFeature.setIsAnalysis(false);
        gsolFeature.setIsObsolete(false);

        if (sequence) {
            gsolFeature.getFeatureLocations().iterator().next().sequence = sequence;
        }

        // TODO: this may be a mistake, is different than the original code
        // you are iterating through all of the children in order to set the SourceFeature and analysis
        // for (FeatureRelationship fr : gsolFeature.getChildFeatureRelationships()) {
        for (FeatureRelationship fr : gsolFeature.getParentFeatureRelationships()) {
            updateNewGsolFeatureAttributes(fr.getChildFeature(), sequence);
        }
    }

    @Transactional
    def setOwner(Feature feature, User owner) {
        if (owner && feature) {
            log.debug "setting owner for feature ${feature} to ${owner}"
            feature.addToOwners(owner)
        } else {
            log.warn "user ${owner} or feature ${feature} is null so not setting"
        }
    }

    /**
     * From Gene.addTranscript
     * @return
     */

    @Timed
    @Transactional
    def generateTranscript(JSONObject jsonTranscript, Sequence sequence, boolean suppressHistory, boolean useCDS = configWrapperService.useCDS()) {
        Gene gene = jsonTranscript.has(FeatureStringEnum.PARENT_ID.value) ? (Gene) Feature.findByUniqueName(jsonTranscript.getString(FeatureStringEnum.PARENT_ID.value)) : null;
        Transcript transcript = null

        User owner = permissionService.getCurrentUser(jsonTranscript)
        // if the gene is set, then don't process, just set the transcript for the found gene
        if (gene) {
            // Scenario I - if 'parent_id' attribute is given then find the gene
            transcript = (Transcript) convertJSONToFeature(jsonTranscript, sequence);
            if (transcript.getFmin() < 0 || transcript.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates")
            }

            setOwner(transcript, owner);

            if (!useCDS || transcriptService.getCDS(transcript) == null) {
                calculateCDS(transcript);
            }
            else {
                // if there are any sequence alterations that overlaps this transcript then
                // recalculate the CDS to account for these changes
                def sequenceAlterations = getSequenceAlterationsForFeature(transcript)
                if (sequenceAlterations.size() > 0) {
                    calculateCDS(transcript)
                }
            }

            addTranscriptToGene(gene, transcript);
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            if (!suppressHistory) {
                transcript.name = nameService.generateUniqueName(transcript)
            }
        } else {
            // Scenario II - find an overlapping isoform and if present, add current transcript to its gene
            FeatureLocation featureLocation = convertJSONToFeatureLocation(jsonTranscript.getJSONObject(FeatureStringEnum.LOCATION.value), sequence)
            Collection<Feature> overlappingFeatures = getOverlappingFeatures(featureLocation).findAll() {
                it = Feature.get(it.id)
                it instanceof Gene
            }

            log.debug "overlapping features: ${overlappingFeatures.size()}"
            for (Feature eachFeature : overlappingFeatures) {
                // get the proper object instead of its proxy, due to lazy loading
                Feature feature = Feature.get(eachFeature.id)
                log.debug "evaluating overlap of feature ${feature.name} of class ${feature.class.name}"

                if (!gene && feature instanceof Gene && !(feature instanceof Pseudogene)) {
                    Gene tmpGene = (Gene) feature;
                    log.debug "found an overlapping gene ${tmpGene}"
                    // removing name from transcript JSON since its naming will be based off of the overlapping gene
                    jsonTranscript.remove(FeatureStringEnum.NAME.value)
                    Transcript tmpTranscript = (Transcript) convertJSONToFeature(jsonTranscript, sequence);
                    updateNewGsolFeatureAttributes(tmpTranscript, sequence);
                    if (tmpTranscript.getFmin() < 0 || tmpTranscript.getFmax() < 0) {
                        throw new AnnotationException("Feature cannot have negative coordinates");
                    }

                    //this one is working, but was marked as needing improvement
                    setOwner(tmpTranscript, owner);

                    if (!useCDS || transcriptService.getCDS(tmpTranscript) == null) {
                        calculateCDS(tmpTranscript);
                    }
                    else {
                        // if there are any sequence alterations that overlaps this transcript then
                        // recalculate the CDS to account for these changes
                        def sequenceAlterations = getSequenceAlterationsForFeature(tmpTranscript)
                        if (sequenceAlterations.size() > 0) {
                            calculateCDS(tmpTranscript)
                        }
                    }

                    if (!suppressHistory) {
                        tmpTranscript.name = nameService.generateUniqueName(tmpTranscript, tmpGene.name)
                    }

                    if (tmpTranscript && tmpGene && overlapperService.overlaps(tmpTranscript, tmpGene)) {
                        log.debug "There is an overlap, adding to an existing gene"
                        transcript = tmpTranscript;
                        gene = tmpGene;
                        addTranscriptToGene(gene, transcript)
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript);
                        transcript.save()

                        if (jsonTranscript.has(FeatureStringEnum.PARENT.value)) {
                            // use metadata of incoming transcript's gene
                            JSONObject jsonGene = jsonTranscript.getJSONObject(FeatureStringEnum.PARENT.value)
                            if (jsonGene.has(FeatureStringEnum.DBXREFS.value)) {
                                // parse dbxrefs
                                JSONArray dbxrefs = jsonGene.getJSONArray(FeatureStringEnum.DBXREFS.value)
                                for (JSONObject dbxref : dbxrefs) {
                                    String dbString = dbxref.get(FeatureStringEnum.DB.value).name
                                    String accessionString = dbxref.get(FeatureStringEnum.ACCESSION.value)
                                    // TODO: needs improvement
                                    boolean exists = false
                                    tmpGene.featureDBXrefs.each {
                                        if (it.db.name == dbString && it.accession == accessionString) {
                                            exists = true
                                        }
                                    }
                                    if (!exists) {
                                        addNonPrimaryDbxrefs(tmpGene, dbString, accessionString)
                                    }
                                }
                            }
                            tmpGene.save()

                            if (jsonGene.has(FeatureStringEnum.PROPERTIES.value)) {
                                // parse properties
                                JSONArray featureProperties = jsonGene.getJSONArray(FeatureStringEnum.PROPERTIES.value)
                                for (JSONObject featureProperty : featureProperties) {
                                    String tagString = featureProperty.get(FeatureStringEnum.TYPE.value).name
                                    String valueString = featureProperty.get(FeatureStringEnum.VALUE.value)
                                    // TODO: needs improvement
                                    boolean exists = false
                                    tmpGene.featureProperties.each {
                                        if (it instanceof Comment) {
                                            exists = true
                                        }
                                        else if (it.tag == tagString && it.value == valueString) {
                                            exists = true
                                        }
                                    }
                                    if (!exists) {
                                        if (tagString == FeatureStringEnum.COMMENT.value) {
                                            // if FeatureProperty is a comment
                                            featurePropertyService.addComment(tmpGene, valueString)
                                        }
                                        else {
                                            addNonReservedProperties(tmpGene, tagString, valueString)
                                        }
                                    }
                                }
                            }
                            tmpGene.save()
                        }
                        gene.save(insert: false, flush: true)
                        break;
                    } else {
                        featureRelationshipService.deleteFeatureAndChildren(tmpTranscript)
                        log.debug "There is no overlap, we are going to return a NULL gene and a NULL transcript "
                    }
                } else {
                    log.error "Feature is not an instance of a gene or is a pseudogene"
                }
            }
        }
        if (gene == null) {
            log.debug "gene is null"
            // Scenario III - create a de-novo gene
            JSONObject jsonGene = new JSONObject();
            if (jsonTranscript.has(FeatureStringEnum.PARENT.value)) {
                // Scenario IIIa - use the 'parent' attribute, if provided, from transcript JSON
                jsonGene = JSON.parse(jsonTranscript.getString(FeatureStringEnum.PARENT.value)) as JSONObject
                jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonTranscript))
            }
            else {
                // Scenario IIIb - use the current mRNA's featurelocation for gene
                jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonTranscript));
                jsonGene.put(FeatureStringEnum.LOCATION.value, jsonTranscript.getJSONObject(FeatureStringEnum.LOCATION.value));
                String cvTermString = FeatureStringEnum.GENE.value
                jsonGene.put(FeatureStringEnum.TYPE.value, convertCVTermToJSON(FeatureStringEnum.CV.value, cvTermString));
            }

            String geneName = null
            if (jsonGene.has(FeatureStringEnum.NAME.value)) {
                geneName = jsonGene.get(FeatureStringEnum.NAME.value)
            }
            else if (jsonTranscript.has(FeatureStringEnum.NAME.value)) {
                geneName = jsonTranscript.getString(FeatureStringEnum.NAME.value)
            }
            else {
//                geneName = nameService.makeUniqueFeatureName(sequence.organism, sequence.name, new LetterPaddingStrategy(), false)
                geneName = nameService.makeUniqueGeneName(sequence.organism, sequence.name, false)
            }
            if (!suppressHistory) {
//                geneName = nameService.makeUniqueFeatureName(sequence.organism, geneName, new LetterPaddingStrategy(), true)
                geneName = nameService.makeUniqueGeneName(sequence.organism, geneName, true)
            }
            // set back to the original gene name
            if (jsonTranscript.has(FeatureStringEnum.GENE_NAME.value)) {
                geneName = jsonTranscript.getString(FeatureStringEnum.GENE_NAME.value)
            }
            jsonGene.put(FeatureStringEnum.NAME.value, geneName)

            gene = (Gene) convertJSONToFeature(jsonGene, sequence);
            updateNewGsolFeatureAttributes(gene, sequence);

            if (gene.getFmin() < 0 || gene.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }
            transcript = transcriptService.getTranscripts(gene).iterator().next();
            if (!useCDS || transcriptService.getCDS(transcript) == null) {
                calculateCDS(transcript);
            }
            else {
                // if there are any sequence alterations that overlaps this transcript then
                // recalculate the CDS to account for these changes
                def sequenceAlterations = getSequenceAlterationsForFeature(transcript)
                if (sequenceAlterations.size() > 0) {
                    calculateCDS(transcript)
                }
            }
            removeExonOverlapsAndAdjacenciesForFeature(gene)
            if (!suppressHistory) {
                transcript.name = nameService.generateUniqueName(transcript)
            }
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript);
            gene.save(insert: true)
            transcript.save(flush: true)

            // doesn't work well for testing
            setOwner(gene, owner);
            setOwner(transcript, owner);
        }
        return transcript;
    }

// TODO: this is kind of a hack for now
    JSONObject convertCVTermToJSON(String cv, String cvTerm) {
        JSONObject jsonCVTerm = new JSONObject();
        JSONObject jsonCV = new JSONObject();
        jsonCVTerm.put(FeatureStringEnum.CV.value, jsonCV);
        jsonCV.put(FeatureStringEnum.NAME.value, cv);
        jsonCVTerm.put(FeatureStringEnum.NAME.value, cvTerm);
        return jsonCVTerm;
    }

    /**
     * TODO: Should be the same result as the older method, need to check:
     *
     *         if (transcript.getGene() != null) {return transcript.getGene();}return transcript;
     * @param feature
     * @return
     */
    @Timed
    Feature getTopLevelFeature(Feature feature) {
        Collection<Feature> parents = feature?.childFeatureRelationships*.parentFeature
        if (parents) {
            return getTopLevelFeature(parents.iterator().next());
        } else {
            return feature;
        }
    }


    @Timed
    @Transactional
    def removeExonOverlapsAndAdjacenciesForFeature(Feature feature) {
        if (feature instanceof Gene) {
            for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                removeExonOverlapsAndAdjacencies(transcript);
            }
        } else if (feature instanceof Transcript) {
            removeExonOverlapsAndAdjacencies((Transcript) feature);
        }
    }

    @Transactional
    def addTranscriptToGene(Gene gene, Transcript transcript) {
        removeExonOverlapsAndAdjacencies(transcript);
        // no feature location, set location to transcript's
        if (gene.getFeatureLocation() == null) {
            FeatureLocation transcriptFeatureLocation = transcript.getFeatureLocation()
            FeatureLocation featureLocation = new FeatureLocation()
            featureLocation.properties = transcriptFeatureLocation.properties
            featureLocation.id = null
            featureLocation.save()
            gene.addToFeatureLocations(featureLocation);
        } else {
            // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
            if (transcript.getFeatureLocation().getFmin() < gene.getFeatureLocation().getFmin()) {
                gene.getFeatureLocation().setFmin(transcript.getFeatureLocation().getFmin());
            }
            if (transcript.getFeatureLocation().getFmax() > gene.getFeatureLocation().getFmax()) {
                gene.getFeatureLocation().setFmax(transcript.getFeatureLocation().getFmax());
            }
        }

        // add transcript
        FeatureRelationship featureRelationship = new FeatureRelationship(
                parentFeature: gene
                , childFeature: transcript
        ).save(failOnError: true)
        gene.addToParentFeatureRelationships(featureRelationship)
        transcript.addToChildFeatureRelationships(featureRelationship)


        updateGeneBoundaries(gene);

//        getSession().indexFeature(transcript);

        // event fire
//        TODO: determine event model?
//        fireAnnotationChangeEvent(transcript, gene, AnnotationChangeEvent.Operation.ADD);
    }

    /**
     * TODO:  this is an N^2  search of overlapping exons
     * @param transcript
     * @return
     */
    @Transactional
    def removeExonOverlapsAndAdjacencies(Transcript transcript) throws AnnotationException {
        List<Exon> sortedExons = transcriptService.getSortedExons(transcript)
        if (!sortedExons || sortedExons?.size() <= 1) {
            return;
        }
        Collections.sort(sortedExons, new FeaturePositionComparator<Exon>(false))
        int inc = 1;
        for (int i = 0; i < sortedExons.size() - 1; i += inc) {
            inc = 1;
            Exon leftExon = sortedExons.get(i);
            for (int j = i + 1; j < sortedExons.size(); ++j) {
                Exon rightExon = sortedExons.get(j);
                if (overlapperService.overlaps(leftExon, rightExon) || isAdjacentTo(leftExon.getFeatureLocation(), rightExon.getFeatureLocation())) {
                    try {
                        exonService.mergeExons(leftExon, rightExon);
                        sortedExons = transcriptService.getSortedExons(transcript)
                        // we have to reload the sortedExons again and start over
                        ++inc;
                    } catch (AnnotationException e) {
                        // we should probably just re-throw this
                        log.error(e)
                        throw e
                    }
                }
            }
        }
    }

/** Checks whether this AbstractSimpleLocationBioFeature is adjacent to the FeatureLocation.
 *
 * @param location - FeatureLocation to check adjacency against
 * @return true if there is adjacency
 */
    boolean isAdjacentTo(FeatureLocation leftLocation, FeatureLocation location) {
        return isAdjacentTo(leftLocation, location, true);
    }

    boolean isAdjacentTo(FeatureLocation leftFeatureLocation, FeatureLocation rightFeatureLocation, boolean compareStrands) {
        if (leftFeatureLocation.sequence != rightFeatureLocation.sequence) {
            return false;
        }
        int thisFmin = leftFeatureLocation.getFmin();
        int thisFmax = leftFeatureLocation.getFmax();
        int thisStrand = leftFeatureLocation.getStrand();
        int otherFmin = rightFeatureLocation.getFmin();
        int otherFmax = rightFeatureLocation.getFmax();
        int otherStrand = rightFeatureLocation.getStrand();
        boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
        if (strandsOverlap &&
                (thisFmax == otherFmin ||
                        thisFmin == otherFmax)) {
            return true;
        }
        return false;
    }


    @Transactional
    def calculateCDS(Transcript transcript) {
        // NOTE: isPseudogene call seemed redundant with isProtenCoding
        calculateCDS(transcript, false)
//        if (transcriptService.isProteinCoding(transcript) && (transcriptService.getGene(transcript) == null)) {
////            calculateCDS(editor, transcript, transcript.getCDS() != null ? transcript.getCDS().getStopCodonReadThrough() != null : false);
////            calculateCDS(transcript, transcript.getCDS() != null ? transcript.getCDS().getStopCodonReadThrough() != null : false);
//            calculateCDS(transcript, transcriptService.getCDS(transcript) != null ? transcriptService.getStopCodonReadThrough(transcript) != null : false);
//        }
    }

    @Timed
    @Transactional
    def calculateCDS(Transcript transcript, boolean readThroughStopCodon) {
        CDS cds = transcriptService.getCDS(transcript);
        log.info "calculateCDS"
        if (cds == null) {
            setLongestORF(transcript, readThroughStopCodon);
            return;
        }
        boolean manuallySetStart = cdsService.isManuallySetTranslationStart(cds);
        boolean manuallySetEnd = cdsService.isManuallySetTranslationEnd(cds);
        if (manuallySetStart && manuallySetEnd) {
            return;
        }
        if (!manuallySetStart && !manuallySetEnd) {
            setLongestORF(transcript, readThroughStopCodon);
        } else if (manuallySetStart) {
            setTranslationStart(transcript, cds.getFeatureLocation().getStrand().equals(-1) ? cds.getFmax() - 1 : cds.getFmin(), true, readThroughStopCodon);
        } else {
            setTranslationEnd(transcript, cds.getFeatureLocation().getStrand().equals(-1) ? cds.getFmin() : cds.getFmax() - 1, true);
        }
    }

/**
 * Calculate the longest ORF for a transcript.  If a valid start codon is not found, allow for partial CDS start/end.
 * Calls setLongestORF(Transcript, TranslationTable, boolean) with the translation table and whether partial
 * ORF calculation extensions are allowed from the configuration associated with this editor.
 *
 * @param transcript - Transcript to set the longest ORF to
 */
    @Transactional
    void setLongestORF(Transcript transcript, boolean readThroughStopCodon) {
        log.debug "setLongestORF(transcript,readThroughStopCodon) ${transcript} ${readThroughStopCodon}"
        setLongestORF(transcript, configWrapperService.getTranslationTable(), false, readThroughStopCodon);
    }

    @Transactional
    void setLongestORF(Transcript transcript) {
        log.debug "setLongestORF(transcript) ${transcript}"
        setLongestORF(transcript, false);
    }

/**
 * Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationStart - Coordinate of the start of translation
 */
    @Transactional
    void setTranslationStart(Transcript transcript, int translationStart) {
        log.debug "setTranslationStart"
        setTranslationStart(transcript, translationStart, false);
    }

/**
 * Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationStart - Coordinate of the start of translation
 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
 */
    @Transactional
    void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd) {
        log.debug "setTranslationStart(transcript,translationStart,translationEnd)"
        setTranslationStart(transcript, translationStart, setTranslationEnd, false);
    }

/**
 * Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationStart - Coordinate of the start of translation
 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
 * @param readThroughStopCodon - if set to true, will read through the first stop codon to the next
 */
    @Transactional
    void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd, boolean readThroughStopCodon) {
        log.debug "setTranslationStart(transcript,translationStart,translationEnd,readThroughStopCodon)"
        setTranslationStart(transcript, translationStart, setTranslationEnd, setTranslationEnd ? configWrapperService.getTranslationTable() : null, readThroughStopCodon);
    }

    /** Convert local coordinate to source feature coordinate.
     *
     * @param localCoordinate - Coordinate to convert to source coordinate
     * @return Source feature coordinate, -1 if local coordinate is longer than feature's length or negative
     */
    int convertLocalCoordinateToSourceCoordinate(Feature feature, int localCoordinate) {
        log.debug "convertLocalCoordinateToSourceCoordinate"

        if (localCoordinate < 0 || localCoordinate > feature.getLength()) {
            return -1;
        }
        if (feature.getFeatureLocation().getStrand() == -1) {
            return feature.getFeatureLocation().getFmax() - localCoordinate - 1;
        } else {
            return feature.getFeatureLocation().getFmin() + localCoordinate;
        }
    }

    int convertLocalCoordinateToSourceCoordinateForTranscript(Transcript transcript, int localCoordinate) {
        // Method converts localCoordinate to sourceCoordinate in reference to the Transcript
        List<Exon> exons = exonService.getSortedExons(transcript)
        int sourceCoordinate = -1;
        if (exons.size() == 0) {
            return convertLocalCoordinateToSourceCoordinate(transcript, localCoordinate);
        }
        int currentLength = 0;
        int currentCoordinate = localCoordinate;
        for (Exon exon : exons) {
            int exonLength = exon.getLength();
            if (currentLength + exonLength >= localCoordinate) {
                if (transcript.getFeatureLocation().getStrand() == Strand.NEGATIVE.value) {
                    sourceCoordinate = exon.getFeatureLocation().getFmax() - currentCoordinate - 1;
                } else {
                    sourceCoordinate = exon.getFeatureLocation().getFmin() + currentCoordinate;
                }
                break;
            }
            currentLength += exonLength;
            currentCoordinate -= exonLength;
        }
        return sourceCoordinate;
    }

    int convertLocalCoordinateToSourceCoordinateForCDS(CDS cds, int localCoordinate) {
        // Method converts localCoordinate to sourceCoordinate in reference to the CDS
        Transcript transcript = transcriptService.getTranscript(cds)
        if (!transcript) {
            return convertLocalCoordinateToSourceCoordinate(cds, localCoordinate);
        }
        int offset = 0;
        List<Exon> exons = exonService.getSortedExons(transcript)
        if (exons.size() == 0) {
            log.debug "FS::convertLocalCoordinateToSourceCoordinateForCDS() - No exons for given transcript"
            return convertLocalCoordinateToSourceCoordinate(cds, localCoordinate)
        }
        if (transcript.strand == Strand.NEGATIVE.value) {
            exons.reverse()
        }
        for (Exon exon : exons) {
            if (!overlapperService.overlaps(cds, exon)) {
                offset += exon.getLength();
                continue;
            } else if (overlapperService.overlaps(cds, exon)) {
                if (exon.fmin >= cds.fmin && exon.fmax <= cds.fmax) {
                    // exon falls within the boundaries of the CDS
                    continue
                } else {
                    // exon doesn't overlap completely with the CDS
                    if (exon.fmin < cds.fmin && exon.strand == Strand.POSITIVE.value) {
                        offset += cds.fmin - exon.fmin
                    } else if (exon.fmax > cds.fmax && exon.strand == Strand.NEGATIVE.value) {
                        offset += exon.fmax - cds.fmax
                    }
                }
            }

            if (exon.getFeatureLocation().getStrand() == Strand.NEGATIVE.value) {
                offset += exon.getFeatureLocation().getFmax() - exon.getFeatureLocation().getFmax();
            } else {
                offset += exon.getFeatureLocation().getFmin() - exon.getFeatureLocation().getFmin();
            }
            break;
        }
        return convertLocalCoordinateToSourceCoordinateForTranscript(transcript, localCoordinate + offset);
    }

/**
 * Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationStart - Coordinate of the start of translation
 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
 * @param translationTable - Translation table that defines the codon translation
 * @param readThroughStopCodon - if set to true, will read through the first stop codon to the next
 */
    @Transactional
    void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd, TranslationTable translationTable, boolean readThroughStopCodon) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            featureRelationshipService.addChildFeature(transcript, cds)
//            transcript.setCDS(cds);
        }
        FeatureLocation transcriptFeatureLocation = FeatureLocation.findByFeature(transcript)
        if (transcriptFeatureLocation.strand == Strand.NEGATIVE.value) {
            setFmax(cds, translationStart + 1);
        } else {
            setFmin(cds, translationStart);
        }
        cdsService.setManuallySetTranslationStart(cds, true);
//        cds.deleteStopCodonReadThrough();
        cdsService.deleteStopCodonReadThrough(cds);
//        featureRelationshipService.deleteRelationships()

        if (setTranslationEnd && translationTable != null) {
            String mrna = getResiduesWithAlterationsAndFrameshifts(transcript);
            if (mrna == null || mrna.equals("null")) {
                return;
            }
            int stopCodonCount = 0;
            for (int i = convertSourceCoordinateToLocalCoordinateForTranscript(transcript, translationStart); i < transcript.getLength(); i += 3) {
                if (i < 0 || i + 3 > mrna.length()) {
                    break;
                }
                String codon = mrna.substring(i, i + 3);

                if (translationTable.getStopCodons().contains(codon)) {
                    if (readThroughStopCodon && ++stopCodonCount < 2) {
//                        StopCodonReadThrough stopCodonReadThrough = cdsService.getStopCodonReadThrough(cds);
                        StopCodonReadThrough stopCodonReadThrough = (StopCodonReadThrough) featureRelationshipService.getChildForFeature(cds, StopCodonReadThrough.ontologyId)
                        if (stopCodonReadThrough == null) {
                            stopCodonReadThrough = cdsService.createStopCodonReadThrough(cds);
                            cdsService.setStopCodonReadThrough(cds, stopCodonReadThrough)
//                            cds.setStopCodonReadThrough(stopCodonReadThrough);
                            if (cds.strand == Strand.NEGATIVE.value) {
                                stopCodonReadThrough.featureLocation.setFmin(convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2));
                                stopCodonReadThrough.featureLocation.setFmax(convertModifiedLocalCoordinateToSourceCoordinate(transcript, i) + 1);
                            } else {
                                stopCodonReadThrough.featureLocation.setFmin(convertModifiedLocalCoordinateToSourceCoordinate(transcript, i));
                                stopCodonReadThrough.featureLocation.setFmax(convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2) + 1);
                            }
                        }
                        continue;
                    }
                    if (transcript.strand == Strand.NEGATIVE.value) {
                        cds.featureLocation.setFmin(convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 2));
                    } else {
                        cds.featureLocation.setFmax(convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 3));
                    }
                    return;
                }
            }
            if (transcript.strand == Strand.NEGATIVE.value) {
                cds.featureLocation.setFmin(transcript.getFmin());
                cds.featureLocation.setIsFminPartial(true);
            } else {
                cds.featureLocation.setFmax(transcript.getFmax());
                cds.featureLocation.setIsFmaxPartial(true);
            }
        }

        Date date = new Date();
        cds.setLastUpdated(date);
        transcript.setLastUpdated(date);

        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }

/** Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
 *  Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationEnd - Coordinate of the end of translation
 */
/*
public void setTranslationEnd(Transcript transcript, int translationEnd) {
    CDS cds = transcript.getCDS();
    if (cds == null) {
        cds = createCDS(transcript);
        transcript.setCDS(cds);
    }
    if (transcript.getStrand() == -1) {
        cds.setFmin(translationEnd + 1);
    }
    else {
        cds.setFmax(translationEnd);
    }
    setManuallySetTranslationEnd(cds, true);
    cds.deleteStopCodonReadThrough();

    // event fire
    fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

}
*/

/**
 * Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation end in
 * @param translationEnd - Coordinate of the end of translation
 */
    @Transactional
    void setTranslationEnd(Transcript transcript, int translationEnd) {
        setTranslationEnd(transcript, translationEnd, false);
    }

/**
 * Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation end in
 * @param translationEnd - Coordinate of the end of translation
 * @param setTranslationStart - if set to true, will search for the nearest in frame start
 */
    @Transactional
    void setTranslationEnd(Transcript transcript, int translationEnd, boolean setTranslationStart) {
        setTranslationEnd(transcript, translationEnd, setTranslationStart,
                setTranslationStart ? configWrapperService.getTranslationTable() : null
        );
    }

/**
 * Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
 * Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation end in
 * @param translationEnd - Coordinate of the end of translation
 * @param setTranslationStart - if set to true, will search for the nearest in frame start codon
 * @param translationTable - Translation table that defines the codon translation
 */
    @Transactional
    void setTranslationEnd(Transcript transcript, int translationEnd, boolean setTranslationStart, TranslationTable translationTable) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            transcriptService.setCDS(transcript, cds);
        }
        if (transcript.strand == Strand.NEGATIVE.value) {
            cds.featureLocation.setFmin(translationEnd);
        } else {
            cds.featureLocation.setFmax(translationEnd + 1);
        }
        cdsService.setManuallySetTranslationEnd(cds, true);
        cdsService.deleteStopCodonReadThrough(cds);
        if (setTranslationStart && translationTable != null) {
            String mrna = getResiduesWithAlterationsAndFrameshifts(transcript);
            if (mrna == null || mrna.equals("null")) {
                return;
            }
            for (int i = convertSourceCoordinateToLocalCoordinateForTranscript(transcript, translationEnd) - 3; i >= 0; i -= 3) {
                if (i - 3 < 0) {
                    break;
                }
                String codon = mrna.substring(i, i + 3);
                if (translationTable.getStartCodons().contains(codon)) {
                    if (transcript.strand == Strand.NEGATIVE.value) {
                        cds.featureLocation.setFmax(convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 3));
                    } else {
                        cds.featureLocation.setFmin(convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 2));
                    }
                    return;
                }
            }
            if (transcript.strand == Strand.NEGATIVE.value) {
                cds.featureLocation.setFmin(transcript.getFmin());
                cds.featureLocation.setIsFminPartial(true);
            } else {
                cds.featureLocation.setFmax(transcript.getFmax());
                cds.featureLocation.setIsFmaxPartial(true);
            }
        }

        Date date = new Date();
        cds.setLastUpdated(date);
        transcript.setLastUpdated(date);

        // event fire TODO: ??
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }

    @Transactional
    void setTranslationFmin(Transcript transcript, int translationFmin) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            transcriptService.setCDS(transcript, cds);
        }
        setFmin(cds, translationFmin);
        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
    }

    @Transactional
    void setTranslationFmax(Transcript transcript, int translationFmax) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            transcriptService.setCDS(transcript, cds);
        }
        setFmax(cds, translationFmax);

        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }

/**
 * Set the translation start and end in the transcript.  Sets the translation start and end in the underlying CDS
 * feature.  Instantiates the CDS object for the transcript if it doesn't already exist.
 *
 * @param transcript - Transcript to set the translation start in
 * @param translationStart - Coordinate of the start of translation
 * @param translationEnd - Coordinate of the end of translation
 * @param manuallySetStart - whether the start was manually set
 * @param manuallySetEnd - whether the end was manually set
 */
    @Transactional
    void setTranslationEnds(Transcript transcript, int translationStart, int translationEnd, boolean manuallySetStart, boolean manuallySetEnd) {
        setTranslationFmin(transcript, translationStart);
        setTranslationFmax(transcript, translationEnd);
        cdsService.setManuallySetTranslationStart(transcriptService.getCDS(transcript), manuallySetStart);
        cdsService.setManuallySetTranslationEnd(transcriptService.getCDS(transcript), manuallySetEnd);

        Date date = new Date();
        transcriptService.getCDS(transcript).setLastUpdated(date);
        transcript.setLastUpdated(date);

        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }


    void setLongestORF(Transcript transcript, TranslationTable translationTable, boolean allowPartialExtension) {
        log.debug "setLongestORF(transcript,translationTable,allowPartialExtension)"
        setLongestORF(transcript, translationTable, allowPartialExtension, false);
    }

    /** Get the residues for a feature with any alterations and frameshifts.
     *
     * @param feature - Feature to retrieve the residues for
     * @return Residues for the feature with any alterations and frameshifts
     */
    String getResiduesWithAlterationsAndFrameshifts(Feature feature) {
        if (!(feature instanceof CDS)) {
            return getResiduesWithAlterations(feature, getSequenceAlterationsForFeature(feature))
        }
        Transcript transcript = (Transcript) featureRelationshipService.getParentForFeature(feature, Transcript.ontologyId)
        Collection<SequenceAlteration> alterations = getFrameshiftsAsAlterations(transcript);
        List<SequenceAlteration> allSequenceAlterationList = getSequenceAlterationsForFeature(feature)
        alterations.addAll(allSequenceAlterationList);
        return getResiduesWithAlterations(feature, alterations)
    }

    /**
     // TODO: should be a single query here, currently 194 ms
     * Get all sequenceAlterations associated with a feature.
     * Basically I want to include all upstream alterations on a sequence for that feature
     * @param feature
     * @return
     */
    List<SequenceAlteration> getAllSequenceAlterationsForFeature(Feature feature) {
        List<Sequence> sequence = Sequence.executeQuery("select s from Feature  f join f.featureLocations fl join fl.sequence s where f = :feature ", [feature: feature])
        SequenceAlteration.executeQuery("select sa from SequenceAlteration sa join sa.featureLocations fl join fl.sequence s where s = :sequence order by fl.fmin asc ", [sequence: sequence])
    }

    List<SequenceAlteration> getFrameshiftsAsAlterations(Transcript transcript) {
        List<SequenceAlteration> frameshifts = new ArrayList<SequenceAlteration>();
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            return frameshifts;
        }
        Sequence sequence = cds.getFeatureLocation().sequence
        List<Frameshift> frameshiftList = transcriptService.getFrameshifts(transcript)
        for (Frameshift frameshift : frameshiftList) {
            if (frameshift.isPlusFrameshift()) {
                // a plus frameshift skips bases during translation, which can be mapped to a deletion for the
                // the skipped bases
//                Deletion deletion = new Deletion(cds.getOrganism(), "Deletion-" + frameshift.getCoordinate(), false,
//                        false, new Timestamp(new Date().getTime()), cds.getConfiguration());

                FeatureLocation featureLocation = new FeatureLocation(
                        fmin: frameshift.coordinate
                        , fmax: frameshift.coordinate + frameshift.frameshiftValue
                        , strand: cds.featureLocation.strand
                        , sequence: sequence
                )

                Deletion deletion = new Deletion(
                        uniqueName: FeatureStringEnum.DELETION_PREFIX.value + frameshift.coordinate
                        , isObsolete: false
                        , isAnalysis: false
                )

                featureLocation.feature = deletion
                deletion.addToFeatureLocations(featureLocation)

                frameshifts.add(deletion);
                featureLocation.save()
                deletion.save()
                frameshift.save(flush: true)

//                deletion.setFeatureLocation(frameshift.getCoordinate(),
//                        frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
//                        cds.getFeatureLocation().getStrand(), sourceFeature);


            } else {
                // a minus frameshift goes back bases during translation, which can be mapped to an insertion for the
                // the repeated bases
                Insertion insertion = new Insertion(
                        uniqueName: FeatureStringEnum.INSERTION_PREFIX.value + frameshift.coordinate
                        , isAnalysis: false
                        , isObsolete: false
                ).save()

//                Insertion insertion = new Insertion(cds.getOrganism(), "Insertion-" + frameshift.getCoordinate(), false,
//                        false, new Timestamp(new Date().getTime()), cds.getConfiguration());

                FeatureLocation featureLocation = new FeatureLocation(
                        fmin: frameshift.coordinate
                        , fmax: frameshift.coordinate + frameshift.frameshiftValue
                        , strand: cds.featureLocation.strand
                        , sequence: sequence
                ).save()

                insertion.addToFeatureLocations(featureLocation)
                featureLocation.feature = insertion

//                insertion.setFeatureLocation(frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
//                        frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
//                        cds.getFeatureLocation().getStrand(), sourceFeature);

                String alterationResidues = sequenceService.getRawResiduesFromSequence(sequence, frameshift.getCoordinate() + frameshift.getFrameshiftValue(), frameshift.getCoordinate())
                insertion.alterationResidue = alterationResidues
                // TODO: correct?
//                insertion.setResidues(sequence.getResidues().substring(
//                        frameshift.getCoordinate() + frameshift.getFrameshiftValue(), frameshift.getCoordinate()));
                frameshifts.add(insertion);

                insertion.save()
                featureLocation.save()
                frameshift.save(flush: true)
            }
        }
        return frameshifts;
    }

/*
 * Calculate the longest ORF for a transcript.  If a valid start codon is not found, allow for partial CDS start/end.
 *
 * @param transcript - Transcript to set the longest ORF to
 * @param translationTable - Translation table that defines the codon translation
 * @param allowPartialExtension - Where partial ORFs should be used for possible extension
 *
 */
    @Timed
    @Transactional
    void setLongestORF(Transcript transcript, TranslationTable translationTable, boolean allowPartialExtension, boolean readThroughStopCodon) {
        String mrna = getResiduesWithAlterationsAndFrameshifts(transcript);
        if (!mrna) {
            return;
        }
        String longestPeptide = "";
        int bestStartIndex = -1;
        int bestStopIndex = -1;
        int startIndex = -1;
        int stopIndex = -1;
        boolean partialStart = false;
        boolean partialStop = false;

        if (mrna.length() > 3) {
            for (String startCodon : translationTable.getStartCodons()) {
                // find the first start codon
                startIndex = mrna.indexOf(startCodon)
                while(startIndex >= 0) {
                    String mrnaSubstring = mrna.substring(startIndex)
                    String aa = SequenceTranslationHandler.translateSequence(mrnaSubstring, translationTable, true, readThroughStopCodon)
                    if (aa.length() > longestPeptide.length()) {
                        longestPeptide = aa
                        bestStartIndex = startIndex
                    }
                    startIndex = mrna.indexOf(startCodon, startIndex + 1)
                }
            }

            // Just in case the 5' end is missing, check to see if a longer
            // translation can be obatained without looking for a start codon
            startIndex = 0
            while(startIndex < 3) {
                String mrnaSubstring = mrna.substring(startIndex)
                String aa = SequenceTranslationHandler.translateSequence(mrnaSubstring, translationTable, true, readThroughStopCodon)
                if (aa.length() > longestPeptide.length()) {
                    partialStart = true
                    longestPeptide = aa
                    bestStartIndex = startIndex
                }
                startIndex++
            }
        }

        // check for partial stop
        if (!longestPeptide.substring(longestPeptide.length() - 1).equals(TranslationTable.STOP)) {
            partialStop = true
            bestStopIndex = -1
        }
        else {
            stopIndex = bestStartIndex + (longestPeptide.length() * 3)
            partialStop = false
            bestStopIndex = stopIndex
        }

        log.debug "bestStartIndex: ${bestStartIndex} bestStopIndex: ${bestStopIndex}; partialStart: ${partialStart} partialStop: ${partialStop}"

        if (transcript instanceof MRNA) {
            CDS cds = transcriptService.getCDS(transcript)
            if (cds == null) {
                cds = transcriptService.createCDS(transcript);
                transcriptService.setCDS(transcript, cds);
            }

            int fmin = convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStartIndex)

            if (bestStopIndex >= 0) {
                log.debug "bestStopIndex >= 0"
                int fmax = convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStopIndex)
                if (cds.strand == Strand.NEGATIVE.value) {
                    int tmp = fmin
                    fmin = fmax + 1
                    fmax = tmp + 1
                }
                setFmin(cds, fmin)
                setFmax(cds, fmax)
            }
            else {
                log.debug "bestStopIndex < 0"
                int fmax = transcript.strand == Strand.NEGATIVE.value ? transcript.fmin : transcript.fmax
                if (cds.strand == Strand.NEGATIVE.value) {
                    int tmp = fmin
                    fmin = fmax
                    fmax = tmp + 1
                }
                setFmin(cds, fmin)
                setFmax(cds, fmax)
            }

            if (cds.featureLocation.strand == Strand.NEGATIVE.value) {
                cds.featureLocation.setIsFminPartial(partialStop)
                cds.featureLocation.setIsFmaxPartial(partialStart)
            }
            else {
                cds.featureLocation.setIsFminPartial(partialStart)
                cds.featureLocation.setIsFmaxPartial(partialStop)
            }

            log.debug "Final CDS fmin: ${cds.fmin} fmax: ${cds.fmax}"

            if (readThroughStopCodon) {
                String aa = SequenceTranslationHandler.translateSequence(getResiduesWithAlterationsAndFrameshifts(cds), translationTable, true, true);
                int firstStopIndex = aa.indexOf(TranslationTable.STOP);
                if (firstStopIndex < aa.length() - 1) {
                    StopCodonReadThrough stopCodonReadThrough = cdsService.createStopCodonReadThrough(cds);
                    cdsService.setStopCodonReadThrough(cds, stopCodonReadThrough);
                    int offset = transcript.getStrand() == -1 ? -2 : 0;
                    setFmin(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + offset);
                    setFmax(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + 3 + offset);
                }
            } else {
                cdsService.deleteStopCodonReadThrough(cds);
            }
            cdsService.setManuallySetTranslationStart(cds, false);
            cdsService.setManuallySetTranslationEnd(cds, false);
        }
    }


    @Timed
    @Transactional
    Feature convertJSONToFeature(JSONObject jsonFeature, Sequence sequence) {
        Feature gsolFeature
        try {
            JSONObject type = jsonFeature.getJSONObject(FeatureStringEnum.TYPE.value);
            String ontologyId = convertJSONToOntologyId(type)
            if (!ontologyId) {
                log.warn "Feature type not set for ${type}"
                return null
            }

            gsolFeature = generateFeatureForType(ontologyId)
            if (jsonFeature.has(FeatureStringEnum.ID.value)) {
                gsolFeature.setId(jsonFeature.getLong(FeatureStringEnum.ID.value));
            }

            if (jsonFeature.has(FeatureStringEnum.UNIQUENAME.value)) {
                gsolFeature.setUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value));
            } else {
                gsolFeature.setUniqueName(nameService.generateUniqueName());
            }
            if (jsonFeature.has(FeatureStringEnum.NAME.value)) {
                gsolFeature.setName(jsonFeature.getString(FeatureStringEnum.NAME.value));
            }
            else {
                // since name attribute cannot be null, using the feature's own uniqueName
                gsolFeature.name = gsolFeature.uniqueName
            }
            if (jsonFeature.has(FeatureStringEnum.SYMBOL.value)) {
                gsolFeature.setSymbol(jsonFeature.getString(FeatureStringEnum.SYMBOL.value));
            }
            if (jsonFeature.has(FeatureStringEnum.DESCRIPTION.value)) {
                gsolFeature.setDescription(jsonFeature.getString(FeatureStringEnum.DESCRIPTION.value));
            }
            if (gsolFeature instanceof Deletion) {
                int deletionLength = jsonFeature.location.fmax - jsonFeature.location.fmin
                gsolFeature.deletionLength = deletionLength
            }

            gsolFeature.save(failOnError: true)


            if (jsonFeature.has(FeatureStringEnum.LOCATION.value)) {
                JSONObject jsonLocation = jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value);
                FeatureLocation featureLocation
                if (singletonFeatureTypes.contains(type.getString(FeatureStringEnum.NAME.value))) {
                    featureLocation = convertJSONToFeatureLocation(jsonLocation, sequence, Strand.NONE.value)
                } else {
                    featureLocation = convertJSONToFeatureLocation(jsonLocation, sequence)
                }
                featureLocation.sequence = sequence
                featureLocation.feature = gsolFeature
                featureLocation.save()
                gsolFeature.addToFeatureLocations(featureLocation);
            }

            if (gsolFeature instanceof Deletion) {
                sequenceService.setResiduesForFeatureFromLocation((Deletion) gsolFeature)
            } else if (jsonFeature.has(FeatureStringEnum.RESIDUES.value) && gsolFeature instanceof SequenceAlteration) {
                sequenceService.setResiduesForFeature(gsolFeature, jsonFeature.getString(FeatureStringEnum.RESIDUES.value))
            }

            if (jsonFeature.has(FeatureStringEnum.CHILDREN.value)) {
                JSONArray children = jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value);
                log.debug "jsonFeature ${jsonFeature} has ${children?.size()} children"
                for (int i = 0; i < children.length(); ++i) {
                    JSONObject childObject = children.getJSONObject(i)
                    Feature child = convertJSONToFeature(childObject, sequence);
                    // if it retuns null, we ignore it
                    if (child) {
                        child.save(failOnError: true)
                        FeatureRelationship fr = new FeatureRelationship();
                        fr.setParentFeature(gsolFeature);
                        fr.setChildFeature(child);
                        fr.save(failOnError: true)
                        gsolFeature.addToParentFeatureRelationships(fr);
                        child.addToChildFeatureRelationships(fr);
                        child.save()
                    }
                    gsolFeature.save()
                }
            }
            if (jsonFeature.has(FeatureStringEnum.TIMEACCESSION.value)) {
                gsolFeature.setDateCreated(new Date(jsonFeature.getInt(FeatureStringEnum.TIMEACCESSION.value)));
            } else {
                gsolFeature.setDateCreated(new Date());
            }
            if (jsonFeature.has(FeatureStringEnum.TIMELASTMODIFIED.value)) {
                gsolFeature.setLastUpdated(new Date(jsonFeature.getInt(FeatureStringEnum.TIMELASTMODIFIED.value)));
            } else {
                gsolFeature.setLastUpdated(new Date());
            }
            if (jsonFeature.has(FeatureStringEnum.PROPERTIES.value)) {
                JSONArray properties = jsonFeature.getJSONArray(FeatureStringEnum.PROPERTIES.value);
                for (int i = 0; i < properties.length(); ++i) {
                    JSONObject property = properties.getJSONObject(i);
                    JSONObject propertyType = property.getJSONObject(FeatureStringEnum.TYPE.value);
                    String propertyName = ""
                    if (property.has(FeatureStringEnum.NAME.value)) {
                        propertyName = property.get(FeatureStringEnum.NAME.value)
                    }
                    else {
                        propertyName = propertyType.get(FeatureStringEnum.NAME.value)
                    }
                    String propertyValue = property.get(FeatureStringEnum.VALUE.value)

                    FeatureProperty gsolProperty = null;
                    if (propertyName == FeatureStringEnum.STATUS.value) {
                        // property of type 'Status'
                        AvailableStatus availableStatus = AvailableStatus.findByValue(propertyValue)
                        if (availableStatus) {
                            Status status = new Status(
                                    value: availableStatus.value,
                                    feature: gsolFeature
                            ).save(failOnError: true)
                            gsolFeature.status = status
                            gsolFeature.save()
                        }
                        else {
                            log.warn "Ignoring status ${propertyValue} as its not defined."
                        }
                    }
                    else {
                        if (propertyName == FeatureStringEnum.COMMENT.value) {
                            // property of type 'Comment'
                            gsolProperty = new Comment();
                        } else {
                            gsolProperty = new FeatureProperty();
                        }

                        if (propertyType.has(FeatureStringEnum.NAME.value)) {
                            CV cv = CV.findByName(propertyType.getJSONObject(FeatureStringEnum.CV.value).getString(FeatureStringEnum.NAME.value))
                            CVTerm cvTerm = CVTerm.findByNameAndCv(propertyType.getString(FeatureStringEnum.NAME.value), cv)
                            gsolProperty.setType(cvTerm);
                        } else {
                            log.warn "No proper type for the CV is set ${propertyType as JSON}"
                        }
                        gsolProperty.setTag(propertyName)
                        gsolProperty.setValue(propertyValue)
                        gsolProperty.setFeature(gsolFeature);

                        int rank = 0;
                        for (FeatureProperty fp : gsolFeature.getFeatureProperties()) {
                            if (fp.getType().equals(gsolProperty.getType())) {
                                if (fp.getRank() > rank) {
                                    rank = fp.getRank();
                                }
                            }
                        }
                        gsolProperty.setRank(rank + 1);
                        gsolProperty.save()
                        gsolFeature.addToFeatureProperties(gsolProperty);
                    }
                }
            }
            if (jsonFeature.has(FeatureStringEnum.DBXREFS.value)) {
                JSONArray dbxrefs = jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value);
                for (int i = 0; i < dbxrefs.length(); ++i) {
                    JSONObject dbxref = dbxrefs.getJSONObject(i);
                    JSONObject db = dbxref.getJSONObject(FeatureStringEnum.DB.value);


                    DB newDB = DB.findOrSaveByName(db.getString(FeatureStringEnum.NAME.value))
                    DBXref newDBXref = DBXref.findOrSaveByDbAndAccession(
                            newDB,
                            dbxref.getString(FeatureStringEnum.ACCESSION.value)
                    ).save()
                    gsolFeature.addToFeatureDBXrefs(newDBXref)
                    gsolFeature.save()
                }
            }
        }
        catch (JSONException e) {
            log.error("Exception creating Feature from JSON ${jsonFeature}", e)
            return null;
        }
        return gsolFeature;
    }


    String getCvTermFromFeature(Feature feature) {
        String cvTerm = feature.hasProperty(FeatureStringEnum.ALTERNATECVTERM.value) ? feature.getProperty(FeatureStringEnum.ALTERNATECVTERM.value) : feature.cvTerm
        return cvTerm
    }

    boolean isJsonTranscript(JSONObject jsonObject) {
        JSONObject typeObject = jsonObject.getJSONObject(FeatureStringEnum.TYPE.value)
        String typeString = typeObject.getString(FeatureStringEnum.NAME.value)
        if (typeString == MRNA.cvTerm || typeString == MRNA.alternateCvTerm) {
            return true
        } else {
            return false
        }
    }

    // TODO: (perform on client side, slightly ugly)
    Feature generateFeatureForType(String ontologyId) {
        switch (ontologyId) {
            case MRNA.ontologyId: return new MRNA()
            case MiRNA.ontologyId: return new MiRNA()
            case NcRNA.ontologyId: return new NcRNA()
            case SnoRNA.ontologyId: return new SnoRNA()
            case SnRNA.ontologyId: return new SnRNA()
            case RRNA.ontologyId: return new RRNA()
            case TRNA.ontologyId: return new TRNA()
            case Exon.ontologyId: return new Exon()
            case CDS.ontologyId: return new CDS()
            case Intron.ontologyId: return new Intron()
            case Gene.ontologyId: return new Gene()
            case Pseudogene.ontologyId: return new Pseudogene()
            case Transcript.ontologyId: return new Transcript()
            case TransposableElement.ontologyId: return new TransposableElement()
            case RepeatRegion.ontologyId: return new RepeatRegion()
            case Insertion.ontologyId: return new Insertion()
            case Deletion.ontologyId: return new Deletion()
            case Substitution.ontologyId: return new Substitution()
            case NonCanonicalFivePrimeSpliceSite.ontologyId: return new NonCanonicalFivePrimeSpliceSite()
            case NonCanonicalThreePrimeSpliceSite.ontologyId: return new NonCanonicalThreePrimeSpliceSite()
            case StopCodonReadThrough.ontologyId: return new StopCodonReadThrough()
            default:
                log.error("No feature type exists for ${ontologyId}")
                return null
        }
    }


    String convertJSONToOntologyId(JSONObject jsonCVTerm) {
        String cvString = jsonCVTerm.getJSONObject(FeatureStringEnum.CV.value).getString(FeatureStringEnum.NAME.value)
        String cvTermString = jsonCVTerm.getString(FeatureStringEnum.NAME.value)

        if (cvString.equalsIgnoreCase(FeatureStringEnum.CV.value) || cvString.equalsIgnoreCase(FeatureStringEnum.SEQUENCE.value)) {
            switch (cvTermString.toUpperCase()) {
                case MRNA.cvTerm.toUpperCase(): return MRNA.ontologyId
                case MiRNA.cvTerm.toUpperCase(): return MiRNA.ontologyId
                case NcRNA.cvTerm.toUpperCase(): return NcRNA.ontologyId
                case SnoRNA.cvTerm.toUpperCase(): return SnoRNA.ontologyId
                case SnRNA.cvTerm.toUpperCase(): return SnRNA.ontologyId
                case RRNA.cvTerm.toUpperCase(): return RRNA.ontologyId
                case TRNA.cvTerm.toUpperCase(): return TRNA.ontologyId
                case Transcript.cvTerm.toUpperCase(): return Transcript.ontologyId
                case Gene.cvTerm.toUpperCase(): return Gene.ontologyId
                case Exon.cvTerm.toUpperCase(): return Exon.ontologyId
                case CDS.cvTerm.toUpperCase(): return CDS.ontologyId
                case Intron.cvTerm.toUpperCase(): return Intron.ontologyId
                case Pseudogene.cvTerm.toUpperCase(): return Pseudogene.ontologyId
                case TransposableElement.alternateCvTerm.toUpperCase():
                case TransposableElement.cvTerm.toUpperCase(): return TransposableElement.ontologyId
                case RepeatRegion.alternateCvTerm.toUpperCase():
                case RepeatRegion.cvTerm.toUpperCase(): return RepeatRegion.ontologyId
                case Insertion.cvTerm.toUpperCase(): return Insertion.ontologyId
                case Deletion.cvTerm.toUpperCase(): return Deletion.ontologyId
                case Substitution.cvTerm.toUpperCase(): return Substitution.ontologyId
                case StopCodonReadThrough.cvTerm.toUpperCase(): return StopCodonReadThrough.ontologyId
                case NonCanonicalFivePrimeSpliceSite.cvTerm.toUpperCase(): return NonCanonicalFivePrimeSpliceSite.ontologyId
                case NonCanonicalThreePrimeSpliceSite.cvTerm.toUpperCase(): return NonCanonicalThreePrimeSpliceSite.ontologyId
                case NonCanonicalFivePrimeSpliceSite.alternateCvTerm.toUpperCase(): return NonCanonicalFivePrimeSpliceSite.ontologyId
                case NonCanonicalThreePrimeSpliceSite.alternateCvTerm.toUpperCase(): return NonCanonicalThreePrimeSpliceSite.ontologyId
                default:
                    log.error("CV Term not known ${cvTermString} for CV ${FeatureStringEnum.SEQUENCE}")
                    return null
            }
        } else {
            log.error("CV not known ${cvString}")
        }

        return null

    }

    @Transactional
    void updateGeneBoundaries(Gene gene) {
        log.debug "updateGeneBoundaries"
        if (gene == null) {
            return;
        }
        int geneFmax = Integer.MIN_VALUE;
        int geneFmin = Integer.MAX_VALUE;
        for (Transcript t : transcriptService.getTranscripts(gene)) {
            if (t.getFmin() < geneFmin) {
                geneFmin = t.getFmin();
            }
            if (t.getFmax() > geneFmax) {
                geneFmax = t.getFmax();
            }
        }
        gene.featureLocation.setFmin(geneFmin);
        gene.featureLocation.setFmax(geneFmax);
        gene.setLastUpdated(new Date());
    }

    @Transactional
    def setFmin(Feature feature, int fmin) {
        feature.getFeatureLocation().setFmin(fmin);
    }

    @Transactional
    def setFmax(Feature feature, int fmax) {
        feature.getFeatureLocation().setFmax(fmax);
    }

    /** Convert source feature coordinate to local coordinate.
     *
     * @param sourceCoordinate - Coordinate to convert to local coordinate
     * @return Local coordinate, -1 if source coordinate is <= fmin or >= fmax
     */
    int convertSourceCoordinateToLocalCoordinate(Feature feature, int sourceCoordinate) {
        return convertSourceCoordinateToLocalCoordinate(feature.featureLocation.fmin, feature.featureLocation.fmax, Strand.getStrandForValue(feature.featureLocation.strand), sourceCoordinate)
    }

    int convertSourceCoordinateToLocalCoordinate(int fmin, int fmax, Strand strand, int sourceCoordinate) {
        if (sourceCoordinate < fmin || sourceCoordinate > fmax) {
            return -1;
        }
        if (strand == Strand.NEGATIVE) {
            return fmax - 1 - sourceCoordinate;
        } else {
            return sourceCoordinate - fmin;
        }
    }

    int convertSourceCoordinateToLocalCoordinateForTranscript(Feature feature, int sourceCoordinate) {
        List<Exon> exons = exonService.getSortedExons(feature)
        int localCoordinate = -1
        int currentCoordinate = 0
        for (Exon exon : exons) {
            if (exon.fmin <= sourceCoordinate && exon.fmax >= sourceCoordinate) {
                //sourceCoordinate falls within the exon
                if (exon.strand == Strand.NEGATIVE.value) {
                    localCoordinate = currentCoordinate + (exon.fmax - sourceCoordinate) - 1;
                } else {
                    localCoordinate = currentCoordinate + (sourceCoordinate - exon.fmin);
                }
            }
            currentCoordinate += exon.getLength();
        }
        return localCoordinate
    }


    int convertSourceCoordinateToLocalCoordinateForCDS(Feature feature, int sourceCoordinate) {
        List<Exon> exons = exonService.getSortedExons(feature, true)
        CDS cds = transcriptService.getCDS(feature)
        int localCoordinate = 0

        if (!(cds.fmin <= sourceCoordinate && cds.fmax >= sourceCoordinate)) {
            return -1
        }
        int x = 0
        int y = 0
        if (feature.strand == Strand.POSITIVE.value) {
            for (Exon exon : exons) {
                if (overlapperService.overlaps(exon, cds, true) && exon.fmin >= cds.fmin && exon.fmax <= cds.fmax) {
                    // complete overlap
                    x = exon.fmin
                    y = exon.fmax
                } else if (overlapperService.overlaps(exon, cds, true)) {
                    // partial overlap
                    if (exon.fmin < cds.fmin && exon.fmax < cds.fmax) {
                        x = cds.fmin
                        y = exon.fmax
                    } else {
                        //exon.fmin > cds.fmin && exon.fmax > cds.fmax
                        x = exon.fmin
                        y = cds.fmax
                    }
                } else {
                    // no overlap
                    continue
                }

                if (x <= sourceCoordinate && y >= sourceCoordinate) {
                    localCoordinate += sourceCoordinate - x
                    return localCoordinate
                } else {
                    localCoordinate += y - x
                }
            }
        } else {
            for (Exon exon : exons) {
                if (overlapperService.overlaps(exon, cds, true) && exon.fmin >= cds.fmin && exon.fmax <= cds.fmax) {
                    // complete overlap
                    x = exon.fmax
                    y = exon.fmin
                } else if (overlapperService.overlaps(exon, cds, true)) {
                    // partial overlap
                    //x = cds.fmax
                    //y = exon.fmin
                    if (exon.fmin <= cds.fmin && exon.fmax <= cds.fmax) {
                        x = exon.fmax
                        y = cds.fmin
                    } else {
                        //exon.fmin > cds.fmin && exon.fmax > cds.fmax
                        x = cds.fmax
                        y = exon.fmin
                    }
                } else {
                    // no overlap
                    continue
                }
                if (y <= sourceCoordinate && x >= sourceCoordinate) {
                    localCoordinate += (x - sourceCoordinate) - 1
                    return localCoordinate
                } else {
                    localCoordinate += (x - y)
                }
            }
        }
    }


    void removeFeatureRelationship(Transcript transcript, Feature feature) {

        FeatureRelationship featureRelationship = FeatureRelationship.findByParentFeatureAndChildFeature(transcript, feature)
        if (featureRelationship) {
            FeatureRelationship.deleteAll()
        }
    }

    /**
     * @param gsolFeature
     * @param includeSequence
     * @return
     */
    @Timed
    JSONObject convertFeatureToJSONLite(Feature gsolFeature, boolean includeSequence = false, int depth) {
        JSONObject jsonFeature = new JSONObject();
        if (gsolFeature.id) {
            jsonFeature.put(FeatureStringEnum.ID.value, gsolFeature.id);
        }
        jsonFeature.put(FeatureStringEnum.TYPE.value, generateJSONFeatureStringForType(gsolFeature.ontologyId));
        jsonFeature.put(FeatureStringEnum.UNIQUENAME.value, gsolFeature.getUniqueName());
        if (gsolFeature.getName() != null) {
            jsonFeature.put(FeatureStringEnum.NAME.value, gsolFeature.getName());
        }
        if (gsolFeature.symbol) {
            jsonFeature.put(FeatureStringEnum.SYMBOL.value, gsolFeature.symbol);
        }
        if (gsolFeature.description) {
            jsonFeature.put(FeatureStringEnum.DESCRIPTION.value, gsolFeature.description);
        }
        long start = System.currentTimeMillis();
        if (depth <= 1) {
            String finalOwnerString
            if (gsolFeature.owners) {
                String ownerString = ""
                for (owner in gsolFeature.owners) {
                    ownerString += gsolFeature.owner.username + " "
                }
                finalOwnerString = ownerString?.trim()
            } else if (gsolFeature.owner) {
                finalOwnerString = gsolFeature?.owner?.username
            } else {
                finalOwnerString = "None"
            }
            jsonFeature.put(FeatureStringEnum.OWNER.value.toLowerCase(), finalOwnerString);
        }

        if (gsolFeature.featureLocation) {
            jsonFeature.put(FeatureStringEnum.SEQUENCE.value, gsolFeature.featureLocation.sequence.name);
            jsonFeature.put(FeatureStringEnum.LOCATION.value, convertFeatureLocationToJSON(gsolFeature.featureLocation));
        }


        if (depth <= 1) {
            List<Feature> childFeatures = featureRelationshipService.getChildrenForFeatureAndTypes(gsolFeature)
            if (childFeatures) {
                JSONArray children = new JSONArray();
                jsonFeature.put(FeatureStringEnum.CHILDREN.value, children);
                for (Feature f : childFeatures) {
                    Feature childFeature = f
                    children.put(convertFeatureToJSONLite(childFeature, includeSequence, depth + 1));
                }
            }
        }



        jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, gsolFeature.lastUpdated.time);
        jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, gsolFeature.dateCreated.time);
        return jsonFeature;
    }

    String generateOwnerString(Feature feature){
        if(feature.owner){
          return feature.owner.username
        }
        if (feature.owners) {
            String ownerString = ""
            for (owner in feature.owners) {
                ownerString += owner.username + " "
            }
            return ownerString
        }
        return "None"
    }

    /**
     * @param gsolFeature
     * @param includeSequence
     * @return
     */
    @Timed
    JSONObject convertFeatureToJSON(Feature gsolFeature, boolean includeSequence = false) {
        JSONObject jsonFeature = new JSONObject();
        if (gsolFeature.id) {
            jsonFeature.put(FeatureStringEnum.ID.value, gsolFeature.id);
        }
        jsonFeature.put(FeatureStringEnum.TYPE.value, generateJSONFeatureStringForType(gsolFeature.ontologyId));
        jsonFeature.put(FeatureStringEnum.UNIQUENAME.value, gsolFeature.getUniqueName());
        if (gsolFeature.getName() != null) {
            jsonFeature.put(FeatureStringEnum.NAME.value, gsolFeature.getName());
        }
        if (gsolFeature.symbol) {
            jsonFeature.put(FeatureStringEnum.SYMBOL.value, gsolFeature.symbol);
        }
        if (gsolFeature.description) {
            jsonFeature.put(FeatureStringEnum.DESCRIPTION.value, gsolFeature.description);
        }

        long start = System.currentTimeMillis();
        String finalOwnerString = generateOwnerString(gsolFeature)
        jsonFeature.put(FeatureStringEnum.OWNER.value.toLowerCase(), finalOwnerString);

        long durationInMilliseconds = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        if (gsolFeature.featureLocation) {
            Sequence sequence = gsolFeature.featureLocation.sequence
            jsonFeature.put(FeatureStringEnum.SEQUENCE.value, sequence.name);
        }

        durationInMilliseconds = System.currentTimeMillis() - start;


        start = System.currentTimeMillis();

        // TODO: move this to a configurable place or in another method to process afterwards
        //            List<String> errorList = new ArrayList<>()
        //            errorList.addAll(new Cds3Filter().filterFeature(gsolFeature))
        //            errorList.addAll(new StopCodonFilter().filterFeature(gsolFeature))
        //            JSONArray notesArray = new JSONArray()
        //            for (String error : errorList) {
        //                notesArray.put(error)
        //            }
        //            jsonFeature.put(FeatureStringEnum.NOTES.value, notesArray)
        //            durationInMilliseconds = System.currentTimeMillis()-start;
        //log.debug "notes ${durationInMilliseconds}"


        start = System.currentTimeMillis();
        // get children
        List<Feature> childFeatures = featureRelationshipService.getChildrenForFeatureAndTypes(gsolFeature)


        durationInMilliseconds = System.currentTimeMillis() - start;
        if (childFeatures) {
            JSONArray children = new JSONArray();
            jsonFeature.put(FeatureStringEnum.CHILDREN.value, children);
            for (Feature f : childFeatures) {
                Feature childFeature = f
                children.put(convertFeatureToJSON(childFeature, includeSequence));
            }
        }




        start = System.currentTimeMillis()
        // get parents
        List<Feature> parentFeatures = featureRelationshipService.getParentsForFeature(gsolFeature)

        durationInMilliseconds = System.currentTimeMillis() - start;
        //log.debug "parents ${durationInMilliseconds}"
        if (parentFeatures?.size() == 1) {
            Feature parent = parentFeatures.iterator().next();
            jsonFeature.put(FeatureStringEnum.PARENT_ID.value, parent.getUniqueName());
            jsonFeature.put(FeatureStringEnum.PARENT_NAME.value, parent.getName());
            jsonFeature.put(FeatureStringEnum.PARENT_TYPE.value, generateJSONFeatureStringForType(parent.ontologyId));
        }


        start = System.currentTimeMillis()

        Collection<FeatureLocation> featureLocations = gsolFeature.getFeatureLocations();
        if (featureLocations) {
            FeatureLocation gsolFeatureLocation = featureLocations.iterator().next();
            if (gsolFeatureLocation != null) {
                jsonFeature.put(FeatureStringEnum.LOCATION.value, convertFeatureLocationToJSON(gsolFeatureLocation));
            }
        }

        durationInMilliseconds = System.currentTimeMillis() - start;
        //log.debug "featloc ${durationInMilliseconds}"


        if (gsolFeature instanceof SequenceAlteration) {
            SequenceAlteration sequenceAlteration = (SequenceAlteration) gsolFeature
            if (sequenceAlteration.alterationResidue) {
                jsonFeature.put(FeatureStringEnum.RESIDUES.value, sequenceAlteration.alterationResidue);
            }
        } else if (includeSequence) {
            String residues = sequenceService.getResiduesFromFeature(gsolFeature)
            if (residues) {
                jsonFeature.put(FeatureStringEnum.RESIDUES.value, residues);
            }
        }

        //e.g. properties: [{value: "demo", type: {name: "owner", cv: {name: "feature_property"}}}]
        Collection<FeatureProperty> gsolFeatureProperties = gsolFeature.getFeatureProperties();

        JSONArray properties = new JSONArray();
        jsonFeature.put(FeatureStringEnum.PROPERTIES.value, properties);
        if (gsolFeatureProperties) {
            for (FeatureProperty property : gsolFeatureProperties) {
                JSONObject jsonProperty = new JSONObject();
                JSONObject jsonPropertyType = new JSONObject()
                if (property instanceof Comment) {
                    JSONObject jsonPropertyTypeCv = new JSONObject()
                    jsonPropertyTypeCv.put(FeatureStringEnum.NAME.value, FeatureStringEnum.FEATURE_PROPERTY.value)
                    jsonPropertyType.put(FeatureStringEnum.CV.value, jsonPropertyTypeCv)

                    jsonProperty.put(FeatureStringEnum.TYPE.value, jsonPropertyType);
                    jsonProperty.put(FeatureStringEnum.NAME.value, FeatureStringEnum.COMMENT.value);
                    jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                    properties.put(jsonProperty);
                    continue
                }
                if (property.tag == "justification") {
                    JSONObject jsonPropertyTypeCv = new JSONObject()
                    jsonPropertyTypeCv.put(FeatureStringEnum.NAME.value, FeatureStringEnum.FEATURE_PROPERTY.value)
                    jsonPropertyType.put(FeatureStringEnum.CV.value, jsonPropertyTypeCv)

                    jsonProperty.put(FeatureStringEnum.TYPE.value, jsonPropertyType);
                    jsonProperty.put(FeatureStringEnum.NAME.value, "justification");
                    jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                    properties.put(jsonProperty);
                    continue
                }
                jsonPropertyType.put(FeatureStringEnum.NAME.value, property.type)
                JSONObject jsonPropertyTypeCv = new JSONObject()
                jsonPropertyTypeCv.put(FeatureStringEnum.NAME.value, FeatureStringEnum.FEATURE_PROPERTY.value)
                jsonPropertyType.put(FeatureStringEnum.CV.value, jsonPropertyTypeCv)

                jsonProperty.put(FeatureStringEnum.TYPE.value, jsonPropertyType);
                jsonProperty.put(FeatureStringEnum.NAME.value, property.getTag());
                jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                properties.put(jsonProperty);
            }
        }
//        JSONObject ownerProperty = JSON.parse("{value: ${finalOwnerString}, type: {name: 'owner', cv: {name: 'feature_property'}}}") as JSONObject
//        properties.put(ownerProperty)


        Collection<DBXref> gsolFeatureDbxrefs = gsolFeature.getFeatureDBXrefs();
        if (gsolFeatureDbxrefs) {
            JSONArray dbxrefs = new JSONArray();
            jsonFeature.put(FeatureStringEnum.DBXREFS.value, dbxrefs);
            for (DBXref gsolDbxref : gsolFeatureDbxrefs) {
                JSONObject dbxref = new JSONObject();
                dbxref.put(FeatureStringEnum.ACCESSION.value, gsolDbxref.getAccession());
                dbxref.put(FeatureStringEnum.DB.value, new JSONObject().put(FeatureStringEnum.NAME.value, gsolDbxref.getDb().getName()));
                dbxrefs.put(dbxref);
            }
        }
        jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, gsolFeature.lastUpdated.time);
        jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, gsolFeature.dateCreated.time);
        return jsonFeature;
    }

    JSONObject generateJSONFeatureStringForType(String ontologyId) {
        if (ontologyId == null) return null;
        JSONObject jsonObject = new JSONObject();
        def feature = generateFeatureForType(ontologyId)
        String cvTerm = feature.hasProperty(FeatureStringEnum.ALTERNATECVTERM.value) ? feature.getProperty(FeatureStringEnum.ALTERNATECVTERM.value) : feature.cvTerm

        jsonObject.put(FeatureStringEnum.NAME.value, cvTerm)

        JSONObject cvObject = new JSONObject()
        cvObject.put(FeatureStringEnum.NAME.value, FeatureStringEnum.SEQUENCE.value)
        jsonObject.put(FeatureStringEnum.CV.value, cvObject)

        return jsonObject
    }

    @Timed
    JSONObject convertFeatureLocationToJSON(FeatureLocation gsolFeatureLocation) throws JSONException {
        JSONObject jsonFeatureLocation = new JSONObject();
        if (gsolFeatureLocation.id) {
            jsonFeatureLocation.put(FeatureStringEnum.ID.value, gsolFeatureLocation.id);
        }
        jsonFeatureLocation.put(FeatureStringEnum.FMIN.value, gsolFeatureLocation.getFmin());
        jsonFeatureLocation.put(FeatureStringEnum.FMAX.value, gsolFeatureLocation.getFmax());
        if (gsolFeatureLocation.isIsFminPartial()) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMIN_PARTIAL.value, true);
        }
        if (gsolFeatureLocation.isIsFmaxPartial()) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMAX_PARTIAL.value, true);
        }
        jsonFeatureLocation.put(FeatureStringEnum.STRAND.value, gsolFeatureLocation.getStrand());
        return jsonFeatureLocation;
    }

    @Transactional
    Boolean deleteFeature(Feature feature, HashMap<String, List<Feature>> modifiedFeaturesUniqueNames = new ArrayList<>()) {

        if (feature instanceof Exon) {
            Exon exon = (Exon) feature;
            Transcript transcript = (Transcript) Transcript.findByUniqueName(exonService.getTranscript(exon).getUniqueName());

            if (!(transcriptService.getGene(transcript) instanceof Pseudogene) && transcriptService.isProteinCoding(transcript)) {
                CDS cds = transcriptService.getCDS(transcript);
                if (cdsService.isManuallySetTranslationStart(cds)) {
                    int cdsStart = cds.getStrand() == -1 ? cds.getFmax() : cds.getFmin();
                    if (cdsStart >= exon.getFmin() && cdsStart <= exon.getFmax()) {
                        cdsService.setManuallySetTranslationStart(cds, false);
                    }
                }
            }

            exonService.deleteExon(transcript, exon)
            List<Feature> deletedFeatures = modifiedFeaturesUniqueNames.get(transcript.getUniqueName());
            if (deletedFeatures == null) {
                deletedFeatures = new ArrayList<Feature>();
                modifiedFeaturesUniqueNames.put(transcript.getUniqueName(), deletedFeatures);
            }
            deletedFeatures.add(exon);
            return transcriptService.getExons(transcript)?.size() > 0;
        } else {
            List<Feature> deletedFeatures = modifiedFeaturesUniqueNames.get(feature.getUniqueName());
            if (deletedFeatures == null) {
                deletedFeatures = new ArrayList<Feature>();
                modifiedFeaturesUniqueNames.put(feature.getUniqueName(), deletedFeatures);
            }
            deletedFeatures.add(feature);
            return false;
        }
    }


    @Transactional
    def flipStrand(Feature feature) {

        for (FeatureLocation featureLocation in feature.featureLocations) {
            featureLocation.strand = featureLocation.strand > 0 ? -1 : 1
            featureLocation.save()
        }

        for (Feature childFeature : feature?.parentFeatureRelationships?.childFeature) {
            flipStrand(childFeature);
        }

        feature.save()
        return feature

    }

    boolean typeHasChildren(Feature feature) {
        def f = Feature.get(feature.id)
        boolean hasChildren = !(f instanceof Exon) && !(f instanceof CDS) && !(f instanceof SpliceSite)
        return hasChildren
    }

    /**
     * If genes is empty, create a new gene.
     * Else, merge
     * @param genes
     */
    @Transactional
    private Gene mergeGenes(Set<Gene> genes) {
        // TODO: implement
        Gene newGene = null

        if (!genes) {
            log.error "No genes to merge, returning null"
        }

        for (Gene gene in genes) {
            if (!newGene) {
                newGene = gene
            } else {
                // merging code goes here

            }
        }


        return newGene
    }

    /**
     * Remove old gene / transcript from the transcript
     * Delete gene if no overlapping.
     * @param transcript
     * @param gene
     */
    @Transactional
    private void setGeneTranscript(Transcript transcript, Gene gene) {
        Gene oldGene = transcriptService.getGene(transcript)
        if (gene.uniqueName == oldGene.uniqueName) {
            log.info "Same gene do not need to set"
            return
        }

        transcriptService.deleteTranscript(oldGene, transcript)
        addTranscriptToGene(gene, transcript)

        // if this is empty then delete the gene
        if (!featureRelationshipService.getChildren(oldGene)) {
            deleteFeature(oldGene)
        }
    }

    /**
     * From https://github.com/GMOD/Apollo/issues/73
     * Need to add another call after other calculations are done to verify that we verify that we have not left our current isoform siblings or that we have just joined some and we should merge genes (always taking the one on the left).
     1 - using OrfOverlapper, find other isoforms
     2 - for each isoform, confirm that they belong to the same gene (if not, we merge genes)
     3 - confirm that no other non-overlapping isoforms have the same gene (if not, we create a new gene)
     * @param transcript
     */
    @Transactional
    def handleIsoformOverlap(Transcript transcript) {
        Gene originalGene = transcriptService.getGene(transcript)

        // TODO: should go left to right, may need to sort
        List<Transcript> originalTranscripts = transcriptService.getTranscripts(originalGene)?.sort() { a, b ->
            a.featureLocation.fmin <=> b.featureLocation.fmin
        }
        List<Transcript> newTranscripts = getOverlappingTranscripts(transcript.featureLocation)?.sort() { a, b ->
            a.featureLocation.fmin <=> b.featureLocation.fmin
        };

        List<Transcript> leftBehindTranscripts = originalTranscripts - newTranscripts

        Set<Gene> newGenesToMerge = new HashSet<>()
        for (Transcript newTranscript in newTranscripts) {
            newGenesToMerge.add(transcriptService.getGene(newTranscript))
        }
        Gene newGene = newGenesToMerge ? mergeGenes(newGenesToMerge) : new Gene(
                name: transcript.name
                , uniqueName: nameService.generateUniqueName()
        ).save(flush: true, insert: true)

        for (Transcript newTranscript in newTranscripts) {
            setGeneTranscript(newTranscript, newGene)
        }


        Set<Gene> usedGenes = new HashSet<>()
        while (leftBehindTranscripts.size() > 0) {
            Transcript originalOverlappingTranscript = leftBehindTranscripts.pop()
            Gene originalOverlappingGene = transcriptService.getGene(originalOverlappingTranscript)
            List<Transcript> overlappingTranscripts = getOverlappingTranscripts(originalOverlappingTranscript.featureLocation)
            overlappingTranscripts = overlappingTranscripts - usedGenes
            overlappingTranscripts.each { it ->
                setGeneTranscript(it, originalOverlappingGene)
            }
            leftBehindTranscripts = leftBehindTranscripts - overlappingTranscripts
        }
    }

    @Transactional
    def handleDynamicIsoformOverlap(Transcript transcript) {
        // Get all transcripts that overlap transcript and verify if they have the proper parent gene assigned
        List<Transcript> allOverlappingTranscripts = getTranscriptsWithOverlappingOrf(transcript)
        List<Transcript> allTranscriptsForCurrentGene = transcriptService.getTranscripts(transcriptService.getGene(transcript))
        List<Transcript> allTranscripts = (allOverlappingTranscripts + allTranscriptsForCurrentGene).unique()
        List<Transcript> allSortedTranscripts = allTranscripts?.sort() { a, b -> a.featureLocation.fmin <=> b.featureLocation.fmin }
        if (transcript.strand == Strand.POSITIVE.value) {
            allSortedTranscripts = allTranscripts?.sort() { a, b -> a.featureLocation.fmin <=> b.featureLocation.fmin }
        } else {
            allSortedTranscripts = allTranscripts?.sort() { a, b -> b.featureLocation.fmax <=> a.featureLocation.fmax }
        }
        // In a normal scenario, all sorted transcripts should have the same parent indicating no changes to be made.
        // If there are transcripts that do overlap but do not have the same parent gene then these transcripts should 
        // be merged to the 5' most transcript's gene.
        // If there are transcripts that do not overlap but have the same parent gene then these transcripts should be 
        // given a new, de-novo gene.
        log.debug "allSortedTranscripts:${allSortedTranscripts.name}"
        Transcript fivePrimeTranscript = allSortedTranscripts.get(0)
        Gene fivePrimeGene = transcriptService.getGene(fivePrimeTranscript)
        log.debug "5' Transcript: ${fivePrimeTranscript.name}"
        log.debug "5' Gene: ${fivePrimeGene.name}"
        allSortedTranscripts.remove(0)
        ArrayList<Transcript> transcriptsToAssociate = new ArrayList<Transcript>()
        ArrayList<Gene> genesToMerge = new ArrayList<Gene>()
        ArrayList<Transcript> transcriptsToDissociate = new ArrayList<Transcript>()
        ArrayList<Transcript> transcriptsToUpdate = new ArrayList<Transcript>()

        for (Transcript eachTranscript : allSortedTranscripts) {
            if (eachTranscript && fivePrimeGene && overlapperService.overlaps(eachTranscript, fivePrimeGene)) {
                if (transcriptService.getGene(eachTranscript).uniqueName != fivePrimeGene.uniqueName) {
                    transcriptsToAssociate.add(eachTranscript)
                    genesToMerge.add(transcriptService.getGene(eachTranscript))
                }
            } else {
                if (transcriptService.getGene(eachTranscript).uniqueName == fivePrimeGene.uniqueName) {
                    transcriptsToDissociate.add(eachTranscript)
                }
            }
        }

        log.debug "Transcripts to Associate: ${transcriptsToAssociate}"
        log.debug "Transcripts to Dissociate: ${transcriptsToDissociate}"
        transcriptsToUpdate.addAll(transcriptsToAssociate)
        transcriptsToUpdate.addAll(transcriptsToDissociate)

        if (transcriptsToAssociate.size() > 0) {
            Gene mergedGene = mergeGeneEntities(fivePrimeGene, genesToMerge.unique())
            for (Transcript eachTranscript in transcriptsToAssociate) {
                Gene eachTranscriptParent = transcriptService.getGene(eachTranscript)
                featureRelationshipService.removeFeatureRelationship(eachTranscriptParent, eachTranscript)
                addTranscriptToGene(mergedGene, eachTranscript)
                eachTranscript.name = nameService.generateUniqueName(eachTranscript, mergedGene.name)
                eachTranscript.save()
                if (eachTranscriptParent.parentFeatureRelationships.size() == 0) {
                    ArrayList<FeatureProperty> featureProperties = eachTranscriptParent.featureProperties
                    for (FeatureProperty fp : featureProperties) {
                        featurePropertyService.deleteProperty(eachTranscriptParent, fp)
                    }
                    //eachTranscriptParent.delete()
                    // replace a direct delete with the standard method
                    Feature topLevelFeature = featureService.getTopLevelFeature(eachTranscriptParent)
                    featureRelationshipService.deleteFeatureAndChildren(topLevelFeature)
                }
            }
        }

        if (transcriptsToDissociate.size() > 0) {
            Transcript firstTranscript = null
            for (Transcript eachTranscript in transcriptsToDissociate) {
                if (firstTranscript == null) {
                    firstTranscript = eachTranscript
                    Gene newGene = new Gene(
                            uniqueName: nameService.generateUniqueName(),
                            name: nameService.generateUniqueName(fivePrimeGene)
                    )

                    firstTranscript.owners.each {
                        newGene.addToOwners(it)
                    }
                    newGene.save(flush: true)

                    FeatureLocation newGeneFeatureLocation = new FeatureLocation(
                            feature: newGene,
                            fmin: firstTranscript.fmin,
                            fmax: firstTranscript.fmax,
                            strand: firstTranscript.strand,
                            sequence: firstTranscript.featureLocation.sequence,
                            residueInfo: firstTranscript.featureLocation.residueInfo,
                            locgroup: firstTranscript.featureLocation.locgroup,
                            rank: firstTranscript.featureLocation.rank
                    ).save(flush: true)
                    newGene.addToFeatureLocations(newGeneFeatureLocation)
                    featureRelationshipService.removeFeatureRelationship(transcriptService.getGene(firstTranscript), firstTranscript)
                    addTranscriptToGene(newGene, firstTranscript)
                    firstTranscript.name = nameService.generateUniqueName(firstTranscript, newGene.name)
                    firstTranscript.save(flush: true)
                    continue
                }
                if (eachTranscript && firstTranscript && overlapperService.overlaps(eachTranscript, firstTranscript)) {
                    featureRelationshipService.removeFeatureRelationship(transcriptService.getGene(eachTranscript), eachTranscript)
                    addTranscriptToGene(transcriptService.getGene(firstTranscript), eachTranscript)
                    firstTranscript.name = nameService.generateUniqueName(firstTranscript, transcriptService.getGene(firstTranscript).name)
                    firstTranscript.save(flush: true)
                } else {
                    throw new AnnotationException("Left behind transcript that doesn't overlap with any other transcripts")
                }
            }
        }
        return transcriptsToUpdate
    }

    def getTranscriptsWithOverlappingOrf(Transcript transcript) {
        ArrayList<Transcript> overlappingTranscripts = getOverlappingTranscripts(transcript.featureLocation)
        overlappingTranscripts.remove(transcript) // removing itself
        ArrayList<Transcript> transcriptsWithOverlappingOrf = new ArrayList<Transcript>()
        for (Transcript eachTranscript in overlappingTranscripts) {
            if (eachTranscript && transcript && overlapperService.overlaps(eachTranscript, transcript)) {
                transcriptsWithOverlappingOrf.add(eachTranscript)
            }
        }
        return transcriptsWithOverlappingOrf
    }

    @Transactional
    Gene mergeGeneEntities(Gene mainGene, ArrayList<Gene> genes) {
        def fminList = genes.featureLocation.fmin
        def fmaxList = genes.featureLocation.fmax
        fminList.add(mainGene.fmin)
        fmaxList.add(mainGene.fmax)

        FeatureLocation newFeatureLocation = mainGene.featureLocation
        newFeatureLocation.fmin = fminList.min()
        newFeatureLocation.fmax = fmaxList.max()
        newFeatureLocation.save(flush: true)
        for (Gene gene in genes) {
            gene.featureDBXrefs.each { mainGene.addToFeatureDBXrefs(it) }
            gene.featureGenotypes.each { mainGene.addToFeatureGenotypes(it) }
            gene.featurePhenotypes.each { mainGene.addToFeaturePhenotypes(it) }
            gene.featurePublications.each { mainGene.addToFeaturePublications(it) }
            gene.featureProperties.each { mainGene.addToFeatureProperties(it) }
            gene.featureSynonyms.each { mainGene.addToFeatureSynonyms(it) }
            gene.owners.each { mainGene.addToOwners(it) }
            gene.synonyms.each { mainGene.addToSynonyms(it) }
        }

        mainGene.save(flush: true)
        return mainGene
    }

    private class SequenceAlterationInContextPositionComparator<SequenceAlterationInContext> implements Comparator<SequenceAlterationInContext> {
        @Override
        int compare(SequenceAlterationInContext obj1, SequenceAlterationInContext obj2) {
            return obj1.fmin - obj2.fmin
        }
    }

    def sortSequenceAlterationInContext(List<SequenceAlterationInContext> sequenceAlterationInContextList) {
        Collections.sort(sequenceAlterationInContextList, new SequenceAlterationInContextPositionComparator<SequenceAlterationInContext>())
        return sequenceAlterationInContextList
    }

    def sequenceAlterationInContextOverlapper(Feature feature, SequenceAlterationInContext sequenceAlteration) {
        List<Exon> exonList = exonService.getSortedExons(feature, true)
        for (Exon exon : exonList) {
            int fmin = exon.fmin
            int fmax = exon.fmax
            if ((sequenceAlteration.fmin >= fmin && sequenceAlteration.fmin <= fmax) || (sequenceAlteration.fmin + sequenceAlteration.alterationResidue.length() >= fmin && sequenceAlteration.fmax + sequenceAlteration.alterationResidue.length() <= fmax)) {
                // alteration overlaps with exon
                return true
            }
        }
        return false
    }

    String getResiduesWithAlterations(Feature feature, Collection<SequenceAlteration> sequenceAlterations = new ArrayList<>()) {
        String residueString = null
        List<SequenceAlterationInContext> sequenceAlterationInContextList = new ArrayList<>()
        if (feature instanceof Transcript) {
            residueString = transcriptService.getResiduesFromTranscript((Transcript) feature)
            // sequence from exons, with UTRs too
            sequenceAlterationInContextList = getSequenceAlterationsInContext(feature, sequenceAlterations)
        } else if (feature instanceof CDS) {
            residueString = cdsService.getResiduesFromCDS((CDS) feature)
            // sequence from exons without UTRs
            sequenceAlterationInContextList = getSequenceAlterationsInContext(transcriptService.getTranscript(feature), sequenceAlterations)
        } else {
            // sequence from feature, as is
            residueString = sequenceService.getResiduesFromFeature(feature)
            sequenceAlterationInContextList = getSequenceAlterationsInContext(feature, sequenceAlterations)
        }
        if (sequenceAlterations.size() == 0 || sequenceAlterationInContextList.size() == 0) {
            return residueString
        }

        StringBuilder residues = new StringBuilder(residueString);
        List<SequenceAlterationInContext> orderedSequenceAlterationInContextList = new ArrayList<>(sequenceAlterationInContextList)
        Collections.sort(orderedSequenceAlterationInContextList, new SequenceAlterationInContextPositionComparator<SequenceAlterationInContext>());
        if (!feature.strand.equals(orderedSequenceAlterationInContextList.get(0).strand)) {
            Collections.reverse(orderedSequenceAlterationInContextList);
        }

        int currentOffset = 0
        for (SequenceAlterationInContext sequenceAlteration : orderedSequenceAlterationInContextList) {
            int localCoordinate
            if (feature instanceof Transcript) {
                localCoordinate = convertSourceCoordinateToLocalCoordinateForTranscript(feature, sequenceAlteration.fmin);

            } else if (feature instanceof CDS) {
                if (!((sequenceAlteration.fmin >= feature.fmin && sequenceAlteration.fmin <= feature.fmax) || (sequenceAlteration.fmax >= feature.fmin && sequenceAlteration.fmax <= feature.fmin))) {
                    // check to verify if alteration is part of the CDS
                    continue
                }
                localCoordinate = convertSourceCoordinateToLocalCoordinateForCDS(transcriptService.getTranscript(feature), sequenceAlteration.fmin)
            } else {
                localCoordinate = convertSourceCoordinateToLocalCoordinate(feature, sequenceAlteration.fmin);
            }

            String sequenceAlterationResidues = sequenceAlteration.alterationResidue
            if (feature.getFeatureLocation().getStrand() == -1) {
                sequenceAlterationResidues = SequenceTranslationHandler.reverseComplementSequence(sequenceAlterationResidues);
            }
            // Insertions
            if (sequenceAlteration.instanceOf == Insertion.canonicalName) {
                if (feature.getFeatureLocation().getStrand() == -1) {
                    ++localCoordinate;
                }
                residues.insert(localCoordinate + currentOffset, sequenceAlterationResidues);
                currentOffset += sequenceAlterationResidues.length();
            }
            // Deletions
            else if (sequenceAlteration.instanceOf == Deletion.canonicalName) {
                if (feature.getFeatureLocation().getStrand() == -1) {
                    residues.delete(localCoordinate + currentOffset - sequenceAlteration.alterationResidue.length() + 1,
                            localCoordinate + currentOffset + 1);
                } else {
                    residues.delete(localCoordinate + currentOffset,
                            localCoordinate + currentOffset + sequenceAlteration.alterationResidue.length());
                }
                currentOffset -= sequenceAlterationResidues.length();
            }
            // Substitions
            else if (sequenceAlteration.instanceOf == Substitution.canonicalName) {
                int start = feature.getStrand() == -1 ? localCoordinate - (sequenceAlteration.alterationResidue.length() - 1) : localCoordinate;
                residues.replace(start + currentOffset,
                        start + currentOffset + sequenceAlteration.alterationResidue.length(),
                        sequenceAlterationResidues);
            }
        }

        return residues.toString();
    }

    List<SequenceAlteration> getSequenceAlterationsForFeature(Feature feature) {
        int fmin = feature.fmin
        int fmax = feature.fmax
        Sequence sequence = feature.featureLocation.sequence
        sessionFactory.currentSession.flushMode = FlushMode.MANUAL

        List<SequenceAlteration> sequenceAlterations = SequenceAlteration.executeQuery("select distinct sa from SequenceAlteration sa join sa.featureLocations fl where fl.fmin >= :fmin and fl.fmin <= :fmax or fl.fmax >= :fmin and fl.fmax <= :fmax and fl.sequence = :seqId", [fmin: fmin, fmax: fmax, seqId: sequence])
        sessionFactory.currentSession.flushMode = FlushMode.AUTO

        return sequenceAlterations
    }


    List<SequenceAlterationInContext> getSequenceAlterationsInContext(Feature feature, Collection<SequenceAlteration> sequenceAlterations) {
        List<SequenceAlterationInContext> sequenceAlterationInContextList = new ArrayList<>()
        if (!(feature instanceof CDS) && !(feature instanceof Transcript)) {
            // for features that are not instance of CDS or Transcript (ex. Single exons)
            int featureFmin = feature.fmin
            int featureFmax = feature.fmax
            for (SequenceAlteration eachSequenceAlteration : sequenceAlterations) {
                int alterationFmin = eachSequenceAlteration.fmin
                int alterationFmax = eachSequenceAlteration.fmax
                SequenceAlterationInContext sa = new SequenceAlterationInContext()
                if ((alterationFmin >= featureFmin && alterationFmax <= featureFmax) && (alterationFmax >= featureFmin && alterationFmax <= featureFmax)) {
                    // alteration is within the generic feature
                    sa.fmin = alterationFmin
                    sa.fmax = alterationFmax
                    if (eachSequenceAlteration instanceof Insertion) {
                        sa.instanceOf = Insertion.canonicalName
                    } else if (eachSequenceAlteration instanceof Deletion) {
                        sa.instanceOf = Deletion.canonicalName
                    } else if (eachSequenceAlteration instanceof Substitution) {
                        sa.instanceOf = Substitution.canonicalName
                    }
                    sa.type = 'within'
                    sa.strand = eachSequenceAlteration.strand
                    sa.name = eachSequenceAlteration.name + '-inContext'
                    sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                    sa.offset = eachSequenceAlteration.offset
                    sa.alterationResidue = eachSequenceAlteration.alterationResidue
                    sequenceAlterationInContextList.add(sa)
                } else if ((alterationFmin >= featureFmin && alterationFmin <= featureFmax) && (alterationFmax >= featureFmin && alterationFmax >= featureFmax)) {
                    // alteration starts in exon but ends in an intron
                    int difference = alterationFmax - featureFmax
                    sa.fmin = alterationFmin
                    sa.fmax = Math.min(featureFmax, alterationFmax)
                    if (eachSequenceAlteration instanceof Insertion) {
                        sa.instanceOf = Insertion.canonicalName
                    } else if (eachSequenceAlteration instanceof Deletion) {
                        sa.instanceOf = Deletion.canonicalName
                    } else if (eachSequenceAlteration instanceof Substitution) {
                        sa.instanceOf = Substitution.canonicalName
                    }
                    sa.type = 'exon-to-intron'
                    sa.strand = eachSequenceAlteration.strand
                    sa.name = eachSequenceAlteration.name + '-inContext'
                    sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                    sa.offset = eachSequenceAlteration.offset - difference
                    sa.alterationResidue = eachSequenceAlteration.alterationResidue.substring(0, eachSequenceAlteration.alterationResidue.length() - difference)
                    sequenceAlterationInContextList.add(sa)
                } else if ((alterationFmin <= featureFmin && alterationFmin <= featureFmax) && (alterationFmax >= featureFmin && alterationFmax <= featureFmax)) {
                    // alteration starts within intron but ends in an exon
                    int difference = featureFmin - alterationFmin
                    sa.fmin = Math.max(featureFmin, alterationFmin)
                    sa.fmax = alterationFmax
                    if (eachSequenceAlteration instanceof Insertion) {
                        sa.instanceOf = Insertion.canonicalName
                    } else if (eachSequenceAlteration instanceof Deletion) {
                        sa.instanceOf = Deletion.canonicalName
                    } else if (eachSequenceAlteration instanceof Substitution) {
                        sa.instanceOf = Substitution.canonicalName
                    }
                    sa.type = 'intron-to-exon'
                    sa.strand = eachSequenceAlteration.strand
                    sa.name = eachSequenceAlteration.name + '-inContext'
                    sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                    sa.offset = eachSequenceAlteration.offset - difference
                    sa.alterationResidue = eachSequenceAlteration.alterationResidue.substring(difference, eachSequenceAlteration.alterationResidue.length())
                    sequenceAlterationInContextList.add(sa)
                }
            }
        } else {
            List<Exon> exonList = feature instanceof CDS ? exonService.getSortedExons(transcriptService.getTranscript(feature)) : exonService.getSortedExons(feature, true)
            for (Exon exon : exonList) {
                int exonFmin = exon.fmin
                int exonFmax = exon.fmax

                for (SequenceAlteration eachSequenceAlteration : sequenceAlterations) {
                    int alterationFmin = eachSequenceAlteration.fmin
                    int alterationFmax = eachSequenceAlteration.fmax
                    SequenceAlterationInContext sa = new SequenceAlterationInContext()
                    if ((alterationFmin >= exonFmin && alterationFmin <= exonFmax) && (alterationFmax >= exonFmin && alterationFmax <= exonFmax)) {
                        // alteration is within exon
                        sa.fmin = alterationFmin
                        sa.fmax = alterationFmax
                        if (eachSequenceAlteration instanceof Insertion) {
                            sa.instanceOf = Insertion.canonicalName
                        } else if (eachSequenceAlteration instanceof Deletion) {
                            sa.instanceOf = Deletion.canonicalName
                        } else if (eachSequenceAlteration instanceof Substitution) {
                            sa.instanceOf = Substitution.canonicalName
                        }
                        sa.type = 'within'
                        sa.strand = eachSequenceAlteration.strand
                        sa.name = eachSequenceAlteration.name + '-inContext'
                        sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                        sa.offset = eachSequenceAlteration.offset
                        sa.alterationResidue = eachSequenceAlteration.alterationResidue
                        sequenceAlterationInContextList.add(sa)
                    } else if ((alterationFmin >= exonFmin && alterationFmin <= exonFmax) && (alterationFmax >= exonFmin && alterationFmax >= exonFmax)) {
                        // alteration starts in exon but ends in an intron
                        int difference = alterationFmax - exonFmax
                        sa.fmin = alterationFmin
                        sa.fmax = Math.min(exonFmax, alterationFmax)
                        if (eachSequenceAlteration instanceof Insertion) {
                            sa.instanceOf = Insertion.canonicalName
                        } else if (eachSequenceAlteration instanceof Deletion) {
                            sa.instanceOf = Deletion.canonicalName
                        } else if (eachSequenceAlteration instanceof Substitution) {
                            sa.instanceOf = Substitution.canonicalName
                        }
                        sa.type = 'exon-to-intron'
                        sa.strand = eachSequenceAlteration.strand
                        sa.name = eachSequenceAlteration.name + '-inContext'
                        sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                        sa.offset = eachSequenceAlteration.offset - difference
                        sa.alterationResidue = eachSequenceAlteration.alterationResidue.substring(0, eachSequenceAlteration.alterationResidue.length() - difference)
                        sequenceAlterationInContextList.add(sa)
                    } else if ((alterationFmin <= exonFmin && alterationFmin <= exonFmax) && (alterationFmax >= exonFmin && alterationFmax <= exonFmax)) {
                        // alteration starts within intron but ends in an exon
                        int difference = exonFmin - alterationFmin
                        sa.fmin = Math.max(exonFmin, alterationFmin)
                        sa.fmax = alterationFmax
                        if (eachSequenceAlteration instanceof Insertion) {
                            sa.instanceOf = Insertion.canonicalName
                        } else if (eachSequenceAlteration instanceof Deletion) {
                            sa.instanceOf = Deletion.canonicalName
                        } else if (eachSequenceAlteration instanceof Substitution) {
                            sa.instanceOf = Substitution.canonicalName
                        }
                        sa.type = 'intron-to-exon'
                        sa.strand = eachSequenceAlteration.strand
                        sa.name = eachSequenceAlteration.name + '-inContext'
                        sa.originalAlterationUniqueName = eachSequenceAlteration.uniqueName
                        sa.offset = eachSequenceAlteration.offset - difference
                        sa.alterationResidue = eachSequenceAlteration.alterationResidue.substring(difference, eachSequenceAlteration.alterationResidue.length())
                        sequenceAlterationInContextList.add(sa)
                    }
                }
            }
        }
        return sequenceAlterationInContextList
    }

    int convertModifiedLocalCoordinateToSourceCoordinate(Feature feature, int localCoordinate) {
        Transcript transcript = (Transcript) featureRelationshipService.getParentForFeature(feature, Transcript.ontologyId)
        List<SequenceAlterationInContext> alterations = new ArrayList<>()
        if (feature instanceof CDS) {
            List<SequenceAlteration> frameshiftsAsAlterations = getFrameshiftsAsAlterations(transcript)
            if (frameshiftsAsAlterations.size() > 0) {
                for (SequenceAlteration frameshifts : frameshiftsAsAlterations) {
                    SequenceAlterationInContext sa = new SequenceAlterationInContext()
                    sa.fmin = frameshifts.fmin
                    sa.fmax = frameshifts.fmax
                    sa.alterationResidue = frameshifts.alterationResidue
                    sa.type = 'frameshifts-as-alteration'
                    sa.instanceOf = Frameshift.canonicalName
                    sa.originalAlterationUniqueName = frameshifts.uniqueName
                    sa.name = frameshifts.uniqueName + '-frameshifts-inContext'
                    alterations.add(sa)
                }
            }
        }

        alterations.addAll(getSequenceAlterationsInContext(feature, getAllSequenceAlterationsForFeature(feature)))
        if (alterations.size() == 0) {
            if (feature instanceof CDS) {
                // if feature is CDS then calling convertLocalCoordinateToSourceCoordinateForCDS
                return convertLocalCoordinateToSourceCoordinateForCDS((CDS) feature, localCoordinate);
            } else if (feature instanceof Transcript) {
                // if feature is Transcript then calling convertLocalCoordinateToSourceCoordinateForTranscript
                return convertLocalCoordinateToSourceCoordinateForTranscript((Transcript) feature, localCoordinate);
            } else {
                // calling convertLocalCoordinateToSourceCoordinate
                return convertLocalCoordinateToSourceCoordinate(feature, localCoordinate);
            }
        }

        Collections.sort(alterations, new SequenceAlterationInContextPositionComparator<SequenceAlterationInContext>());
        if (feature.getFeatureLocation().getStrand() == -1) {
            Collections.reverse(alterations);
        }

        int insertionOffset = 0
        int deletionOffset = 0
        for (SequenceAlterationInContext alteration : alterations) {
            int alterationResidueLength = alteration.alterationResidue.length()
            if (!sequenceAlterationInContextOverlapper(feature, alteration)) {
                // sequenceAlterationInContextOverlapper method verifies if the alteration is within any of the given exons of the transcript
                continue;
            }
            int coordinateInContext = -1
            if (feature instanceof CDS) {
                // if feature is CDS then calling convertSourceCoordinateToLocalCoordinateForCDS
                coordinateInContext = convertSourceCoordinateToLocalCoordinateForCDS(feature, alteration.fmin)
            } else if (feature instanceof Transcript) {
                // if feature is Transcript then calling convertSourceCoordinateToLocalCoordinateForTranscript
                coordinateInContext = convertSourceCoordinateToLocalCoordinateForTranscript(feature, alteration.fmin)
            } else {
                // calling convertSourceCoordinateToLocalCoordinate
                coordinateInContext = convertSourceCoordinateToLocalCoordinate(feature, alteration.fmin)
            }

            if (feature.strand == Strand.NEGATIVE.value) {
                if (coordinateInContext <= localCoordinate && alteration.instanceOf == Deletion.canonicalName) {
                    deletionOffset += alterationResidueLength
                }
                if ((coordinateInContext - alterationResidueLength) - 1 <= localCoordinate && alteration.instanceOf == Insertion.canonicalName) {
                    insertionOffset += alterationResidueLength
                }
                if ((localCoordinate - coordinateInContext) - 1 < alterationResidueLength && (localCoordinate - coordinateInContext) >= 0 && alteration.instanceOf == Insertion.canonicalName) {
                    insertionOffset -= (alterationResidueLength - (localCoordinate - coordinateInContext - 1))

                }

            } else {
                if (coordinateInContext < localCoordinate && alteration.instanceOf == Deletion.canonicalName) {
                    deletionOffset += alterationResidueLength
                }
                if ((coordinateInContext + alterationResidueLength) <= localCoordinate && alteration.instanceOf == Insertion.canonicalName) {
                    insertionOffset += alterationResidueLength
                }
                if ((localCoordinate - coordinateInContext) < alterationResidueLength && (localCoordinate - coordinateInContext) >= 0 && alteration.instanceOf == Insertion.canonicalName) {
                    insertionOffset += localCoordinate - coordinateInContext
                }
            }
        }
        localCoordinate = localCoordinate - insertionOffset
        localCoordinate = localCoordinate + deletionOffset

        if (feature instanceof CDS) {
            // if feature is CDS then calling convertLocalCoordinateToSourceCoordinateForCDS
            return convertLocalCoordinateToSourceCoordinateForCDS((CDS) feature, localCoordinate)
        } else if (feature instanceof Transcript) {
            // if feature is Transcript then calling convertLocalCoordinateToSourceCoordinateForTranscript
            return convertLocalCoordinateToSourceCoordinateForTranscript((Transcript) feature, localCoordinate)
        } else {
            // calling convertLocalCoordinateToSourceCoordinate for all other feature types
            return convertLocalCoordinateToSourceCoordinate(feature, localCoordinate)
        }
    }

    /* convert an input local coordinate to a local coordinate that incorporates sequence alterations */

    int convertSourceToModifiedLocalCoordinate(Feature feature, Integer localCoordinate, List<SequenceAlteration> alterations = new ArrayList<>()) {
        log.debug "convertSourceToModifiedLocalCoordinate"

        if (alterations.size() == 0) {
            log.debug "No alterations returning ${localCoordinate}"
            return localCoordinate
        }


        Collections.sort(alterations, new FeaturePositionComparator<SequenceAlteration>());
        if (feature.getFeatureLocation().getStrand() == -1) {
            Collections.reverse(alterations);
        }

        int deletionOffset = 0
        int insertionOffset = 0

        for (SequenceAlteration alteration : alterations) {
            int alterationResidueLength = alteration.alterationResidue.length()
            int coordinateInContext = convertSourceCoordinateToLocalCoordinate(feature, alteration.fmin);

            //getAllSequenceAlterationsForFeature returns alterations over entire scaffold?!
            if (alteration.fmin <= feature.fmin || alteration.fmax > feature.fmax) {
                continue
            }

            if (feature.strand == Strand.NEGATIVE.value) {
                coordinateInContext = feature.featureLocation.calculateLength() - coordinateInContext
                log.debug "Checking negative insertion ${coordinateInContext} ${localCoordinate} ${(coordinateInContext - alterationResidueLength) - 1}"
                if (coordinateInContext <= localCoordinate && alteration instanceof Deletion) {
                    log.debug "Processing negative deletion"
                    deletionOffset += alterationResidueLength
                }
                if ((coordinateInContext - alterationResidueLength) - 1 <= localCoordinate && alteration instanceof Insertion) {
                    log.debug "Processing negative insertion ${coordinateInContext} ${localCoordinate} ${(coordinateInContext - alterationResidueLength) - 1}"
                    insertionOffset += alterationResidueLength
                }
                if ((localCoordinate - coordinateInContext) - 1 < alterationResidueLength && (localCoordinate - coordinateInContext) >= 0 && alteration instanceof Insertion) {
                    log.debug "Processing negative insertion pt 2"
                    insertionOffset -= (alterationResidueLength - (localCoordinate - coordinateInContext - 1))

                }

            } else {
                if (coordinateInContext < localCoordinate && alteration instanceof Deletion) {
                    log.debug "Processing positive deletion"
                    deletionOffset += alterationResidueLength
                }
                if ((coordinateInContext + alterationResidueLength) <= localCoordinate && alteration instanceof Insertion) {
                    log.debug "Processing positive insertion"
                    insertionOffset += alterationResidueLength
                }
                if ((localCoordinate - coordinateInContext) < alterationResidueLength && (localCoordinate - coordinateInContext) >= 0 && alteration instanceof Insertion) {
                    log.debug "Processing positive insertion pt 2"
                    insertionOffset += localCoordinate - coordinateInContext
                }
            }

        }

        log.debug "Returning ${localCoordinate - deletionOffset + insertionOffset}"
        return localCoordinate - deletionOffset + insertionOffset

    }


    def changeAnnotationType(JSONObject inputObject, Feature feature, Sequence sequence, User user, String type) {
        String uniqueName = feature.uniqueName
        String originalType = feature.alternateCvTerm ? feature.alternateCvTerm : feature.cvTerm
        JSONObject currentFeatureJsonObject = convertFeatureToJSON(feature)
        Feature newFeature = null

        String topLevelFeatureType = null
        if (type == Transcript.alternateCvTerm) {
            topLevelFeatureType = Pseudogene.alternateCvTerm
        } else if (singletonFeatureTypes.contains(type)) {
            topLevelFeatureType = type
        } else {
            topLevelFeatureType = Gene.alternateCvTerm
        }

        Gene parentGene = null
        String parentGeneSymbol = null
        String parentGeneDescription = null
        Set<DBXref> parentGeneDbxrefs = null
        Set<FeatureProperty> parentGeneFeatureProperties = null
        List<Transcript> transcriptList = []

        if (feature instanceof Transcript) {
            parentGene = transcriptService.getGene((Transcript) feature)
            parentGeneSymbol = parentGene.symbol
            parentGeneDescription = parentGene.description
            parentGeneDbxrefs = parentGene.featureDBXrefs
            parentGeneFeatureProperties = parentGene.featureProperties
            transcriptList = transcriptService.getTranscripts(parentGene)
        }

        log.debug "Parent gene Dbxrefs: ${parentGeneDbxrefs}"
        log.debug "Parent gene Feature Properties: ${parentGeneFeatureProperties}"

        if (currentFeatureJsonObject.has(FeatureStringEnum.PARENT_TYPE.value)) {
            currentFeatureJsonObject.get(FeatureStringEnum.PARENT_TYPE.value).name = topLevelFeatureType
        }
        currentFeatureJsonObject.get(FeatureStringEnum.TYPE.value).name = type
        currentFeatureJsonObject.put(FeatureStringEnum.USERNAME.value, currentFeatureJsonObject.get(FeatureStringEnum.OWNER.value.toLowerCase()))
        currentFeatureJsonObject.remove(FeatureStringEnum.PARENT_ID.value)
        currentFeatureJsonObject.remove(FeatureStringEnum.ID.value)
        currentFeatureJsonObject.remove(FeatureStringEnum.OWNER.value.toLowerCase())
        currentFeatureJsonObject.remove(FeatureStringEnum.DATE_CREATION.value)
        currentFeatureJsonObject.remove(FeatureStringEnum.DATE_LAST_MODIFIED.value)
        if (currentFeatureJsonObject.has(FeatureStringEnum.CHILDREN.value)) {
            for (JSONObject childFeature : currentFeatureJsonObject.get(FeatureStringEnum.CHILDREN.value)) {
                childFeature.remove(FeatureStringEnum.ID.value)
                childFeature.remove(FeatureStringEnum.OWNER.value.toLowerCase())
                childFeature.remove(FeatureStringEnum.DATE_CREATION.value)
                childFeature.remove(FeatureStringEnum.DATE_LAST_MODIFIED.value)
                childFeature.get(FeatureStringEnum.PARENT_TYPE.value).name = type
            }
        }


        if (!singletonFeatureTypes.contains(originalType) && rnaFeatureTypes.contains(type)) {
            // *RNA to *RNA
            if (transcriptList.size() == 1) {
                featureRelationshipService.deleteFeatureAndChildren(parentGene)
            } else {
                featureRelationshipService.removeFeatureRelationship(parentGene, feature)
                featureRelationshipService.deleteFeatureAndChildren(feature)
            }

            log.debug "Converting ${originalType} to ${type}"
            Transcript transcript = null
            if (type == MRNA.alternateCvTerm) {
                // *RNA to mRNA
                transcript = generateTranscript(currentFeatureJsonObject, sequence, true)
                setLongestORF(transcript)
            } else {
                // *RNA to *RNA
                transcript = addFeature(currentFeatureJsonObject, sequence, user, true)
                setLongestORF(transcript)
            }

            Gene newGene = transcriptService.getGene(transcript)
            newGene.symbol = parentGeneSymbol
            newGene.description = parentGeneDescription

            parentGeneDbxrefs.each { it ->
                DBXref dbxref = new DBXref(
                        db: it.db,
                        accession: it.accession,
                        version: it.version,
                        description: it.description
                ).save()
                newGene.addToFeatureDBXrefs(dbxref)
            }

            parentGeneFeatureProperties.each { it ->
                if (it instanceof Comment) {
                    featurePropertyService.addComment(newGene, it.value)
                } else {
                    FeatureProperty fp = new FeatureProperty(
                            type: it.type,
                            value: it.value,
                            rank: it.rank,
                            tag: it.tag,
                            feature: newGene
                    ).save()
                    newGene.addToFeatureProperties(fp)
                }
            }
            newGene.save(flush: true)
            newFeature = transcript
        } else if (!singletonFeatureTypes.contains(originalType) && singletonFeatureTypes.contains(type)) {
            // *RNA to singleton
            if (transcriptList.size() == 1) {
                featureRelationshipService.deleteFeatureAndChildren(parentGene)
            } else {
                featureRelationshipService.removeFeatureRelationship(parentGene, feature)
                featureRelationshipService.deleteFeatureAndChildren(feature)
            }
            currentFeatureJsonObject.put(FeatureStringEnum.UNIQUENAME.value, uniqueName)
            currentFeatureJsonObject.remove(FeatureStringEnum.CHILDREN.value)
            currentFeatureJsonObject.remove(FeatureStringEnum.PARENT_TYPE.value)
            currentFeatureJsonObject.remove(FeatureStringEnum.PARENT_ID.value)
            currentFeatureJsonObject.get(FeatureStringEnum.LOCATION.value).strand = 0
            Feature singleton = addFeature(currentFeatureJsonObject, sequence, user, true)
            newFeature = singleton
        } else if (singletonFeatureTypes.contains(originalType) && singletonFeatureTypes.contains(type)) {
            // singleton to singleton
            currentFeatureJsonObject.put(FeatureStringEnum.UNIQUENAME.value, uniqueName)
            featureRelationshipService.deleteFeatureAndChildren(feature)
            Feature singleton = addFeature(currentFeatureJsonObject, sequence, user, true)
            newFeature = singleton
        } else {
            log.error "Not enough information available to change ${uniqueName} from ${originalType} -> ${type}."
        }

        // TODO: synonyms, featureSynonyms, featureGenotypes, featurePhenotypes

        return newFeature
    }

    def addFeature(JSONObject jsonFeature, Sequence sequence, User user, boolean suppressHistory) {
        Feature returnFeature = null

        if (rnaFeatureTypes.contains(jsonFeature.get(FeatureStringEnum.TYPE.value).name)) {
            Gene gene = jsonFeature.has(FeatureStringEnum.PARENT_ID.value) ? (Gene) Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.PARENT_ID.value)) : null
            Transcript transcript = null

            if (gene) {
                // Scenario I - if 'parent_id' attribute is given then find the gene
                transcript = (Transcript) convertJSONToFeature(jsonFeature, sequence)
                if (transcript.fmin < 0 || transcript.fmax < 0) {
                    throw new AnnotationException("Feature cannot have negative coordinates")
                }

                setOwner(transcript, user)

                addTranscriptToGene(gene, transcript)
                if (!suppressHistory) {
                    String name = nameService.generateUniqueName(transcript)
                    transcript.name = name
                }
            } else {
                // Scenario II - find and overlapping isoform and if present, add current transcript to its gene.
                // Disabling Scenario II since there is no appropriate overlapper to determine overlaps between non-coding transcripts.
            }

            if (gene == null) {
                log.debug "gene is still NULL"
                // Scenario III - create a de-novo gene
                JSONObject jsonGene = new JSONObject()
                if (jsonFeature.has(FeatureStringEnum.PARENT.value)) {
                    // Scenario IIIa - use the 'parent' attribute, if provided, from feature JSON
                    jsonGene = JSON.parse(jsonFeature.getString(FeatureStringEnum.PARENT.value)) as JSONObject
                    jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonFeature))
                }
                else {
                    // Scenario IIIb - use the current mRNA's featurelocation for gene
                    jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonFeature))
                    jsonGene.put(FeatureStringEnum.LOCATION.value, jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value))
                    String cvTermString = jsonFeature.get(FeatureStringEnum.TYPE.value).name == Transcript.alternateCvTerm ? Pseudogene.alternateCvTerm : Gene.alternateCvTerm
                    jsonGene.put(FeatureStringEnum.TYPE.value, convertCVTermToJSON(FeatureStringEnum.CV.value, cvTermString))
                }

                String geneName = null
                if (jsonGene.has(FeatureStringEnum.NAME.value)) {
                    geneName = jsonGene.getString(FeatureStringEnum.NAME.value)
                    log.debug "jsonGene already has 'name': ${geneName}"
                }
                else if (jsonFeature.has(FeatureStringEnum.PARENT_NAME.value)) {
                    String principalName = jsonFeature.getString(FeatureStringEnum.PARENT_NAME.value)
                    geneName = nameService.makeUniqueGeneName(sequence.organism, principalName, false)
                    log.debug "jsonFeature has 'parent_name' attribute; using ${principalName} to generate ${geneName}"
                }
                else
                if (jsonFeature.has(FeatureStringEnum.NAME.value)) {
                    geneName = jsonFeature.getString(FeatureStringEnum.NAME.value)
                    log.debug "jsonGene already has 'name': ${geneName}"
                }
                else {
                    geneName = nameService.makeUniqueGeneName(sequence.organism, sequence.name, false)
                    log.debug "Making a new unique gene name: ${geneName}"
                }

                if (!suppressHistory) {
                    geneName = nameService.makeUniqueGeneName(sequence.organism, geneName, true)
                }

                // set back to the original gene name
                if (jsonFeature.has(FeatureStringEnum.GENE_NAME.value)) {
                    geneName = jsonFeature.getString(FeatureStringEnum.GENE_NAME.value)
                }
                jsonGene.put(FeatureStringEnum.NAME.value, geneName)

                gene = (Gene) convertJSONToFeature(jsonGene, sequence)
                updateNewGsolFeatureAttributes(gene, sequence)

                if (gene.fmin < 0 || gene.fmax < 0) {
                    throw new AnnotationException("Feature cannot have negative coordinates")
                }

                transcript = transcriptService.getTranscripts(gene).first()
                removeExonOverlapsAndAdjacenciesForFeature(gene)
                if (!suppressHistory) {
                    String name = nameService.generateUniqueName(transcript, geneName)
                    transcript.name = name
                }

                gene.save(insert: true)
                transcript.save(flush: true)

                setOwner(gene, user);
                setOwner(transcript, user);
            }

            removeExonOverlapsAndAdjacencies(transcript)
            CDS cds = transcriptService.getCDS(transcript)
            if (cds != null) {
                featureRelationshipService.deleteChildrenForTypes(transcript, CDS.ontologyId)
                cds.delete()
            }
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
            returnFeature = transcript
        } else {
            if (!jsonFeature.containsKey(FeatureStringEnum.NAME.value) && jsonFeature.containsKey(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childArray = jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                if (childArray?.size() == 1 && childArray.getJSONObject(0).containsKey(FeatureStringEnum.NAME.value)) {
                    jsonFeature.put(FeatureStringEnum.NAME.value, childArray.getJSONObject(0).getString(FeatureStringEnum.NAME.value))
                }
            }
            Feature feature = convertJSONToFeature(jsonFeature, sequence)
            if (!suppressHistory) {
                String name = nameService.generateUniqueName(feature, feature.name)
                feature.name = name
            }
            updateNewGsolFeatureAttributes(feature, sequence)

            setOwner(feature, user);
            feature.save(insert: true, flush: true)
            if (jsonFeature.get(FeatureStringEnum.TYPE.value).name == Gene.alternateCvTerm ||
                    jsonFeature.get(FeatureStringEnum.TYPE.value).name == Pseudogene.alternateCvTerm) {
                Transcript transcript = transcriptService.getTranscripts(feature).iterator().next()
                setOwner(transcript, user);
                removeExonOverlapsAndAdjacencies(transcript)
                CDS cds = transcriptService.getCDS(transcript)
                if (cds != null) {
                    featureRelationshipService.deleteChildrenForTypes(transcript, CDS.ontologyId)
                    cds.delete()
                }
                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript)
                transcript.save(flush: true)
                returnFeature = transcript
            } else {
                returnFeature = feature
            }
        }

        return returnFeature
    }

    def addNonPrimaryDbxrefs(Feature feature, String dbString, String accessionString) {
        DB db = DB.findByName(dbString)
        if (!db) {
            db = new DB(name: dbString).save()
        }
        DBXref dbxref = DBXref.findOrSaveByAccessionAndDb(accessionString, db)
        dbxref.save(flush: true)
        feature.addToFeatureDBXrefs(dbxref)
        feature.save()
    }

    def addNonReservedProperties(Feature feature, String tagString, String valueString) {
        FeatureProperty featureProperty = new FeatureProperty(
                feature: feature,
                value: valueString,
                tag: tagString
        ).save()
        featurePropertyService.addProperty(feature, featureProperty)
        feature.save()
    }
}
