package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.Strand

//@GrailsCompileStatic
@Transactional(readOnly = true)
class ExonService {

//    CvTermService cvTermService
    def transcriptService
    def featureService
    def featureRelationshipService
    def sequenceService
    def overlapperService
    def nameService
    def assemblageService
    def projectionService

    /** Retrieve the transcript that this exon is associated with.  Uses the configuration to
     * determine which parent is a transcript.  The transcript object is generated on the fly.  Returns
     * <code>null</code> if this exon is not associated with any transcript.
     *
     * @return Transcript that this Exon is associated with
     */
    public Transcript getTranscript(Exon exon) {

        // this could be for any transcript, though
        return (Transcript) featureRelationshipService.getParentForFeature(exon,transcriptService.ontologyIds as String[])
    }

    /**
     * Merge exon1 and exon2.  The "newly" created exon retains exon1's ID.
     *
     * @param exon1 - Exon to be merged to
     * @param exon2 - Exon to be merged with
     * @throws AnnotationException - If exons don't belong to the same transcript or are in separate strands
     */
    @Transactional
    public void mergeExons(Exon exon1, Exon exon2) throws AnnotationException {
//        // both exons must be part of the same transcript
//        if (!getTranscript(exon1).equals(getTranscript(exon2))) {
//            throw new AnnotationEditorException("mergeExons(): Exons must have same parent transcript", exon1, exon2);
//        }
        // both exons must be in the same strand
        Transcript transcript = getTranscript(exon1);
        Assemblage transcriptAssemblage = assemblageService.generateAssemblageForFeature(transcript)
        if (!exon1?.getStrand()?.equals(exon2?.getStrand())) {
            throw new AnnotationException("mergeExons(): Exons must be in the same strand ${exon1} ${exon2}");
        }
        if (exon1.getFmin() > exon2.getFmin()) {
            setFmin(exon1, exon2.getFmin(),transcriptAssemblage)
        }
        if (exon1.getFmax() < exon2.getFmax()) {
            setFmax(exon1, exon2.fmax,transcriptAssemblage)
        }
        // need to delete exon2 from transcript
        if (getTranscript(exon2) != null) {
            deleteExon(getTranscript(exon2), exon2);
        }
        
        featureService.removeExonOverlapsAndAdjacencies(transcript,transcriptAssemblage);

    }


    /**
     * Delete an exon from a transcript.  If there are no exons left on the transcript, the transcript
     * is deleted from the parent gene.
     *
     * @param transcript - Transcript to have the exon deleted from
     * @param exon - Exon to be deleted from the transcript
     */
    @Transactional
    public void deleteExon(Transcript transcript, Exon exon) {
        featureRelationshipService.removeFeatureRelationship(transcript,exon)


        int fmin = Integer.MAX_VALUE;
        int fmax = Integer.MIN_VALUE;

        // update transcript boundaries if necessary
        if (exon.getFmin().equals(transcript.getFmin())) {
            for (Exon e : transcriptService.getExons(transcript)) {
                if (e.getFmin() < fmin) {
                    fmin = e.getFmin();
                }
            }
            if(fmin!=Integer.MAX_VALUE){
                transcriptService.setFmin(transcript,fmin);
            }
        }
        if (exon.getFmax().equals(transcript.getFmax())) {
            for (Exon e : transcriptService.getExons(transcript)) {
                if (e.getFmax() > fmax) {
                    fmax = e.getFmax();
                }
            }
            if(fmax!=Integer.MIN_VALUE) {
                transcriptService.setFmax(transcript, fmax);
            }
        }
        // update gene boundaries if necessary
        if(fmax > fmin){
            transcriptService.updateGeneBoundaries(transcript);
        }

        exon.save(flush: true)
        exon.featureLocations.clear()
        exon.parentFeatureRelationships?.clear()
        exon.childFeatureRelationships?.clear()
        exon.featureProperties?.clear()
        List<FeatureRelationship> parentFeatures = FeatureRelationship.findAllByChildFeature(exon)
        def childFeatures = FeatureRelationship.findAllByParentFeature(exon)
        if(parentFeatures){
            parentFeatures.each { FeatureRelationship it ->
                FeatureRelationship.executeUpdate("delete from FeatureRelationship fr where fr.id = :frid",[frid:it.id])
            }
        }

        exon.delete(flush: true)
        transcript.save(flush: true)


    }

