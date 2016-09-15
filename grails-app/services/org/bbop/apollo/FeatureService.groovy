package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.alteration.SequenceAlterationInContext
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
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
    def assemblageService
    def projectionService
    def preferenceService
    def sessionFactory

    public static final
    def rnaFeatureTypes = [MRNA.alternateCvTerm, MiRNA.alternateCvTerm, NcRNA.alternateCvTerm, RRNA.alternateCvTerm, SnRNA.alternateCvTerm, SnoRNA.alternateCvTerm, TRNA.alternateCvTerm, Transcript.alternateCvTerm]
    public static final def singletonFeatureTypes = [RepeatRegion.alternateCvTerm, TransposableElement.alternateCvTerm]

    /**
     * If a json feature location extends across two scaffolds, we need a feature locaiton for each
     * @param jsonLocation
     * @param assemblage
     * @param projected
     * @param defaultStrand
     * @return
     * @throws JSONException
     */
    @Timed
    @Transactional
    public List<FeatureLocation> convertJSONToFeatureLocations(JSONObject jsonLocation, Assemblage assemblage, int defaultStrand = Strand.POSITIVE.value) throws JSONException {
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)

        Integer min = jsonLocation.getInt(FeatureStringEnum.FMIN.value)
        Integer max = jsonLocation.getInt(FeatureStringEnum.FMAX.value)
        Organism organism = assemblage.organism

        List<String> sequenceListString = []

        // TODO: I think that passing the SEQUENCE value in addition to the projection is asking for trouble.  fmin / fmax should just apply to the projection
        if (jsonLocation.containsKey(FeatureStringEnum.SEQUENCE.value)) {
            String sequenceString = jsonLocation.getString(FeatureStringEnum.SEQUENCE.value)
            if (sequenceString.startsWith("{")) {
                sequenceListString.add((JSON.parse(sequenceString) as JSONObject).name)
            } else if (sequenceString.startsWith("[")) {
                sequenceListString.addAll((JSON.parse(sequenceString) as JSONArray).name)
            } else {
                sequenceListString.add(sequenceString)
            }
        } else {

            // TODO: this should be the default / only method if possible
            List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.getReverseProjectionSequences(min, max)

            for (ProjectionSequence projectionSequence in projectionSequenceList) {
                sequenceListString.add(projectionSequence.name)
            }
        }

        List<FeatureLocation> featureLocationList = new ArrayList<>()

        Map<String, Integer> orderedSequenceMap = multiSequenceProjection.getOrderedSequenceMap()
        List<Sequence> sequenceList = Sequence.findAllByNameInListAndOrganism(sequenceListString, organism).sort(){ a,b ->
               orderedSequenceMap.get(a.name) <=> orderedSequenceMap.get(b.name)
        }
        MultiSequenceProjection fullProjection = projectionService.createMultiSequenceProjection(assemblageService.generateAssemblageForSequence(sequenceList))
        int rank = 0
        sequenceListString.each{ String sequenceNameEntry ->
            ProjectionSequence projectionSequence = multiSequenceProjection.getProjectionSequence(sequenceNameEntry,assemblage.organism)
            FeatureLocation featureLocation = convertJSONToFeatureLocation(jsonLocation, fullProjection, projectionSequence, defaultStrand)
            if(featureLocation){
                featureLocation.rank = rank
                featureLocationList.add(featureLocation)
                ++rank
            }
        }

        return featureLocationList

    }


    /**
     *
     * @param jsonLocation We assume that things are brought in projected
     * @param multiSequenceProjection
     * @param order
     * @param defaultStrand
     * @return
     * @throws JSONException
     *
     * In this method we have to assert that each "order" correlates with a single sequence
     * If the fmin / fmax DO NOT correspond, then it has to be dropped.
     */
    @Timed
    @Transactional
    public FeatureLocation convertJSONToFeatureLocation(JSONObject jsonLocation, MultiSequenceProjection multiSequenceProjection, ProjectionSequence projectionSequence, int defaultStrand = Strand.POSITIVE.value) throws JSONException {

        /**
         * fmin/ fmax have not been projected yet
         */
        int fmin = jsonLocation.getInt(FeatureStringEnum.FMIN.value);
        int fmax = jsonLocation.getInt(FeatureStringEnum.FMAX.value);

        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.getReverseProjectionSequences(fmin, fmax)
//        assert projectionSequenceList.size()==2
        ProjectionSequence firstProjectionSequence = projectionSequenceList.first()
        ProjectionSequence lastProjectionSequence = projectionSequenceList.last()

//        if (false) {
        // case 1, fmin and fmax are both within the projectSequence and match the order, nothing to do
        if (firstProjectionSequence == lastProjectionSequence && projectionSequence.order == firstProjectionSequence.order) {
//            fmin = multiSequenceProjection.projectReverseValue(fmin)
//            fmax = multiSequenceProjection.projectReverseValue(fmax)
        }
        // case 2, fmin and fmax are both outside the projectSequence
        if (firstProjectionSequence.name != projectionSequence.name && lastProjectionSequence.name != projectionSequence.name) {
            return null
        }
        // case 3, fmin is outside to the left and fmax is within it
        if (firstProjectionSequence.name != projectionSequence.name
                && firstProjectionSequence.order < projectionSequence.order
                && lastProjectionSequence.name == projectionSequence.name
                && lastProjectionSequence.order == projectionSequence.order
        ) {
            fmin = projectionSequence.originalOffset
//            fmax = multiSequenceProjection.projectReverseValue(fmax)
        }
        // case 4, fmin is inside and fmax is to the right
        if (lastProjectionSequence.name != projectionSequence.name
                && lastProjectionSequence.order > projectionSequence.order
                && firstProjectionSequence.name == projectionSequence.name
                && firstProjectionSequence.order == projectionSequence.order
        ) {
//            fmin = multiSequenceProjection.projectReverseValue(fmax)
            fmax = projectionSequence.originalOffset + projectionSequence.unprojectedLength
        }
//        }

        // we have to do a reverse projection of it,
        Organism organism = preferenceService.getOrganismForToken(projectionSequence.organism)
        Sequence sequence = Sequence.findByNameAndOrganism(projectionSequence.name, organism)

        FeatureLocation featureLocation = new FeatureLocation();
        if (jsonLocation.has(FeatureStringEnum.ID.value)) {
            featureLocation.setId(jsonLocation.getLong(FeatureStringEnum.ID.value));
        }
        // let's revert this a bit
        fmin = fmin - projectionSequence.originalOffset
        fmax = fmax - projectionSequence.originalOffset

        featureLocation.setFmin(fmin);
        featureLocation.setFmax(fmax);
        if (jsonLocation.getInt(FeatureStringEnum.STRAND.value) == Strand.POSITIVE.value || jsonLocation.getInt(FeatureStringEnum.STRAND.value) == Strand.NEGATIVE.value) {
            featureLocation.setStrand(jsonLocation.getInt(FeatureStringEnum.STRAND.value));
        } else {
            featureLocation.setStrand(defaultStrand)
        }
        featureLocation.setSequence(sequence)
        return featureLocation;
    }


    public Collection<Transcript> getOverlappingTranscripts(Transcript transcript, boolean compareStrands = true) {
        List<Transcript> transcriptList = new ArrayList<>()
        transcript.featureLocations.each {
            transcriptList.addAll(getOverlappingTranscripts(it, compareStrands))
        }
        return transcriptList
    }
    /** Get features that overlap a given location.
     *
     * @param location - FeatureLocation that the features overlap
     * @param compareStrands - Whether to compare strands in overlap
     * @return Collection of Feature objects that overlap the FeatureLocation
     */
    public Collection<Transcript> getOverlappingTranscripts(FeatureLocation location, boolean compareStrands = true) {
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
    public Collection<Feature> getOverlappingFeatures(FeatureLocation location, boolean compareStrands = true) {
        if (compareStrands) {
            //Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax ))",[fmin:location.fmin,fmax:location.fmax,strand:location.strand,sequence:location.sequence])
            Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and fl.strand = :strand and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))", [fmin: location.fmin, fmax: location.fmax, strand: location.strand, sequence: location.sequence])
        } else {
            //Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax ))",[fmin:location.fmin,fmax:location.fmax,sequence:location.sequence])
            Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl where fl.sequence = :sequence and ((fl.fmin <= :fmin and fl.fmax > :fmin) or (fl.fmin <= :fmax and fl.fmax >= :fmax) or (fl.fmin >= :fmin and fl.fmax <= :fmax))", [fmin: location.fmin, fmax: location.fmax, sequence: location.sequence])
        }
    }

    /**
     * Should return a unique set of features.
     * @param locationList
     * @param compareStrands
     * @return
     */
    public Collection<Feature> getOverlappingFeaturesFromList(List<FeatureLocation> locationList, boolean compareStrands = true) {
        List<Feature> overlappingFeatures = []
        locationList.each { it ->
            overlappingFeatures.addAll(getOverlappingFeatures(it, compareStrands))
        }
        return overlappingFeatures.unique()
    }

    /**
     * Set sequence based on FeatureLocation.
     * If a sequence has multiple feature locations, then use the first one?
     *
     * @param feature
     * @param assemblage
     */
    @Transactional
    void updateNewGsolFeatureAttributes(Feature feature, Assemblage assemblage) {

        feature.setIsAnalysis(false);
        feature.setIsObsolete(false);
        if (assemblage) {
            MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)

            Organism organism
            feature.featureLocations.each() {
                // if its set . . . don't reset!
                if (!it.sequence) {
                    ProjectionSequence projectionSequence = multiSequenceProjection.getReverseProjectionSequence(it.fmin)
                    String sequenceName = projectionSequence.name
                    organism = organism ?: preferenceService.getOrganismForToken(projectionSequence.organism)
                    it.sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
                }
            }
        }

        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
            updateNewGsolFeatureAttributes(fr.getChildFeature(), assemblage);
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
    def generateTranscript(JSONObject jsonTranscript, Assemblage assemblage, boolean suppressHistory) {
        Gene gene = jsonTranscript.has(FeatureStringEnum.PARENT_ID.value) ? (Gene) Feature.findByUniqueName(jsonTranscript.getString(FeatureStringEnum.PARENT_ID.value)) : null;
        Transcript transcript = null
        boolean useCDS = configWrapperService.useCDS()

        User owner = permissionService.getCurrentUser(jsonTranscript)
        // if the gene is set, then don't process, just set the transcript for the found gene
        if (gene) {
            // Scenario I - if 'parent_id' attribute is given then find the gene
            transcript = (Transcript) convertJSONToFeature(jsonTranscript, assemblage);
            if (transcript.getFmin() < 0 || transcript.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates")
            }

            setOwner(transcript, owner);

            if (!useCDS || transcriptService.getCDS(transcript) == null) {
                calculateCDS(transcript, false, assemblage);
            }

            addTranscriptToGene(gene, transcript, assemblage);
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript, assemblage);
            if (!suppressHistory) {
                transcript.name = nameService.generateUniqueName(transcript)
            }
        } else {
            // Scenario II - find an overlapping isoform and if present, add current transcript to its gene
//            FeatureLocation featureLocation = convertJSONToFeatureLocation(jsonTranscript.getJSONObject(FeatureStringEnum.LOCATION.value), assemblage, false)
            List<FeatureLocation> featureLocationList = convertJSONToFeatureLocations(jsonTranscript.getJSONObject(FeatureStringEnum.LOCATION.value), assemblage)

            // TODO: this should be the eventual method
//            Collection<Feature> overlappingFeatures = getOverlappingFeatures(featureLocationList).findAll() {
//                it = Feature.get(it.id)
//                it instanceof Gene
//            }
//            FeatureLocation featureLocation = featureLocationList.first()
            Collection<Feature> overlappingFeatures = getOverlappingFeaturesFromList(featureLocationList).findAll() {
                it = Feature.get(it.id)
                it instanceof Gene
            }
//            Collection<Feature> overlappingFeatures = getOverlappingFeatures(featureLocationList.first()).findAll() {
//                it = Feature.get(it.id)
//                it instanceof Gene
//            }

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
                    Transcript tmpTranscript = (Transcript) convertJSONToFeature(jsonTranscript, assemblage);
                    updateNewGsolFeatureAttributes(tmpTranscript, assemblage);
                    if (tmpTranscript.getFmin() < 0 || tmpTranscript.getFmax() < 0) {
                        throw new AnnotationException("Feature cannot have negative coordinates");
                    }

                    //this one is working, but was marked as needing improvement
                    setOwner(tmpTranscript, owner);

                    if (!useCDS || transcriptService.getCDS(tmpTranscript) == null) {
                        calculateCDS(tmpTranscript, false, assemblage);
                    }
                    if (!suppressHistory) {
                        tmpTranscript.name = nameService.generateUniqueName(tmpTranscript, tmpGene.name)
                    }

                    if (overlapperService.overlaps(tmpTranscript, tmpGene)) {
                        log.debug "There is an overlap, adding to an existing gene"
                        transcript = tmpTranscript;
                        gene = tmpGene;
                        addTranscriptToGene(gene, transcript, assemblage)
                        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript, assemblage);
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
                                        } else if (it.tag == tagString && it.value == valueString) {
                                            exists = true
                                        }
                                    }
                                    if (!exists) {
                                        if (tagString == FeatureStringEnum.COMMENT.value) {
                                            // if FeatureProperty is a comment
                                            featurePropertyService.addComment(tmpGene, valueString)
                                        } else {
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
            } else {
                // Scenario IIIb - use the current mRNA's featurelocation for gene
                jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonTranscript));
                jsonGene.put(FeatureStringEnum.LOCATION.value, jsonTranscript.getJSONObject(FeatureStringEnum.LOCATION.value));
                String cvTermString = FeatureStringEnum.GENE.value
                jsonGene.put(FeatureStringEnum.TYPE.value, convertCVTermToJSON(FeatureStringEnum.CV.value, cvTermString));
            }

            String geneName = null
            if (jsonGene.has(FeatureStringEnum.NAME.value)) {
                geneName = jsonGene.get(FeatureStringEnum.NAME.value)
            } else if (jsonTranscript.has(FeatureStringEnum.NAME.value)) {
                geneName = jsonTranscript.getString(FeatureStringEnum.NAME.value)
            } else {
                geneName = nameService.makeUniqueGeneName(assemblage.organism, assemblage.sequenceList, false)
            }
            if (!suppressHistory) {
//                geneName = nameService.makeUniqueFeatureName(sequence.organism, geneName, new LetterPaddingStrategy(), true)
                geneName = nameService.makeUniqueGeneName(assemblage.organism, geneName, true)
            }
            // set back to the original gene name
            if (jsonTranscript.has(FeatureStringEnum.GENE_NAME.value)) {
                geneName = jsonTranscript.getString(FeatureStringEnum.GENE_NAME.value)
            }
            jsonGene.put(FeatureStringEnum.NAME.value, geneName)

            gene = (Gene) convertJSONToFeature(jsonGene, assemblage);

            updateNewGsolFeatureAttributes(gene, assemblage);
            if (gene.getFmin() < 0 || gene.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates");
            }
            transcript = transcriptService.getTranscripts(gene).iterator().next();
            if (!useCDS || transcriptService.getCDS(transcript) == null) {
                calculateCDS(transcript, false, assemblage);
            }
            removeExonOverlapsAndAdjacenciesForFeature(gene, assemblage)
            if (!suppressHistory) {
                transcript.name = nameService.generateUniqueName(transcript)
            }
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript, assemblage);
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
    def removeExonOverlapsAndAdjacenciesForFeature(Feature feature, Assemblage assemblage) {
        if (feature instanceof Gene) {
            for (Transcript transcript : transcriptService.getTranscripts((Gene) feature)) {
                removeExonOverlapsAndAdjacencies(transcript, assemblage);
            }
        } else if (feature instanceof Transcript) {
            removeExonOverlapsAndAdjacencies((Transcript) feature, assemblage);
        }
    }


    @Transactional
    def addTranscriptToGene(Gene gene, Transcript transcript, Assemblage assemblage) {
        removeExonOverlapsAndAdjacencies(transcript, assemblage);
        // no feature location, set location to transcript's
        if (gene.featureLocations == null) {
            transcript.featureLocations.each { transcriptFeatureLocation ->
                FeatureLocation featureLocation = new FeatureLocation()
                featureLocation.properties = transcriptFeatureLocation.properties
                featureLocation.id = null
                featureLocation.save()
                gene.addToFeatureLocations(featureLocation);
            }
        } else {
            // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
            transcriptService.updateGeneBoundaries(transcript, assemblage)
//            if (transcript.fmin < gene.fmin) {
//                gene.getFeatureLocation().setFmin(transcript.getFeatureLocation().getFmin());
//            }
//            if (transcript.getFeatureLocation().getFmax() > gene.getFeatureLocation().getFmax()) {
//                gene.getFeatureLocation().setFmax(transcript.getFeatureLocation().getFmax());
//            }
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
    def removeExonOverlapsAndAdjacencies(Transcript transcript, Assemblage assemblage) throws AnnotationException {
        List<Exon> sortedExons = transcriptService.getSortedExons(transcript, false, assemblage)
        if (!sortedExons || sortedExons?.size() <= 1) {
            return;
        }
        // sort exon if the assemblage is not there
        int inc = 1;
        for (int i = 0; i < sortedExons.size() - 1; i += inc) {
            inc = 1;
            Exon leftExon = sortedExons.get(i);
            for (int j = i + 1; j < sortedExons.size(); ++j) {
                Exon rightExon = sortedExons.get(j);
                if (overlapperService.overlaps(leftExon, rightExon) || isAdjacentTo(leftExon.lastFeatureLocation, rightExon.firstFeatureLocation)) {
                    try {
                        exonService.mergeExons(leftExon, rightExon);
                        sortedExons = transcriptService.getSortedExons(transcript, false, assemblage)
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
    public boolean isAdjacentTo(FeatureLocation leftLocation, FeatureLocation location) {
        return isAdjacentTo(leftLocation, location, true);
    }

    public boolean isAdjacentTo(FeatureLocation leftFeatureLocation, FeatureLocation rightFeatureLocation, boolean compareStrands) {
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

    @Timed
    @Transactional
    def calculateCDS(Transcript transcript, boolean readThroughStopCodon, Assemblage assemblage) {
        CDS cds = transcriptService.getCDS(transcript);
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
        log.info "calculateCDS"
        if (cds == null) {
            setLongestORF(transcript, readThroughStopCodon, multiSequenceProjection);
            return;
        }
        boolean manuallySetStart = cdsService.isManuallySetTranslationStart(cds);
        boolean manuallySetEnd = cdsService.isManuallySetTranslationEnd(cds);
        if (manuallySetStart && manuallySetEnd) {
            return;
        }
        if (!manuallySetStart && !manuallySetEnd) {
            setLongestORF(transcript, readThroughStopCodon, multiSequenceProjection);
        } else if (manuallySetStart) {
            setTranslationStart(transcript, cds.isNegativeStrand() ? cds.getFmax() - 1 : cds.getFmin(), true, configWrapperService.getTranslationTable(), readThroughStopCodon, multiSequenceProjection);
        } else {
            setTranslationEnd(transcript, cds.isNegativeStrand() ? cds.getFmin() : cds.getFmax() - 1, true, configWrapperService.getTranslationTable(), multiSequenceProjection);
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
    public void setLongestORF(Transcript transcript, boolean readThroughStopCodon, MultiSequenceProjection multiSequenceProjection) {
        log.debug "setLongestORF(transcript,readThroughStopCodon) ${transcript} ${readThroughStopCodon}"
        setLongestORF(transcript, configWrapperService.getTranslationTable(), false, readThroughStopCodon, multiSequenceProjection);
    }

    /** Convert local coordinate to source feature coordinate.
     *
     * @param localCoordinate - Coordinate to convert to source coordinate
     * @return Source feature coordinate, -1 if local coordinate is longer than feature's length or negative
     */
    public int convertLocalCoordinateToSourceCoordinate(Feature feature, int localCoordinate) {
        log.debug "convertLocalCoordinateToSourceCoordinate"

        if (localCoordinate < 0 || localCoordinate > feature.getLength()) {
            return -1;
        }

        if (feature.isNegativeStrand()) {
            return feature.fmax - localCoordinate - 1;
        } else {
            return feature.fmin + localCoordinate;
        }
    }

    int convertLocalCoordinateToSourceCoordinateForTranscript(Transcript transcript, int localCoordinate) {
        // Method converts localCoordinate to sourceCoordinate in reference to the Transcript
        List<Exon> exons = transcriptService.getSortedExons(transcript, true)

        Map<String, Integer> offsetMap = generateOffsetMap(transcript)

        int sourceCoordinate = -1;
        if (exons.size() == 0) {
            return convertLocalCoordinateToSourceCoordinate(transcript, localCoordinate);
        }
        int currentLength = 0;
        int currentCoordinate = localCoordinate;
        for (Exon exon : exons) {
            int exonLength = exon.getLength();
            if (currentLength + exonLength >= localCoordinate) {
                if (transcript.isNegativeStrand()) {
                    String sequenceName = exon.lastSequence.name
                    Integer offset = offsetMap.get(sequenceName)
                    sourceCoordinate = exon.getFmax() - currentCoordinate - 1 + offset
                } else {
                    String sequenceName = exon.firstSequence.name
                    Integer offset = offsetMap.get(sequenceName)
                    sourceCoordinate = exon.getFmin() + currentCoordinate + offset
                }
                break;
            }
            currentLength += exonLength;
            currentCoordinate -= exonLength;
        }
        return sourceCoordinate;
    }

    Map<String, Integer> generateOffsetMap(Feature feature) {
        Map<String, Integer> offsetMap = new HashMap()
        int offset = 0

        feature.featureLocations.sort() { a, b ->
            a.rank <=> b.rank
        }.each {
            offsetMap.put(it.sequence.name, offset)
            offset += it.sequence.length
        }
        return offsetMap
    }

    int convertLocalCoordinateToSourceCoordinateForCDS(CDS cds, int localCoordinate) {
        // Method converts localCoordinate to sourceCoordinate in reference to the CDS
        Transcript transcript = transcriptService.getTranscript(cds)
        if (!transcript) {
            return convertLocalCoordinateToSourceCoordinate(cds, localCoordinate);
        }
        int offset = 0;
        List<Exon> exons = transcriptService.getSortedExons(transcript, true)
        if (exons.size() == 0) {
            log.debug "FS::convertLocalCoordinateToSourceCoordinateForCDS() - No exons for given transcript"
            return convertLocalCoordinateToSourceCoordinate(cds, localCoordinate)
        }
        if (transcript.isNegativeStrand()) {
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
                    if (exon.fmin < cds.fmin && exon.isPositiveStrand()) {
                        offset += cds.fmin - exon.fmin
                    } else if (exon.fmax > cds.fmax && exon.isNegativeStrand()) {
                        offset += exon.fmax - cds.fmax
                    }
                }
            }

            if (exon.isNegativeStrand()) {
                offset += exon.fmax - exon.fmax
            } else {
                offset += exon.fmin - exon.fmin
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
    public void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd, TranslationTable translationTable, boolean readThroughStopCodon, MultiSequenceProjection multiSequenceProjection) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            featureRelationshipService.addChildFeature(transcript, cds)
//            transcript.setCDS(cds);
        }
        FeatureLocation transcriptFeatureLocation = FeatureLocation.findByFeature(transcript)
        if (transcriptFeatureLocation.strand == Strand.NEGATIVE.value) {
            setFmax(cds, translationStart + 1, multiSequenceProjection);
        } else {
            setFmin(cds, translationStart, multiSequenceProjection);
        }
        cdsService.setManuallySetTranslationStart(cds, true);
        cdsService.deleteStopCodonReadThrough(cds);

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
                            if (cds.isNegativeStrand()) {
                                setFeatureLocations(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2), convertModifiedLocalCoordinateToSourceCoordinate(transcript, i) + 1)
//                                setFmin(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2))
//                                setFmax(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i) + 1);
                            } else {
                                setFeatureLocations(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i), convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2) + 1)
//                                setFmin(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i));
//                                setFmax(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 2) + 1);
                            }
                        }
                        continue;
                    }
                    if (transcript.isNegativeStrand()) {
                        setFmin(cds, convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 2));
                    } else {
                        setFmax(cds, convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 3));
                    }
                    return;
                }
            }
            if (transcript.isNegativeStrand()) {
                setFmin(transcript, transcript.fmin)
                transcript.firstFeatureLocation.setIsFminPartial(true)
//                cds.featureLocation.setFmin(transcript.getFmin());
//                cds.featureLocation.setIsFminPartial(true);
            } else {
                setFmax(transcript, transcript.fmax)
                transcript.lastFeatureLocation.setIsFminPartial(true)
//                cds.featureLocation.setFmax(transcript.getFmax());
//                cds.featureLocation.setIsFmaxPartial(true);
            }
        }

        Date date = new Date();
        cds.setLastUpdated(date);
        transcript.setLastUpdated(date);


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
    public void setTranslationEnd(Transcript transcript, int translationEnd, boolean setTranslationStart, TranslationTable translationTable, MultiSequenceProjection multiSequenceProjection) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            cds = transcriptService.createCDS(transcript);
            transcriptService.setCDS(transcript, cds);
        }
        if (transcript.isNegativeStrand()) {
            featureService.setFmin(cds, translationEnd)
        } else {
            featureService.setFmax(cds, translationEnd + 1)
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
                    if (transcript.isNegativeStrand()) {
                        setFmax(cds, convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 3))
                    } else {
                        setFmin(cds, convertLocalCoordinateToSourceCoordinateForTranscript(transcript, i + 2))
                    }
                    return;
                }
            }
            if (transcript.isNegativeStrand()) {
                setFmin(cds, transcript.fmin)
                cds.firstFeatureLocation.setIsFminPartial(true);
            } else {
                setFmax(cds, transcript.fmax)
//                cds.featureLocation.setFmax(transcript.getFmax());
                cds.lastFeatureLocation.setIsFmaxPartial(true);
            }
        }

        Date date = new Date();
        cds.setLastUpdated(date);
        transcript.setLastUpdated(date);

    }

    /** Get the residues for a feature with any alterations and frameshifts.
     *
     * @param feature - Feature to retrieve the residues for
     * @return Residues for the feature with any alterations and frameshifts
     */
    public String getResiduesWithAlterationsAndFrameshifts(Feature feature) {
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
        List<Sequence> listOfSequences = Sequence.executeQuery("select s from Feature  f join f.featureLocations fl join fl.sequence s where f = :feature ", [feature: feature])
        List<SequenceAlteration> sequenceAlterationList = new ArrayList<>()
        // TODO: optimize by order somehow to reduce fetches . . . typically the max sequence size is two (and more typically one), so probalby not too big a deal
        for (Sequence sequence in listOfSequences) {
            sequenceAlterationList.addAll(SequenceAlteration.executeQuery("select sa from SequenceAlteration sa join sa.featureLocations fl join fl.sequence s where s = :sequence order by fl.fmin asc ", [sequence: sequence]))
        }
        return sequenceAlterationList
    }

    List<SequenceAlteration> getFrameshiftsAsAlterations(Transcript transcript) {
        List<SequenceAlteration> frameshifts = new ArrayList<SequenceAlteration>();
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            return frameshifts;
        }
        // get sequence for transcript region (generate assemblage) and frameshift coordinate
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(transcript)
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
//        Sequence sequence = cds.getFeatureLocation().sequence
        Organism organism = transcript.organism
        List<Frameshift> frameshiftList = transcriptService.getFrameshifts(transcript)
        for (Frameshift frameshift : frameshiftList) {

            ProjectionSequence projectionSequence1 = multiSequenceProjection.getReverseProjectionSequence(frameshift.coordinate)
            ProjectionSequence projectionSequence2 = multiSequenceProjection.getReverseProjectionSequence(frameshift.coordinate + frameshift.frameshiftValue)
            if (projectionSequence1 != projectionSequence2) {
                throw new AnnotationException("Can not getFrameshits across a scaffold boundary.  Please report this bug.")
            }
            Sequence sequence1 = Sequence.findByNameAndOrganism(projectionSequence1.name, organism)

            if (frameshift.isPlusFrameshift()) {
                // a plus frameshift skips bases during translation, which can be mapped to a deletion for the
                // the skipped bases


                FeatureLocation featureLocation = new FeatureLocation(
                        fmin: frameshift.coordinate
                        , fmax: frameshift.coordinate + frameshift.frameshiftValue
                        , strand: cds.strand
                        , sequence: sequence1
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

            } else {
                // a minus frameshift goes back bases during translation, which can be mapped to an insertion for the
                // the repeated bases
                Insertion insertion = new Insertion(
                        uniqueName: FeatureStringEnum.INSERTION_PREFIX.value + frameshift.coordinate
                        , isAnalysis: false
                        , isObsolete: false
                ).save()

                FeatureLocation featureLocation = new FeatureLocation(
                        fmin: frameshift.coordinate
                        , fmax: frameshift.coordinate + frameshift.frameshiftValue
                        , strand: cds.strand
                        , sequence: sequence1
                ).save()

                insertion.addToFeatureLocations(featureLocation)
                featureLocation.feature = insertion

                String alterationResidues = sequenceService.getRawResiduesFromSequence(sequence1, frameshift.getCoordinate() + frameshift.getFrameshiftValue(), frameshift.getCoordinate())
                insertion.alterationResidue = alterationResidues
                frameshifts.add(insertion);

                insertion.save()
                featureLocation.save()
                frameshift.save(flush: true)
            }
        }
        return frameshifts;
    }

