package org.bbop.apollo

import grails.transaction.Transactional
import grails.compiler.GrailsCompileStatic
import org.bbop.apollo.editor.AnnotationEditor

@GrailsCompileStatic
@Transactional
class ExonService {

//    CvTermService cvTermService
    TranscriptService transcriptService
    FeatureService featureService
    FeatureRelationshipService featureRelationshipService

    /** Retrieve the transcript that this exon is associated with.  Uses the configuration to
     * determine which parent is a transcript.  The transcript object is generated on the fly.  Returns
     * <code>null</code> if this exon is not associated with any transcript.
     *
     * @return Transcript that this Exon is associated with
     */
    public Transcript getTranscript(Exon exon) {

//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm transcriptCvTerm = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT.value)

        return (Transcript) featureRelationshipService.getParentForFeature(exon,Transcript.ontologyId)
//        for (FeatureRelationship fr : exon.getParentFeatureRelationships()) {
//            if (partOfCvTerm == fr.getType() && transcriptCvTerm == fr.getObjectFeature().getType()) {
//                return (Transcript) fr.getObjectFeature()
//            }
//        }
//        return null;
    }

    /**
     * Merge exon1 and exon2.  The "newly" created exon retains exon1's ID.
     *
     * @param exon1 - Exon to be merged to
     * @param exon2 - Exon to be merged with
     * @throws AnnotationException - If exons don't belong to the same transcript or are in separate strands
     */
    public void mergeExons(Exon exon1, Exon exon2) throws AnnotationException {
//        // both exons must be part of the same transcript
//        if (!getTranscript(exon1).equals(getTranscript(exon2))) {
//            throw new AnnotationEditorException("mergeExons(): Exons must have same parent transcript", exon1, exon2);
//        }
        // both exons must be in the same strand
        Transcript transcript = getTranscript(exon1);
        if (!exon1?.featureLocation?.getStrand()?.equals(exon2?.featureLocation?.getStrand())) {
            throw new AnnotationException("mergeExons(): Exons must be in the same strand ${exon1} ${exon2}");
        }
        if (exon1.getFmin() > exon2.getFmin()) {
            setFmin(exon1, exon2.getFmin())
//            exon1.setFmin(exon2.getFmin());
        }
        if (exon1.getFmax() < exon2.getFmax()) {
            setFmax(exon1, exon2.fmax)
//            exon1.setFmax(exon2.getFmax());
        }
        // need to delete exon2 from transcript
        if (getTranscript(exon2) != null) {
            deleteExon(getTranscript(exon2), exon2);
        }
//        setLongestORF(getTranscript(exon1));
        featureService.removeExonOverlapsAndAdjacencies(transcript);

//        Date date = new Date();
//        exon1.setTimeLastModified(date);
//        transcript.setTimeLastModified(date);

        // TODO: event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }


    /**
     * Delete an exon from a transcript.  If there are no exons left on the transcript, the transcript
     * is deleted from the parent gene.
     *
     * @param transcript - Transcript to have the exon deleted from
     * @param exon - Exon to be deleted from the transcript
     */
    public void deleteExon(Transcript transcript, Exon exon) {
//        transcript.deleteExon(exon);
//        transcriptService.removeChildFeature(transcript,exon,FeatureStringEnum.EXON.value)
//        transcriptService.removeParentFeature(transcript,exon,FeatureStringEnum.EXON.value)
        featureRelationshipService.removeFeatureRelationship(transcript,exon)


        // an empty transcript should be removed from gene,  TODO??
//        if (transcript.getNumberOfExons() == 0) {
//            if (transcript.getGene() != null) {
//                deleteTranscript(transcript.getGene(), transcript);
//            }
//            else {
//                deleteFeature(transcript);
//            }
//        }
//        else {
//            setLongestORF(transcript);
//        }
        // update transcript boundaries if necessary
        if (exon.getFmin().equals(transcript.getFmin())) {
            int fmin = Integer.MAX_VALUE;
            for (Exon e : transcriptService.getExons(transcript)) {
                if (e.getFmin() < fmin) {
                    fmin = e.getFmin();
                }
            }
            transcriptService.setFmin(transcript,fmin);
        }
        if (exon.getFmax().equals(transcript.getFmax())) {
            int fmax = Integer.MIN_VALUE;
            for (Exon e : transcriptService.getExons(transcript)) {
                if (e.getFmax() > fmax) {
                    fmax = e.getFmax();
                }
            }
            transcriptService.setFmax(transcript,fmax);
        }
        // update gene boundaries if necessary
        transcriptService.updateGeneBoundaries(transcript);

//        getSession().unindexFeature(exon);
//        getSession().indexFeature(transcript);

//        transcript.setTimeLastModified(new Date());

        // event fire?? TODO: not really active?
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationEditor.AnnotationChangeEvent.Operation.UPDATE);

    }