    /**
     * Need to provide a assemblage to do this
     * @param exon
     * @param fmin
     */
    @Transactional
    public void setFmin(Exon exon, Integer fmin, Assemblage assemblage) {
        org.bbop.apollo.projection.MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
        featureService.setFmin(exon,fmin,projection)
        Transcript transcript = getTranscript(exon)
        if (transcript != null && fmin < transcript.getFmin()) {
            featureService.setFmin(transcript, fmin,projection);
        }
    }

    /**
     * Need to provide a assemblage to do this
     * @param exon
     * @param fmax
     */
    @Transactional
    public void setFmax(Exon exon, Integer fmax, Assemblage assemblage) {
        org.bbop.apollo.projection.MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
        featureService.setFmax(exon,fmax,projection)
        Transcript transcript = getTranscript(exon)
        if (transcript != null && fmax > transcript.getFmax()) {
            featureService.setFmax(transcript, fmax,projection)
        }
    }


    @Transactional
    public Exon makeIntron(Exon exon, int genomicPosition, int minimumIntronSize, Assemblage assemblage) {
        String sequence = sequenceService.getResiduesFromFeature(exon)
        int exonPosition = featureService.convertSourceCoordinateToLocalCoordinate(exon,genomicPosition);
//        // find donor coordinate
        String donorSite = null;
        int donorCoordinate = -1;
        for(String donor : SequenceTranslationHandler.spliceDonorSites){
            int coordinate = sequence.substring(0, exonPosition - minimumIntronSize).lastIndexOf(donor);
            if (coordinate > donorCoordinate) {
                donorCoordinate = coordinate;
                donorSite = donor;
            }
        }
//        // find acceptor coordinate
        String acceptorSite = null;
        int acceptorCoordinate = -1;
        for (String acceptor : SequenceTranslationHandler.getSpliceAcceptorSites()) {
            int coordinate = sequence.substring(exonPosition + minimumIntronSize, sequence.length()).indexOf(acceptor);
            if (acceptorCoordinate == -1 || coordinate < acceptorCoordinate) {
                acceptorCoordinate = coordinate;
                acceptorSite = acceptor;
            }
        }
//        // no donor/acceptor found
        if (donorCoordinate == -1 || acceptorCoordinate == -1 || (acceptorCoordinate - donorCoordinate) == 1) {
            //return splitExon(exon, genomicPosition - 1, genomicPosition + 1, splitExonUniqueName);
            return null;
        }
        acceptorCoordinate += exonPosition + minimumIntronSize;
        if (exon.isNegativeStrand()) {
            int tmp = acceptorCoordinate;
            acceptorCoordinate = donorCoordinate + 1 - donorSite.length();
            donorCoordinate = tmp + 1;
        } else {
            acceptorCoordinate += acceptorSite.length();
        }
        Exon splitExon = splitExon(exon, featureService.convertLocalCoordinateToSourceCoordinate(exon,donorCoordinate) , featureService.convertLocalCoordinateToSourceCoordinate(exon,acceptorCoordinate),assemblage);

        exon.save()
        splitExon.save()

        return splitExon;
    }
//

    /**
     * Set exon boundaries.
     *
     * @param exon - Exon to be modified
     * @param fmin - New fmin to be set
     * @param fmax - New fmax to be set
     */
    @Transactional
    public void setExonBoundaries(Exon exon, int fmin, int fmax) {

        Transcript transcript = getTranscript(exon)
        Assemblage assemblage = assemblageService.generateAssemblageForFeature(exon)
        setFmin(exon,fmin,assemblage)
        setFmax(exon,fmax,assemblage)

        featureService.removeExonOverlapsAndAdjacencies(transcript,assemblage);

        featureService.updateGeneBoundaries(transcriptService.getGene(transcript));
    }