/**
 * Calculate the longest ORF for a transcript.  If a valid start codon is not found, allow for partial CDS start/end.
 *
 * @param transcript - Transcript to set the longest ORF to
 * @param translationTable - Translation table that defines the codon translation
 * @param allowPartialExtension - Where partial ORFs should be used for possible extension
 *
 */
    @Timed
    @Transactional
    public void setLongestORF(Transcript transcript, TranslationTable translationTable, boolean allowPartialExtension, boolean readThroughStopCodon, MultiSequenceProjection multiSequenceProjection) {
        log.debug "setLongestORF(transcript,translationTable,allowPartialExtension,readThroughStopCodon)"
        String mrna = getResiduesWithAlterationsAndFrameshifts(transcript);
        if (mrna == null || mrna.equals("null")) {
            return;
        }
        String longestPeptide = "";
        int bestStartIndex = -1;
        int bestStopIndex = -1;
        boolean partialStop = false;

        if (mrna.length() > 3) {
            for (String startCodon : translationTable.getStartCodons()) {
                int startIndex = mrna.indexOf(startCodon);
                while (startIndex >= 0) {
                    String mrnaSubstring = mrna.substring(startIndex);
                    String aa = SequenceTranslationHandler.translateSequence(mrnaSubstring, translationTable, true, readThroughStopCodon);
                    if (aa.length() > longestPeptide.length()) {
                        longestPeptide = aa;
                        bestStartIndex = startIndex;
                        bestStopIndex = startIndex + (aa.length() * 3);
                        if (!longestPeptide.substring(longestPeptide.length() - 1).equals(TranslationTable.STOP)) {
                            partialStop = true;
                            bestStopIndex += mrnaSubstring.length() % 3;
                        }
                    }
                    startIndex = mrna.indexOf(startCodon, startIndex + 1);
                }
            }
        }

        if (transcript instanceof MRNA) {
            CDS cds = transcriptService.getCDS(transcript)
            if (cds == null) {
                cds = transcriptService.createCDS(transcript);
                transcriptService.setCDS(transcript, cds);
            }
            if (bestStartIndex >= 0) {
                int fmin = convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStartIndex);
                int fmax = convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStopIndex);
                if (cds.isNegativeStrand()) {
                    int tmp = fmin;
                    fmin = fmax + 1;
                    fmax = tmp + 1;
                }
                // get the projection from the transcript,
                setFeatureLocations(cds, fmin, fmax, transcript)
//                setFmin(cds, fmin, multiSequenceProjection);
//                cds.firstFeatureLocation.setIsFminPartial(false);
//                setFmax(cds, fmax, multiSequenceProjection);
//                cds.lastFeatureLocation.setIsFmaxPartial(partialStop);
            } else {
//                setFmin(cds, transcript.getFmin(), multiSequenceProjection);
//                cds.firstFeatureLocation.setIsFminPartial(true);
                String aa = SequenceTranslationHandler.translateSequence(mrna, translationTable, true, readThroughStopCodon);
                if (aa.substring(aa.length() - 1).equals(TranslationTable.STOP)) {
                    setFeatureLocations(cds, transcript.fmin, convertModifiedLocalCoordinateToSourceCoordinate(transcript, aa.length() * 3), multiSequenceProjection)
//                    setFmax(cds, convertModifiedLocalCoordinateToSourceCoordinate(transcript, aa.length() * 3), multiSequenceProjection);
                    cds.lastFeatureLocation.setIsFmaxPartial(false);
                } else {
                    setFeatureLocations(cds, transcript.fmin, transcript.fmax, multiSequenceProjection)
//                    setFmax(cds, transcript.getFmax(), multiSequenceProjection);
//                    cds.lastFeatureLocation.setIsFmaxPartial(true);
                }
            }
            if (readThroughStopCodon) {
                String aa = SequenceTranslationHandler.translateSequence(getResiduesWithAlterationsAndFrameshifts(cds), translationTable, true, true);
                int firstStopIndex = aa.indexOf(TranslationTable.STOP);
                if (firstStopIndex < aa.length() - 1) {
                    StopCodonReadThrough stopCodonReadThrough = cdsService.createStopCodonReadThrough(cds);
                    cdsService.setStopCodonReadThrough(cds, stopCodonReadThrough);
                    int offset = transcript.getStrand() == -1 ? -2 : 0;
                    setFeatureLocations(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + offset, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + 3 + offset, multiSequenceProjection)
//                    setFmin(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + offset, multiSequenceProjection);
//                    setFmax(stopCodonReadThrough, convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + 3 + offset, multiSequenceProjection);
                }
            } else {
                cdsService.deleteStopCodonReadThrough(cds);
            }
            cdsService.setManuallySetTranslationStart(cds, false);
            cdsService.setManuallySetTranslationEnd(cds, false);
        }

    }