    public void setFmin(Exon exon, Integer fmin) {
        exon.getFeatureLocation().setFmin(fmin);
        Transcript transcript = getTranscript(exon)
        if (transcript != null && fmin < transcript.getFmin()) {
            transcriptService.setFmin(transcript, fmin);
        }
    }

    public void setFmax(Exon exon, Integer fmax) {
        exon.getFeatureLocation().setFmax(fmax);
        Transcript transcript = getTranscript(exon)
        if (transcript != null && fmax > transcript.getFmax()) {
            transcriptService.setFmax(transcript, fmax)
        }
    }

    /** Set the transcript that this exon is associated with.  Uses the configuration to
     * determine which parent is a transcript.  If the exon is already associated with a transcript,
     * updates that association.  Otherwise, it creates a new association.
     *
     * @param transcript - Transcript that this transcript will be associated with
     */
//    public void setTranscript(Exon exon, Transcript transcript) {
//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm transcriptCvTerm = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT.value)
//
//        featureRelationshipService.setChildForType(transcript,exon)
//
////        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
////            if (partOfCvTerm == fr.getType() && transcriptCvTerm == fr.getObjectFeature().getType()) {
////                fr.setObjectFeature(transcript);
////            }
////            return;
////        }
//
////        FeatureRelationship fr = new FeatureRelationship(
////                type: partOfCvTerm,
////                childFeature: transcript,
////                parentFeature: exon,
////                rank: 0 // TODO: Do we need to rank the order of any other transcripts?
////        );
////        exon.getParentFeatureRelationships().add(fr);
////        transcript.getChildFeatureRelationships().add(fr);
//    }

/**
 * Splits the exon, creating two exons, the left one which starts at exon.getFmin() and ends at
 * newLeftMax and the right one which starts at newRightMin and ends at exon.getFeatureLocation.getFmax().
 *
 * @param exon - Exon to be split
 * @param newLeftMax - Left split exon max
 * @param newRightMin - Right split exon min
 */
//    public Exon splitExon(Exon exon, int newLeftMax, int newRightMin, String splitExonUniqueName) {
//        session.unindexFeature(exon);
//        Exon leftExon = exon;
//        Exon rightExon = new Exon(exon, splitExonUniqueName);
//
////        leftExon.setUniqueName(exon.getUniqueName() + "-left");
//        leftExon.setFmax(newLeftMax);
//        rightExon.setFmin(newRightMin);
//
//        addExon(exon.getTranscript(), rightExon);
//        session.indexFeature(leftExon);
//        session.indexFeature(rightExon);
//
//        // event fire
////        fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationEvent.Operation.UPDATE);
//
//        Date date = new Date();
//        exon.setTimeLastModified(date);
//        rightExon.setTimeAccessioned(date);
//        rightExon.setTimeLastModified(date);
//        exon.getTranscript().setTimeLastModified(date);
//
//        return rightExon;
//    }
//
//    public Exon makeIntron(Exon exon, int genomicPosition, int minimumIntronSize, String splitExonUniqueName) {
//        String sequence = exon.getResidues();
//        int exonPosition = exon.convertSourceCoordinateToLocalCoordinate(genomicPosition);
//        // find donor coordinate
//        String donorSite = null;
//        int donorCoordinate = -1;
//        for (String donor : SequenceUtil.getSpliceDonorSites()) {
//            int coordinate = sequence.substring(0, exonPosition - minimumIntronSize).lastIndexOf(donor);
//            if (coordinate > donorCoordinate) {
//                donorCoordinate = coordinate;
//                donorSite = donor;
//            }
//        }
//        // find acceptor coordinate
//        String acceptorSite = null;
//        int acceptorCoordinate = -1;
//        for (String acceptor : SequenceUtil.getSpliceAcceptorSites()) {
//            int coordinate = sequence.substring(exonPosition + minimumIntronSize, sequence.length()).indexOf(acceptor);
//            if (acceptorCoordinate == -1 || coordinate < acceptorCoordinate) {
//                acceptorCoordinate = coordinate;
//                acceptorSite = acceptor;
//            }
//        }
//        // no donor/acceptor found
//        if (donorCoordinate == -1 || acceptorCoordinate == -1 || (acceptorCoordinate - donorCoordinate) == 1) {
//            //return splitExon(exon, genomicPosition - 1, genomicPosition + 1, splitExonUniqueName);
//            return null;
//        }
//        acceptorCoordinate += exonPosition + minimumIntronSize;
//        if (exon.getStrand().equals(-1)) {
//            int tmp = acceptorCoordinate;
//            acceptorCoordinate = donorCoordinate + 1 - donorSite.length();
//            donorCoordinate = tmp + 1;
//        } else {
//            acceptorCoordinate += acceptorSite.length();
//        }
//        Exon splitExon = splitExon(exon, exon.convertLocalCoordinateToSourceCoordinate(donorCoordinate), exon.convertLocalCoordinateToSourceCoordinate(acceptorCoordinate), splitExonUniqueName);
//        /*
//        if (exon.getLength() == 0) {
//            deleteExon(exon.getTranscript(), exon);
//        }
//        if (splitExon.getLength() == 0) {
//            deleteExon(splitExon.getTranscript(), splitExon);
//        }
//        */
//
//        // event fire
//        fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationChangeEvent.Operation.UPDATE);
//
//        Date date = new Date();
//        exon.setTimeLastModified(date);
//        splitExon.setTimeAccessioned(date);
//        splitExon.setTimeLastModified(date);
//        exon.getTranscript().setTimeLastModified(date);
//
//        return splitExon;
//    }
//
//    /**
//     * Set exon boundaries.
//     *
//     * @param exon - Exon to be modified
//     * @param fmin - New fmin to be set
//     * @param fmax - New fmax to be set
//     */
//    public void setExonBoundaries(Exon exon, int fmin, int fmax) {
//        Transcript transcript = exon.getTranscript();
//        exon.setFmin(fmin);
//        exon.setFmax(fmax);
//        removeExonOverlapsAndAdjacencies(transcript);
//
//        updateGeneBoundaries(exon.getTranscript().getGene());
//
//        session.unindexFeature(transcript);
//        session.indexFeature(transcript);
//
//        Date date = new Date();
//        exon.setTimeLastModified(date);
//        exon.getTranscript().setTimeLastModified(date);
//
//        // event fire
//        fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationChangeEvent.Operation.UPDATE);
//
//    }
//
//    public void setToDownstreamDonor(Exon exon) throws AnnotationEditorException {
//        Transcript transcript = exon.getTranscript();
//        Gene gene = transcript.getGene();
//        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
//        Integer nextExonFmin = null;
//        Integer nextExonFmax = null;
//        for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext();) {
//            Exon e = iter.next();
//            if (e.getUniqueName().equals(exon.getUniqueName())) {
//                if (iter.hasNext()) {
//                    Exon e2 = iter.next();
//                    nextExonFmin = e2.getFmin();
//                    nextExonFmax = e2.getFmax();
//                    break;
//                }
//            }
//        }
//        int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) + 2 : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) + 1;
//        String residues = gene.getResidues();
//        while (coordinate < residues.length()) {
//            int c = gene.convertLocalCoordinateToSourceCoordinate(coordinate);
//            if (nextExonFmin != null && (c >= nextExonFmin && c <= nextExonFmax + 1)) {
//                throw new AnnotationEditorException("Cannot set to downstream donor - will overlap next exon");
//            }
//            String seq = residues.substring(coordinate, coordinate + 2);
//            if (SequenceUtil.getSpliceDonorSites().contains(seq)) {
//                if (exon.getStrand() == -1) {
//                    setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 1, exon.getFmax());
//                } else {
//                    setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate));
//                }
//                return;
//            }
//            ++coordinate;
//        }
//    }
//
//    public void setToUpstreamDonor(Exon exon) throws AnnotationEditorException {
//        Transcript transcript = exon.getTranscript();
//        Gene gene = transcript.getGene();
//        int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1;
//        int exonStart = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1 : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin());
//        String residues = gene.getResidues();
//        while (coordinate > 0) {
//            if (coordinate <= exonStart) {
//                throw new AnnotationEditorException("Cannot set to upstream donor - will remove exon");
//            }
//            String seq = residues.substring(coordinate, coordinate + 2);
//            if (SequenceUtil.getSpliceDonorSites().contains(seq)) {
//                if (exon.getStrand() == -1) {
//                    setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 1, exon.getFmax());
//                } else {
//                    setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate));
//                }
//                return;
//            }
//            --coordinate;
//        }
//    }
//
//    public void setToDownstreamAcceptor(Exon exon) throws AnnotationEditorException {
//        Transcript transcript = exon.getTranscript();
//        Gene gene = transcript.getGene();
//        int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin());
//        int exonEnd = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1;
//        String residues = gene.getResidues();
//        while (coordinate < residues.length()) {
//            if (coordinate >= exonEnd) {
//                throw new AnnotationEditorException("Cannot set to downstream acceptor - will remove exon");
//            }
//            String seq = residues.substring(coordinate, coordinate + 2);
//            if (SequenceUtil.getSpliceAcceptorSites().contains(seq)) {
//                if (exon.getStrand() == -1) {
//                    setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate) - 1);
//                } else {
//                    setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 2, exon.getFmax());
//                }
//                return;
//            }
//            ++coordinate;
//        }
//    }
//
//    public void setToUpstreamAcceptor(Exon exon) throws AnnotationException {
//        Transcript transcript = exon.getTranscript();
//        Gene gene = transcript.getGene();
//        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
//        Integer prevExonFmin = null;
//        Integer prevExonFmax = null;
//        for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext();) {
//            Exon e = iter.next();
//            if (e.getUniqueName().equals(exon.getUniqueName())) {
//                if (iter.hasPrevious()) {
//                    iter.previous();
//                    if (iter.hasPrevious()) {
//                        Exon e2 = iter.previous();
//                        prevExonFmin = e2.getFmin();
//                        prevExonFmax = e2.getFmax();
//                    }
//                }
//                break;
//            }
//        }
//        int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax() + 2) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin() - 3);
//        String residues = gene.getResidues();
//        while (coordinate >= 0) {
//            int c = gene.convertLocalCoordinateToSourceCoordinate(coordinate);
//            if (prevExonFmin != null && (c >= prevExonFmin && c <= prevExonFmax - 2)) {
//                throw new AnnotationEditorException("Cannot set to upstream acceptor - will overlap previous exon");
//            }
//            String seq = residues.substring(coordinate, coordinate + 2);
//            if (SequenceUtil.getSpliceAcceptorSites().contains(seq)) {
//                if (exon.getStrand() == -1) {
//                    setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate) - 1);
//                } else {
//                    setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 2, exon.getFmax());
//                }
//                return;
//            }
//            --coordinate;
//        }
//    }

    List<Exon> getSortedExons(Transcript transcript,boolean sortByStrand = false ) {
        List<Exon> sortedExons= new LinkedList<Exon>(transcriptService.getExons(transcript));
        Collections.sort(sortedExons,new FeaturePositionComparator<Exon>(sortByStrand))
        return sortedExons
    }

}