    @Transactional
    def setToDownstreamDonor(Exon exon) {
        Transcript transcript = getTranscript(exon)
        Gene gene = transcriptService.getGene(transcript)

        List<Exon> exons = transcriptService.getSortedExons(transcript,true)

        Integer nextExonFmin = null;
        Integer nextExonFmax = null;
        for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext(); ) {
            Exon e = iter.next();
            if (e.getUniqueName().equals(exon.getUniqueName())) {
                if (iter.hasNext()) {
                    Exon e2 = iter.next();
                    nextExonFmin = e2.fmin
                    nextExonFmax = e2.fmax
                    break;
                }
            }
        }
        FeatureLocation exonFeatureLocation = FeatureLocation.findByFeature(exon)
        int coordinate = exonFeatureLocation.getStrand() == -1 ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exonFeatureLocation.fmin) + 2 : featureService.convertSourceCoordinateToLocalCoordinate(gene,exonFeatureLocation.fmax) + 1;
        String residues = sequenceService.getResiduesFromFeature(gene)
        while (coordinate < residues.length()) {
            int c = featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate);
            if (nextExonFmin != null && (c >= nextExonFmin && c <= nextExonFmax + 1)) {
                throw new AnnotationException("Cannot set to downstream donor - will overlap next exon");
            }
            String seq = residues.substring(coordinate, coordinate + 2);

            if (SequenceTranslationHandler.getSpliceDonorSites().contains(seq)) {
                if (exon.isNegativeStrand()) {
                    setExonBoundaries(exon,featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate)+1,exon.getFmax())
                } else {
                    setExonBoundaries(exon,exon.getFmin(),featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate))
                }
                return;
            }
            ++coordinate;
        }
    }

    @Transactional
    def setToUpstreamDonor(Exon exon) {
        Transcript transcript = getTranscript(exon)
        Gene gene = transcriptService.getGene(transcript)

        FeatureLocation exonFeatureLocation = FeatureLocation.findByFeature(exon)
        int coordinate = exonFeatureLocation.getStrand() == -1 ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exonFeatureLocation.fmin) + 2 : featureService.convertSourceCoordinateToLocalCoordinate(gene,exonFeatureLocation.fmax) + 1;
        int exonStart = exonFeatureLocation.getStrand() == -1 ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmax()) - 1 : featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmin());
        String residues = sequenceService.getResiduesFromFeature(gene)
        while (coordinate > 0 ) {

            if (coordinate <= exonStart) {
                throw new AnnotationException("Cannot set to upstream donor - will remove exon");
            }
            String seq = residues.substring(coordinate, coordinate + 2);

//            log.debug "seq ${seq} in ${SequenceTranslationHandler.spliceDonorSites}"
            if (SequenceTranslationHandler.getSpliceDonorSites().contains(seq)) {
                if (exon.getStrand() == -1) {
                    setExonBoundaries(exon, featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate) + 1, exon.getFmax());
                } else {
                    setExonBoundaries(exon, exon.getFmin(), featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate));
                }
                return;
            }
            --coordinate;
        }
    }

    @Transactional
    def setToUpstreamAcceptor(Exon exon) {
        Transcript transcript = getTranscript(exon);
        Gene gene = transcriptService.getGene(transcript);

        List<Exon> exons = transcriptService.getSortedExons(transcript,true)
        Integer prevExonFmin = null;
        Integer prevExonFmax = null;
        for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext(); ) {
            Exon e = iter.next();
            if (e.getUniqueName().equals(exon.getUniqueName())) {
                if (iter.hasPrevious()) {
                    iter.previous();
                    if (iter.hasPrevious()) {
                        Exon e2 = iter.previous();
                        prevExonFmin = e2.getFmin();
                        prevExonFmax = e2.getFmax();
                    }
                }
                break;
            }
        }
        int coordinate = exon.isNegativeStrand() ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmax() + 2) : featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmin() - 3);
        String residues = sequenceService.getResiduesFromFeature(gene)
        while (coordinate >= 0) {
            int c = featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate);
            if (prevExonFmin != null && (c >= prevExonFmin && c <= prevExonFmax - 2)) {
                throw new AnnotationException("Cannot set to upstream acceptor - will overlap previous exon");
            }
            String seq = residues.substring(coordinate, coordinate + 2);
            if (SequenceTranslationHandler.getSpliceAcceptorSites().contains(seq)) {
                if (exon.getStrand() == -1) {
                    setExonBoundaries(exon, exon.getFmin(), featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate) - 1);
                } else {
                    setExonBoundaries(exon, featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate) + 2, exon.getFmax());
                }
                return;
            }
            --coordinate;
        }

    }

    @Transactional
    def setToDownstreamAcceptor(Exon exon) {
        log.debug "setting downstream acceptor: ${exon}"
        Transcript transcript = getTranscript(exon);
        Gene gene = transcriptService.getGene(transcript);
        int coordinate = exon.isNegativeStrand() ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmax()) : featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmin());
        int exonEnd = exon.isNegativeStrand() ? featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmin()) : featureService.convertSourceCoordinateToLocalCoordinate(gene,exon.getFmax()) - 1;
        String residues = sequenceService.getResiduesFromFeature(gene);
        while (coordinate < residues.length()) {
            if (coordinate >= exonEnd) {
                throw new AnnotationException("Cannot set to downstream acceptor - will remove exon");
            }
            String seq = residues.substring(coordinate, coordinate + 2);
            if (SequenceTranslationHandler.getSpliceAcceptorSites().contains(seq)) {
                if (exon.getStrand() == -1) {
                    setExonBoundaries(exon, exon.getFmin(), featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate) - 1);
                } else {
                    setExonBoundaries(exon, featureService.convertLocalCoordinateToSourceCoordinate(gene,coordinate) + 2, exon.getFmax());
                }
                return;
            }
            ++coordinate;
        }

    }

    @Transactional
    Exon splitExon(Exon leftExon, int newLeftMax, int newRightMin, Assemblage assemblage) {

        // we want to get the right-most permissible feature Location
        FeatureLocation leftFeatureLocation
//        = leftExon.getFeatureLocation()
        leftExon.featureLocations.sort(){it.rank}.each {
            if(newLeftMax < it.fmax && newLeftMax > it.fmin){
                leftFeatureLocation = it
            }
        }

        if(leftFeatureLocation==null){
            throw new AnnotationException("Unable to find an existing feature location to split ${leftExon} at ${newLeftMax}.")
        }

        String uniqueName = nameService.generateUniqueName(leftExon)
        Exon rightExon = new Exon(
                uniqueName: uniqueName
                ,name: uniqueName
                ,isAnalysis: leftExon.isAnalysis
                ,isObsolete: leftExon.isObsolete
        ).save(insert:true)

        
        FeatureLocation rightFeatureLocation = new FeatureLocation(
                feature: rightExon
                ,fmin: leftFeatureLocation.fmin
                ,isFminPartial: leftFeatureLocation.isFminPartial
                ,fmax: leftFeatureLocation.fmax
                ,isFmaxPartial: leftFeatureLocation.isFmaxPartial
                ,strand: leftFeatureLocation.strand
                ,phase: leftFeatureLocation.phase
                ,residueInfo: leftFeatureLocation.residueInfo
                ,locgroup: leftFeatureLocation.locgroup
                ,rank: leftFeatureLocation.rank
                ,sequence: leftFeatureLocation.sequence
        ).save(insert:true)
        rightExon.addToFeatureLocations(rightFeatureLocation)

        leftFeatureLocation.fmax = newLeftMax
        rightFeatureLocation.fmin = newRightMin
        
        leftFeatureLocation.save()
        rightFeatureLocation.save()

        Transcript transcript = getTranscript(leftExon)
        transcriptService.addExon(transcript,rightExon,true,assemblage)

        transcript.save()
        rightExon.save()

        return rightExon

    }

    String getCodingSequenceInPhase(Exon exon, boolean removePartialCodons) {
        Transcript transcript = getTranscript(exon)
        CDS cds = transcriptService.getCDS(transcript)
        if (cds == null || !overlapperService.overlaps(exon, cds, true)) {
            return ""
        }

        String residues = sequenceService.getGenomicResiduesFromSequenceWithAlterations(
                assemblageService.generateAssemblageForFeature(exon)
                ,exon.fmin < cds.fmin ? cds.fmin : exon.fmin
                ,exon.fmax > cds.fmax ? cds.fmax : exon.fmax
                ,Strand.getStrandForValue(exon.strand)
        )

        ArrayList <Exon> exons = transcriptService.getSortedExons(transcript,false)
        if (exon.strand == Strand.NEGATIVE.value) {
            Collections.reverse(exons)
        }

        int phase = getPhaseForExon(exon)
        if (removePartialCodons) {
            residues = residues.substring(phase)
            residues = residues.substring(0, residues.length() - (residues.length() % 3))
        }
        return residues
    }

    int getPhaseForExon(Exon exon) {
        int phase = 0
        Transcript transcript = getTranscript(exon)
        CDS cds = transcriptService.getCDS(transcript)
        if (cds == null || !overlapperService.overlaps(exon, cds, true)) {
            return phase
        }

        ArrayList <Exon> exons = transcriptService.getSortedExons(transcript,false)
        if (exon.strand == Strand.NEGATIVE.value) {
            Collections.reverse(exons)
        }

        int length = getLengthOfPreviousExons(exons, exon, cds)
        phase = length % 3 == 0 ? 0 : 3 - (length % 3)
        return phase
    }

    int getLengthOfPreviousExons(ArrayList<Exon> exons, Exon exon, CDS cds) {
        int length = 0
        for (Exon e in exons) {
            if (e.equals(exon)) {
                break
            }
            if (!overlapperService.overlaps(e, cds, true)) {
                continue
            }
            int fmin = e.fmin < cds.fmin ? cds.fmin : e.fmin
            int fmax = e.fmax > cds.fmax ? cds.fmax : e.fmax
            length += fmin < fmax ? fmax - fmin : fmin - fmax
        }
        return length
    }
}