//    @Timed
//    @Transactional
//    public Feature convertJSONToFeature(JSONObject jsonFeature, Assemblage assemblage) {
//        List<Sequence> sequenceList = assemblageService.getSequencesFromAssemblage(assemblage)
//        if(!sequenceList || sequenceList.size()>1){
//            log.error("trying to convert a feature that has multiple sequences: ${assemblage}")
//        }
//        return convertJSONToFeature(jsonFeature,sequenceList.first())
//    }

    @Timed
    @Transactional
    public Feature convertJSONToFeature(JSONObject jsonFeature, Assemblage assemblage) {
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
            } else {
                gsolFeature.name = gsolFeature.uniqueName + "-${type.get('name')}"
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

            // TODO: will have to split this
            if (jsonFeature.has(FeatureStringEnum.LOCATION.value)) {
                JSONObject jsonLocation = jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value);
                List<FeatureLocation> featureLocationList
                if (singletonFeatureTypes.contains(type.getString(FeatureStringEnum.NAME.value))) {
                    featureLocationList = convertJSONToFeatureLocations(jsonLocation, assemblage, Strand.NONE.value)
//                    featureLocation = convertJSONToFeatureLocation(jsonLocation, assemblage, false, Strand.NONE.value)
                } else {
                    featureLocationList = convertJSONToFeatureLocations(jsonLocation, assemblage)
//                    featureLocation = convertJSONToFeatureLocation(jsonLocation, assemblage, false)
                }

                for (FeatureLocation featureLocation in featureLocationList) {
                    featureLocation.feature = gsolFeature
                    featureLocation.save()
                    gsolFeature.addToFeatureLocations(featureLocation)
                }
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
                    Feature child = convertJSONToFeature(childObject, assemblage);
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
                    } else {
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
                        } else {
                            log.warn "Ignoring status ${propertyValue} as its not defined."
                        }
                    } else {
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
        setFmin(gene, geneFmin)
        setFmax(gene, geneFmax)
        gene.setLastUpdated(new Date());
    }

    @Transactional
    def setFmax(Feature feature, int fmax) {
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(feature)
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
        setFmax(feature, fmax, multiSequenceProjection)
    }

    @Transactional
    def setFmin(Feature feature, int fmin) {
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(feature)
        MultiSequenceProjection multiSequenceProjection = projectionService.createMultiSequenceProjection(assemblage)
        setFmin(feature, fmin, multiSequenceProjection)
    }

    /**
     * To set Fmin properly, you have to get the proper feature location
     * associated with the proper sequence.
     * Going from less to most, you have to delete anything else
     * @param feature
     * @param fmin
     * @param multiSequenceProjection
     * @return
     */
    @Transactional
    def setFmin(Feature feature, int fmin, MultiSequenceProjection multiSequenceProjection) {
        setFeatureLocations(feature, fmin, null, multiSequenceProjection)
    }

    /**
     * Generate the projection from the feature
     * @param feature
     * @param fmin
     * @param fmax
     */
    @Transactional
    def setFeatureLocations(Feature feature, Integer fmin, Integer fmax) {
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(feature)
        setFeatureLocations(feature, fmin, fmax, projectionService.createMultiSequenceProjection(assemblage))
    }

    /**
     * Generate the projection from the feature
     * @param feature
     * @param fmin
     * @param fmax
     */
    @Transactional
    def setFeatureLocations(Feature feature, Integer fmin, Integer fmax, Feature contextFeature) {
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(contextFeature)
        setFeatureLocations(feature, fmin, fmax, projectionService.createMultiSequenceProjection(assemblage))
    }

    /**
     * This method sets the feature locations for a feature over a projection.
     * By definition, the first feature location must contain an fmin and the last must contain an fmax.
     * The intermediates must contain the full scaffold length and will be defined by the multiSequenceProjection.
     *
     * We also need to make sure that the featureLocationMap has the proper order, as well.
     *
     *
     * @param feature
     * @param fmin In the context of the projection only, not the feature
     * @param fmax In the context of the projection only, not the feature
     * @param multiSequenceProjection
     */
    @Transactional
    def setFeatureLocations(Feature feature, Integer fmin, Integer fmax, MultiSequenceProjection multiSequenceProjection) {

        Map<String, FeatureLocation> featureLocationMap = feature.featureLocations.sort() { a, b -> a.rank <=> b.rank ?: a.fmin <=> b.fmin }.collectEntries() {
            [it.sequence.name, it]
        }
        Strand strand = Strand.getStrandForValue(feature.firstFeatureLocation?.strand)
        println "# of feature locations ${feature.featureLocations}"
        Organism organism = preferenceService.getOrganismForToken(multiSequenceProjection.projectedSequences.first().organism)
        Map<String, Sequence> sequenceMap = Sequence.findAllByNameInListAndOrganism(multiSequenceProjection.projectedSequences.name, organism).collectEntries() {
            [(it.name): it]
        }

        ProjectionSequence firstProjectionSequence = fmin ? multiSequenceProjection.getProjectionSequence(fmin) : null
        ProjectionSequence lastProjectionSequence = fmax ? multiSequenceProjection.getProjectionSequence(fmax) : null


        List<FeatureLocation> toDelete = new ArrayList<>()
        List<FeatureLocation> toAdd = new ArrayList<>()

        int featureLocationOrder = 0
        for (ProjectionSequence projectionSequence in multiSequenceProjection.projectedSequences) {

            // if we only have this one
            if (firstProjectionSequence && lastProjectionSequence && projectionSequence.order == firstProjectionSequence.order && firstProjectionSequence.order == lastProjectionSequence.order) {
                FeatureLocation featureLocation = featureLocationMap.get(projectionSequence.name)
                if (!featureLocation) {
                    featureLocation = new FeatureLocation(
                            feature: feature,
                            sequence: sequenceMap.get(firstProjectionSequence.name),
                            fmin: fmin - firstProjectionSequence.originalOffset,
                            isFminPartial: false,
                            fmax: fmax - firstProjectionSequence.originalOffset,
                            isFmaxPartial: false,
                            strand: strand.value,
                            rank: 0
                    ).save(insert: true, failOnError: true)
                    toAdd.add(featureLocation)

                    // we remove anything that is currently on there then, which is probably nothing
                    toDelete.addAll(feature.featureLocations)
                } else {
                    featureLocation.fmin = fmin - firstProjectionSequence.originalOffset
                    featureLocation.isFminPartial = false
                    featureLocation.rank = 0
                    featureLocation.fmax = fmax - firstProjectionSequence.originalOffset
                    featureLocation.isFmaxPartial = false
                    featureLocation.save(insert: false, failOnError: true)
                }
            }
            else
            // set fmin, so add and/or update to set fmin
            if (projectionSequence.order == firstProjectionSequence?.order && firstProjectionSequence?.order != lastProjectionSequence?.order) {
                featureLocationOrder = 0
                FeatureLocation featureLocation = featureLocationMap.get(projectionSequence.name)
                if (!featureLocation) {
                    featureLocation = new FeatureLocation(
                            feature: feature,
                            sequence: sequenceMap.get(firstProjectionSequence.name),
                            fmin: fmin - firstProjectionSequence.originalOffset,
                            isFminPartial: false,
                            fmax: projectionSequence.unprojectedLength,
                            isFmaxPartial: true,
                            strand: strand.value,
                            rank: featureLocationOrder
                    ).save(insert: true, failOnError: true)
                    bumpFeatureLocationRanks(feature)
                    toAdd.add(featureLocation)
                } else {
                    featureLocation.fmin = fmin - firstProjectionSequence.originalOffset
                    featureLocation.isFminPartial = false
                    featureLocation.rank = featureLocationOrder
                    // if this is not fmax projectionSequence, then we have to set to the max
                    if (firstProjectionSequence && lastProjectionSequence && firstProjectionSequence.order < lastProjectionSequence.order) {
                        featureLocation.fmax = projectionSequence.unprojectedLength
                        featureLocation.isFmaxPartial = true
                    }
                    featureLocation.save(insert: false, failOnError: true)
                }
            }
            else
            // set fmin, so add and/or update to set fmin
            // set fmax, so add and/or update to set fmax
            if (projectionSequence.order == lastProjectionSequence?.order && firstProjectionSequence?.order != lastProjectionSequence?.order) {
                ++featureLocationOrder
                FeatureLocation featureLocation = featureLocationMap.get(projectionSequence.name)
                if (!featureLocation) {
                    featureLocation = new FeatureLocation(
                            feature: feature,
                            sequence: sequenceMap.get(lastProjectionSequence.name),
                            fmin: 0,
                            isFminPartial: true,
                            fmax: fmax - lastProjectionSequence.originalOffset,
                            isFmaxPartial: false,
                            strand: strand.value,
                            rank: featureLocationOrder
                    ).save(insert: true, failOnError: true)
                    toAdd.add(featureLocation)
                } else {
                    featureLocation.fmax = fmax - lastProjectionSequence.originalOffset
                    featureLocation.isFminPartial = false
                    if (firstProjectionSequence && lastProjectionSequence && firstProjectionSequence.order < lastProjectionSequence.order) {
                        featureLocation.fmin = 0
                        featureLocation.isFminPartial = true
                    }
                    featureLocation.save(insert: false, failOnError: true)
                }
            }


            if (firstProjectionSequence && lastProjectionSequence) {
                // below or above fmin or fmax, so delete
                FeatureLocation featureLocation = featureLocationMap.get(projectionSequence.name)
                if (projectionSequence.order < firstProjectionSequence.order || projectionSequence.order > lastProjectionSequence.order) {
                    if (featureLocation) {
                        toDelete.add(featureLocation)
                    }
                }
                // inbetween the fmin and the fmax, so add and update to full scaffold length
                else if (projectionSequence.order > firstProjectionSequence.order && projectionSequence.order < lastProjectionSequence.order) {
                    ++featureLocationOrder
                    if (!featureLocation) {
                        featureLocation = new FeatureLocation(
                                feature: feature,
                                sequence: sequenceMap.get(projectionSequence.name),
                                fmin: 0,
                                isFminPartial: true,
                                fmax: projectionSequence.unprojectedLength,
                                isFmaxPartial: true,
                                strand: strand.value,
                                rank: featureLocationOrder
                        ).save(insert: true, failOnError: true)
                        toAdd.add(featureLocation)
                    } else {
                        featureLocation.fmin = 0
                        featureLocation.isFminPartial = true
                        featureLocation.fmax = projectionSequence.unprojectedLength
                        featureLocation.isFmaxPartial = true
                        featureLocation.rank = featureLocationOrder
                        featureLocation.save(insert: false,failOnError: true)
                    }
                }
            }
        }

        // remove to delete
        toDelete.each {
            feature.removeFromFeatureLocations(it)
        }

        toAdd.each {
            feature.addToFeatureLocations(it)
        }

        feature.save(flush: true, insert: false)

    }

    @Transactional
    int bumpFeatureLocationRanks(Feature feature) {

        int bumped = 0
        for(FeatureLocation featureLocation in feature.featureLocations){
            featureLocation.rank = featureLocation.rank +1
            featureLocation.save(insert:false)
            ++bumped
        }
        return bumped
    }

    @Transactional
    def setFmax(Feature feature, int fmax, MultiSequenceProjection multiSequenceProjection) {
        setFeatureLocations(feature, null, fmax, multiSequenceProjection)
    }

    /** Convert source feature coordinate to local coordinate.
     *
     * @param sourceCoordinate - Coordinate to convert to local coordinate
     * @return Local coordinate, -1 if source coordinate is <= fmin or >= fmax
     */
    public int convertSourceCoordinateToLocalCoordinate(Feature feature, int sourceCoordinate) {
        return convertSourceCoordinateToLocalCoordinate(feature.fmin, feature.fmax, Strand.getStrandForValue(feature.strand), sourceCoordinate)
    }

    public int convertSourceCoordinateToLocalCoordinate(int fmin, int fmax, Strand strand, int sourceCoordinate) {
        if (sourceCoordinate < fmin || sourceCoordinate > fmax) {
            return -1;
        }
        if (strand == Strand.NEGATIVE) {
            return fmax - 1 - sourceCoordinate;
        } else {
            return sourceCoordinate - fmin;
        }
    }

    public int convertSourceCoordinateToLocalCoordinateForTranscript(Feature feature, int sourceCoordinate) {
        List<Exon> exons = transcriptService.getSortedExons(feature, true)
        int localCoordinate = -1
        int currentCoordinate = 0
        for (Exon exon : exons) {
            if (exon.fmin <= sourceCoordinate && exon.fmax >= sourceCoordinate) {
                //sourceCoordinate falls within the exon
                if (exon.isNegativeStrand()) {
                    localCoordinate = currentCoordinate + (exon.fmax - sourceCoordinate) - 1;
                } else {
                    localCoordinate = currentCoordinate + (sourceCoordinate - exon.fmin);
                }
            }
            currentCoordinate += exon.getLength();
        }
        return localCoordinate
    }


    public int convertSourceCoordinateToLocalCoordinateForCDS(Feature feature, int sourceCoordinate) {
        List<Exon> exons = transcriptService.getSortedExons(feature, true)
        CDS cds = transcriptService.getCDS(feature)
        int localCoordinate = 0

        if (!(cds.fmin <= sourceCoordinate && cds.fmax >= sourceCoordinate)) {
            return -1
        }
        int x = 0
        int y = 0
        if (feature.isPositiveStrand()) {
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


    public void removeFeatureRelationship(Transcript transcript, Feature feature) {

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
    JSONObject convertFeatureToJSONLite(Feature gsolFeature, boolean includeSequence, int depth, Assemblage assemblage) {
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

        // TODO: use a sequence string see how long this is since LOCATION is an object and not an array

        // TODO: make sure we add a sequenceList in here for seqeuence, which will get interpreted as a track

        // TODO: see what intepretations in both the client and the server code do
        List<FeatureLocation> featureLocations = gsolFeature.getFeatureLocations()?.sort() { it.rank };
        if (featureLocations) {
//            if(featureLocations.size()==1){
//                jsonFeature.put(FeatureStringEnum.LOCATION.value, convertFeatureLocationToJSON(featureLocations.first()));
//            }
//            else{
            jsonFeature.put(FeatureStringEnum.SEQUENCE.value, gsolFeature.sequenceNames);
//            jsonFeature.put(FeatureStringEnum.LOCATION.value, convertFeatureLocationToJSON(gsolFeature.featureLocation));
//                assemblage = assemblage ?: assemblageService.generateAssemblageForFeature(gsolFeature)
            jsonFeature.put(FeatureStringEnum.LOCATION.value, createFeatureLocationJSONFromAssemblage(gsolFeature, assemblage));
//            }
        }


        if (depth <= 1) {
            List<Feature> childFeatures = featureRelationshipService.getChildrenForFeatureAndTypes(gsolFeature)
            if (childFeatures) {
                JSONArray children = new JSONArray();
                jsonFeature.put(FeatureStringEnum.CHILDREN.value, children);
                for (Feature f : childFeatures) {
                    Feature childFeature = f
                    children.put(convertFeatureToJSONLite(childFeature, includeSequence, depth + 1, assemblage));
                }
            }
        }

        jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, gsolFeature.lastUpdated.time);
        jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, gsolFeature.dateCreated.time);
        return jsonFeature;
    }

    JSONObject createFeatureLocationJSONFromAssemblage(Feature feature, Assemblage assemblage) {
        int calculatedFmin = -1
        int calculatedFmax = -1
        Boolean fminPartial = false
        Boolean fmaxPartial = false

        assemblage = assemblage ?: assemblageService.generateAssemblageForFeature(feature)
        List<Sequence> sequenceList = assemblageService.getSequencesFromAssemblage(assemblage)
        MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
//        List<ProjectionSequence> projectionSequenceList = projection.getProjectedSequences()

        List<FeatureLocation> featureLocations = feature.getFeatureLocations()?.sort() { it.rank };
//        int offset = 0
        for (FeatureLocation featureLocation in featureLocations) {
            // calculate offset based on index
            int featureLocationIndex = sequenceList.indexOf(featureLocation.sequence)
            if (featureLocationIndex >= 0) {
//                ProjectionSequence projectionSequence = projectionSequenceList.get(featureLocationIndex)
                if (calculatedFmin < 0) {
//                    calculatedFmin = featureLocation.fmin  + projectionSequence.offset // only does the first one, which is typically 0
                    calculatedFmin = featureLocation.fmin
                    fminPartial = featureLocation.isFminPartial
                }
//                calculatedFmax = featureLocation.fmax + projectionSequence.offset
//                calculatedFmax = featureLocation.fmax
//                fmaxPartial = featureLocation.isFmaxPartial
                calculatedFmax = calculatedFmax < 0 ? featureLocation.fmax : featureLocation.fmax + calculatedFmax
                fmaxPartial = featureLocation.isFmaxPartial
            }
//            offset += featureLocation.sequence.end
        }


        return generateFeatureLocationToJSON(assemblage.sequenceList, feature.strand, calculatedFmin, calculatedFmax, fminPartial, fmaxPartial)
    }

    String generateOwnerString(Feature feature) {
        String finalOwnerString
        if (feature.owners) {
            String ownerString = ""
            for (owner in feature.owners) {
                ownerString += feature.owner.username + " "
            }
            finalOwnerString = ownerString?.trim()
        } else if (feature.owner) {
            finalOwnerString = feature?.owner?.username
        } else {
            finalOwnerString = "None"
        }
    }


    JSONObject convertFeatureToJSON(Feature gsolFeature, boolean includeSequence = false) {
        return convertFeatureToJSON(gsolFeature, includeSequence, assemblageService.generateAssemblageForFeature(gsolFeature))
    }

    /**
     * @param gsolFeature
     * @param includeSequence
     * @return
     */
    @Timed
    JSONObject convertFeatureToJSON(Feature gsolFeature, boolean includeSequence = false, Assemblage assemblage) {
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
        jsonFeature.put(FeatureStringEnum.SEQUENCE.value, gsolFeature.sequenceNames);

        durationInMilliseconds = System.currentTimeMillis() - start;


        start = System.currentTimeMillis();

        // get children
        List<Feature> childFeatures = featureRelationshipService.getChildrenForFeatureAndTypes(gsolFeature)


        durationInMilliseconds = System.currentTimeMillis() - start;
        if (childFeatures) {
            JSONArray children = new JSONArray();
            jsonFeature.put(FeatureStringEnum.CHILDREN.value, children);
            for (Feature f : childFeatures) {
                Feature childFeature = f
                children.put(convertFeatureToJSON(childFeature, includeSequence, assemblage));
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
            jsonFeature.put(FeatureStringEnum.PARENT_TYPE.value, generateJSONFeatureStringForType(parent.ontologyId));
        }


        start = System.currentTimeMillis()

        List<FeatureLocation> featureLocations = gsolFeature.getFeatureLocations()?.sort() { it.rank };
        if (featureLocations) {
//            if(featureLocations.size()==1){
//                jsonFeature.put(FeatureStringEnum.LOCATION.value, convertFeatureLocationToJSON(featureLocations.first()));
//            }
//            else{
            // TODO: should probably move somewhere else, but the important part here is that it calculates a SINGLE location
            // for the relevant assemblage
            jsonFeature.put(FeatureStringEnum.LOCATION.value, createFeatureLocationJSONFromAssemblage(gsolFeature, assemblage))
//            }
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
    JSONObject generateFeatureLocationToJSON(String sequenceString, Integer strand, Integer fmin, Integer fmax, Boolean fminPartial = false, Boolean fmaxPartial = false) throws JSONException {
        JSONObject jsonFeatureLocation = new JSONObject();
        jsonFeatureLocation.put(FeatureStringEnum.FMIN.value, fmin)
        jsonFeatureLocation.put(FeatureStringEnum.FMAX.value, fmax)
        if (fmin == null || fminPartial) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMIN_PARTIAL.value, true);
        }
        if (fmax == null || fmaxPartial) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMAX_PARTIAL.value, true);
        }
        jsonFeatureLocation.put(FeatureStringEnum.STRAND.value, strand);
        jsonFeatureLocation.put(FeatureStringEnum.SEQUENCE.value, sequenceString);
        return jsonFeatureLocation
    }

    @Timed
    JSONObject convertFeatureLocationToJSON(FeatureLocation gsolFeatureLocation, Integer fmin = null, Integer fmax = null) throws JSONException {
        JSONObject jsonFeatureLocation = new JSONObject();
        if (gsolFeatureLocation.id) {
            jsonFeatureLocation.put(FeatureStringEnum.ID.value, gsolFeatureLocation.id);
        }
        jsonFeatureLocation.put(FeatureStringEnum.FMIN.value, fmin ?: gsolFeatureLocation.getFmin());
        jsonFeatureLocation.put(FeatureStringEnum.FMAX.value, fmax ?: gsolFeatureLocation.getFmax());
        if (fmin == null && gsolFeatureLocation.isIsFminPartial()) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMIN_PARTIAL.value, true);
        }
        if (fmax == null && gsolFeatureLocation.isIsFmaxPartial()) {
            jsonFeatureLocation.put(FeatureStringEnum.IS_FMAX_PARTIAL.value, true);
        }
        jsonFeatureLocation.put(FeatureStringEnum.STRAND.value, gsolFeatureLocation.getStrand());
        if (gsolFeatureLocation.sequence) {
            jsonFeatureLocation.put(FeatureStringEnum.SEQUENCE.value, gsolFeatureLocation.sequence.name);
        }
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
     * From https://github.com/GMOD/Apollo/issues/73
     * Need to add another call after other calculations are done to verify that we verify that we have not left our current isoform siblings or that we have just joined some and we should merge genes (always taking the one on the left).
     1 - using OrfOverlapper, find other isoforms
     2 - for each isoform, confirm that they belong to the same gene (if not, we merge genes)
     3 - confirm that no other non-overlapping isoforms have the same gene (if not, we create a new gene)
     * @param transcript
     */
    @Transactional
    def handleDynamicIsoformOverlap(Transcript transcript) {
        // Get all transcripts that overlap transcript and verify if they have the proper parent gene assigned
        List<Transcript> allOverlappingTranscripts = getTranscriptsWithOverlappingOrf(transcript)
        List<Transcript> allTranscriptsForCurrentGene = transcriptService.getTranscripts(transcriptService.getGene(transcript))
        List<Transcript> allTranscripts = (allOverlappingTranscripts + allTranscriptsForCurrentGene).unique()
        List<Transcript> allSortedTranscripts = allTranscripts?.sort() { a, b -> a.fmin <=> b.fmin }
        if (transcript.isPositiveStrand()) {
            allSortedTranscripts = allTranscripts?.sort() { a, b -> a.fmin <=> b.fmin }
        }
        if (transcript.isNegativeStrand()) {
            allSortedTranscripts = allTranscripts?.sort() { a, b -> b.fmax <=> a.fmax }
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
            if (overlapperService.overlaps(eachTranscript, fivePrimeGene)) {
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
                addTranscriptToGene(mergedGene, eachTranscript, assemblageService.generateAssemblageForFeatures(mergedGene, eachTranscript))
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

                    firstTranscript.featureLocations.each { featureLocation ->
                        FeatureLocation newGeneFeatureLocation = new FeatureLocation(
                                feature: newGene,
                                fmin: featureLocation.fmin,
                                fmax: featureLocation.fmax,
                                strand: featureLocation.strand,
                                sequence: featureLocation.sequence,
                                residueInfo: featureLocation.residueInfo,
                                locgroup: featureLocation.locgroup,
                                rank: featureLocation.rank
                        ).save(flush: true)
                        newGene.addToFeatureLocations(newGeneFeatureLocation)
                    }
                    featureRelationshipService.removeFeatureRelationship(transcriptService.getGene(firstTranscript), firstTranscript)
                    addTranscriptToGene(newGene, firstTranscript, assemblageService.generateAssemblageForFeatures(newGene, eachTranscript))
                    firstTranscript.name = nameService.generateUniqueName(firstTranscript, newGene.name)
                    firstTranscript.save(flush: true)
                    continue
                }
                if (overlapperService.overlaps(eachTranscript, firstTranscript)) {
                    featureRelationshipService.removeFeatureRelationship(transcriptService.getGene(eachTranscript), eachTranscript)
                    Gene firstGene = transcriptService.getGene(firstTranscript)
                    addTranscriptToGene(firstGene, eachTranscript, assemblageService.generateAssemblageForFeatures(firstGene, eachTranscript))
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
        List<Transcript> overlappingTranscripts = getOverlappingTranscripts(transcript)
        overlappingTranscripts.remove(transcript) // removing itself
        ArrayList<Transcript> transcriptsWithOverlappingOrf = new ArrayList<Transcript>()
        for (Transcript eachTranscript in overlappingTranscripts) {
            if (overlapperService.overlaps(eachTranscript, transcript)) {
                transcriptsWithOverlappingOrf.add(eachTranscript)
            }
        }
        return transcriptsWithOverlappingOrf
    }

    @Transactional
    Gene mergeGeneEntities(Gene mainGene, List<Gene> genes) {
        def fmin = mainGene.fmin
        def fmax = mainGene.fmax
        genes.each {
            fmin = it.fmin < fmin ? it.fmin : fmin
            fmax = it.fmax < fmax ? it.fmax : fmax
        }

        setFmin(mainGene, fmin)
        setFmax(mainGene, fmax)
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

    /**
     * @param features
     * @param assemblage
     * @return
     */
    List<Feature> sortFeatures(Collection<Feature> features, Assemblage assemblage) {
        // generate a assemblage from the first feature sequence if not defined
        if (!assemblage) {
            assemblage = assemblageService.generateAssemblageForFeature(features.first())
        }

        // populate map of sequences to features using the correct order
        Map<Sequence, List<Feature>> firstSequenceFeatureMap = new HashMap<>()
        List<ProjectionSequence> firstProjectionSequenceList = projectionService.createMultiSequenceProjection(assemblage).getProjectedSequences()

        firstProjectionSequenceList.each {
            Organism organism = preferenceService.getOrganismForToken(it.organism)
            Sequence sequence = Sequence.findByNameAndOrganism(it.name, organism)
            firstSequenceFeatureMap.put(sequence, new ArrayList<Feature>())
        }

        // populate the features based on its first sequence
        features.each {
            // find matching sequence for each
            Sequence sequence = it.firstSequence
            firstSequenceFeatureMap.get(sequence).add(it)
        }

        // sort list of features for each sequence and add them back
        List<Feature> returnList = new ArrayList<>()

        firstProjectionSequenceList.each {
            if (firstSequenceFeatureMap.containsKey(it)) {
                Collections.sort(firstSequenceFeatureMap.get(it), new FeaturePositionComparator<Feature>(false))
                returnList.addAll(firstSequenceFeatureMap.get(it))
            }
        }

        return returnList
    }

    private class SequenceAlterationInContextPositionComparator<SequenceAlterationInContext> implements Comparator<SequenceAlterationInContext> {
        public int compare(SequenceAlterationInContext obj1, SequenceAlterationInContext obj2) {
            return obj1.fmin - obj2.fmin
        }
    }

    def sequenceAlterationInContextOverlapper(Feature feature, SequenceAlterationInContext sequenceAlteration) {
        List<Exon> exonList = transcriptService.getSortedExons(feature, true)
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

    String getResiduesWithAlterations(Feature feature, Collection<SequenceAlteration> sequenceAlterations) {
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
            if (feature.isNegativeStrand()) {
                sequenceAlterationResidues = SequenceTranslationHandler.reverseComplementSequence(sequenceAlterationResidues);
            }
            // Insertions
            if (sequenceAlteration.instanceOf == Insertion.canonicalName) {
                if (feature.isNegativeStrand()) {
                    ++localCoordinate;
                }
                residues.insert(localCoordinate + currentOffset, sequenceAlterationResidues);
                currentOffset += sequenceAlterationResidues.length();
            }
            // Deletions
            else if (sequenceAlteration.instanceOf == Deletion.canonicalName) {
                if (feature.isNegativeStrand()) {
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
        List<SequenceAlteration> sequenceAlterations = new ArrayList<>()

        sessionFactory.currentSession.flushMode = FlushMode.MANUAL
        feature.featureLocations.each { featureLocation ->
            Sequence sequence = featureLocation.sequence
            sequenceAlterations.addAll(SequenceAlteration.executeQuery("select distinct sa from SequenceAlteration sa join sa.featureLocations fl where fl.fmin >= :fmin and fl.fmin <= :fmax or fl.fmax >= :fmin and fl.fmax <= :fmax and fl.sequence = :seqId", [fmin: fmin, fmax: fmax, seqId: sequence]))
        }
        sessionFactory.currentSession.flushMode = FlushMode.AUTO

        return sequenceAlterations
    }


    public List<SequenceAlterationInContext> getSequenceAlterationsInContext(Feature feature, Collection<SequenceAlteration> sequenceAlterations) {
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
            List<Exon> exonList
            if (feature instanceof CDS) {
                exonList = transcriptService.getSortedExons(transcriptService.getTranscript(feature), true)
            } else {
                exonList = transcriptService.getSortedExons((Transcript) feature, true)
            }

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

    public int convertModifiedLocalCoordinateToSourceCoordinate(Feature feature, int localCoordinate) {
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
        if (feature.getStrand() == -1) {
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

            if (feature.isNegativeStrand()) {
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

    public int convertSourceToModifiedLocalCoordinate(Feature feature, Integer localCoordinate, List<SequenceAlteration> alterations = new ArrayList<>()) {
        log.debug "convertSourceToModifiedLocalCoordinate"

        if (alterations.size() == 0) {
            log.debug "No alterations returning ${localCoordinate}"
            return localCoordinate
        }


        Collections.sort(alterations, new FeaturePositionComparator<SequenceAlteration>());
        if (feature.isNegativeStrand()) {
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

            if (feature.isNegativeStrand()) {
                coordinateInContext = feature.length - coordinateInContext
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


    def changeAnnotationType(JSONObject inputObject, Feature feature, Assemblage assemblage, User user, String type) {
        String uniqueName = feature.uniqueName
        String originalType = feature.alternateCvTerm ? feature.alternateCvTerm : feature.cvTerm
        JSONObject currentFeatureJsonObject = convertFeatureToJSON(feature, false, assemblage)
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
                transcript = generateTranscript(currentFeatureJsonObject, assemblage, true)
                setLongestORF(transcript, false, projectionService.createMultiSequenceProjection(assemblage))
            } else {
                // *RNA to *RNA
                transcript = addFeature(currentFeatureJsonObject, assemblage, user, true)
                setLongestORF(transcript, false, projectionService.createMultiSequenceProjection(assemblage))
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
            Feature singleton = addFeature(currentFeatureJsonObject, assemblage, user, true)
            newFeature = singleton
        } else if (singletonFeatureTypes.contains(originalType) && singletonFeatureTypes.contains(type)) {
            // singleton to singleton
            currentFeatureJsonObject.put(FeatureStringEnum.UNIQUENAME.value, uniqueName)
            featureRelationshipService.deleteFeatureAndChildren(feature)
            Feature singleton = addFeature(currentFeatureJsonObject, assemblage, user, true)
            newFeature = singleton
        } else {
            log.error "Not enough information available to change ${uniqueName} from ${originalType} -> ${type}."
        }

        // TODO: synonyms, featureSynonyms, featureGenotypes, featurePhenotypes

        return newFeature
    }

    def addFeature(JSONObject jsonFeature, Assemblage assemblage, User user, boolean suppressHistory) {
        Feature returnFeature = null

        if (rnaFeatureTypes.contains(jsonFeature.get(FeatureStringEnum.TYPE.value).name)) {
            Gene gene = jsonFeature.has(FeatureStringEnum.PARENT_ID.value) ? (Gene) Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.PARENT_ID.value)) : null
            Transcript transcript = null

            if (gene) {
                // Scenario I - if 'parent_id' attribute is given then find the gene
                transcript = (Transcript) convertJSONToFeature(jsonFeature, assemblage)
                if (transcript.fmin < 0 || transcript.fmax < 0) {
                    throw new AnnotationException("Feature cannot have negative coordinates")
                }

                setOwner(transcript, user)

                addTranscriptToGene(gene, transcript, assemblage)
                if (!suppressHistory) {
                    String name = nameService.generateUniqueName(transcript)
                    transcript.name = name + "-" + transcript.alternateCvTerm
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
                } else {
                    // Scenario IIIb - use the current mRNA's featurelocation for gene
                    jsonGene.put(FeatureStringEnum.CHILDREN.value, new JSONArray().put(jsonFeature))
                    jsonGene.put(FeatureStringEnum.LOCATION.value, jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value))
                    String cvTermString = jsonFeature.get(FeatureStringEnum.TYPE.value).name == Transcript.alternateCvTerm ? Pseudogene.alternateCvTerm : Gene.alternateCvTerm
                    jsonGene.put(FeatureStringEnum.TYPE.value, convertCVTermToJSON(FeatureStringEnum.CV.value, cvTermString))
                }

                String geneName = null
                if (jsonGene.has(FeatureStringEnum.NAME.value)) {
                    geneName = jsonGene.getString(FeatureStringEnum.NAME.value)
                } else if (jsonFeature.has(FeatureStringEnum.NAME.value)) {
                    geneName = jsonFeature.getString(FeatureStringEnum.NAME.value)
                } else {
                    geneName = nameService.makeUniqueGeneName(assemblage.organism, assemblage.name, false)
                }

                if (!suppressHistory) {
                    geneName = nameService.makeUniqueGeneName(assemblage.organism, geneName, true)
                }

                // set back to the original gene name
                if (jsonFeature.has(FeatureStringEnum.GENE_NAME.value)) {
                    geneName = jsonFeature.getString(FeatureStringEnum.GENE_NAME.value)
                }
                jsonGene.put(FeatureStringEnum.NAME.value, geneName)
                gene = (Gene) convertJSONToFeature(jsonGene, assemblage)
                updateNewGsolFeatureAttributes(gene, assemblage)

                if (gene.fmin < 0 || gene.fmax < 0) {
                    throw new AnnotationException("Feature cannot have negative coordinates")
                }
                transcript = transcriptService.getTranscripts(gene).iterator().next();
                removeExonOverlapsAndAdjacenciesForFeature(gene, assemblage)
                if (!suppressHistory) {
                    String name = nameService.generateUniqueName(transcript)
                    transcript.name = name + "-" + transcript.alternateCvTerm
                }

                gene.save(insert: true)
                transcript.save(flush: true)

                setOwner(gene, user);
                setOwner(transcript, user);
            }

            removeExonOverlapsAndAdjacencies(transcript, assemblage)
            CDS cds = transcriptService.getCDS(transcript)
            if (cds != null) {
                featureRelationshipService.deleteChildrenForTypes(transcript, CDS.ontologyId)
                cds.delete()
            }
            nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript, assemblage)
            returnFeature = transcript
        } else {
            if (!jsonFeature.containsKey(FeatureStringEnum.NAME.value) && jsonFeature.containsKey(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childArray = jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                if (childArray?.size() == 1 && childArray.getJSONObject(0).containsKey(FeatureStringEnum.NAME.value)) {
                    jsonFeature.put(FeatureStringEnum.NAME.value, childArray.getJSONObject(0).getString(FeatureStringEnum.NAME.value))
                }
            }
            Feature feature = convertJSONToFeature(jsonFeature, assemblage)
            if (!suppressHistory) {
                String name = nameService.generateUniqueName(feature, feature.name)
                feature.name = name + "-" + feature.alternateCvTerm
            }
            updateNewGsolFeatureAttributes(feature, assemblage)

            setOwner(feature, user);
            feature.save(insert: true, flush: true)
            if (jsonFeature.get(FeatureStringEnum.TYPE.value).name == Gene.alternateCvTerm ||
                    jsonFeature.get(FeatureStringEnum.TYPE.value).name == Pseudogene.alternateCvTerm) {
                Transcript transcript = transcriptService.getTranscripts(feature).iterator().next()
                setOwner(transcript, user);
                removeExonOverlapsAndAdjacencies(transcript, assemblage)
                CDS cds = transcriptService.getCDS(transcript)
                if (cds != null) {
                    featureRelationshipService.deleteChildrenForTypes(transcript, CDS.ontologyId)
                    cds.delete()
                }
                nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(transcript, assemblage)
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
