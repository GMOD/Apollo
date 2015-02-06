package org.bbop.apollo

import grails.transaction.Transactional
import grails.compiler.GrailsCompileStatic


@GrailsCompileStatic
@Transactional
class TranscriptService {

    List<String> ontologyIds = [Transcript.ontologyId, SnRNA.ontologyId, MRNA.ontologyId, SnoRNA.ontologyId, MiRNA.ontologyId, TRNA.ontologyId, NcRNA.ontologyId, RRNA.ontologyId]
//    CvTermService cvTermService
    FeatureService featureService
    FeatureRelationshipService featureRelationshipService
//    def nonCanonicalSplitSiteService

    /** Retrieve the CDS associated with this transcript.  Uses the configuration to determine
     *  which child is a CDS.  The CDS object is generated on the fly.  Returns <code>null</code>
     *  if no CDS is associated.
     *
     * @return CDS associated with this transcript
     */
    public CDS getCDS(Transcript transcript) {

        return (CDS) featureRelationshipService.getChildForFeature(transcript,CDS.ontologyId)

//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm cdsCvTerm = cvTermService.getTerm(FeatureStringEnum.CDS.value)
//
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCvTerm == fr.getType() && fr.childFeature.type == cdsCvTerm) {
//                return (CDS) fr.getSubjectFeature();
//            }
//        }
//
//
////        CDS.
////        FeatureRelationship. find all children relationships where type is CDS
//
//        return null;
    }

    /** Retrieve all the exons associated with this transcript.  Uses the configuration to determine
     *  which children are exons.  Exon objects are generated on the fly.  The collection
     *  will be empty if there are no exons associated with the transcript.
     *
     * @return Collection of exons associated with this transcript
     */
    public Collection<Exon> getExons(Transcript transcript) {
//        Collection<Exon> exons = new ArrayList<Exon>();
//        CVTerm partOfCVTerm = cvTermService.partOf
//        CVTerm exonCvTerm = cvTermService.getTerm(FeatureStringEnum.EXON.value)
//
//
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCVTerm != fr.getType()) {
//                continue;
//            }
//            if (exonCvTerm != fr.getSubjectFeature().getType()) {
//                continue;
//            }
//            exons.add((Exon) fr.getSubjectFeature())
//        }

        return (Collection<Exon>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript,Exon.ontologyId)
//        return exons;
    }

    /** Retrieve the gene that this transcript is associated with.  Uses the configuration to
     * determine which parent is a gene.  The gene object is generated on the fly.  Returns
     * <code>null</code> if this transcript is not associated with any gene.
     *
     * @return Gene that this Transcript is associated with
     */
    public Gene getGene(Transcript transcript) {

        return (Gene) featureRelationshipService.getParentForFeature(transcript,Gene.ontologyId)
    }


    public boolean isProteinCoding(Transcript transcript) {
        if (getGene(transcript) != null && getGene(transcript) instanceof Pseudogene) {
            return false;
        }
        return true;
    }

    CDS createCDS(Transcript transcript) {
        String uniqueName = transcript.getUniqueName() + FeatureStringEnum.CDS_SUFFIX.value;

        CDS cds = new CDS(
                uniqueName: uniqueName
                , isAnalysis: transcript.isAnalysis
                , isObsolete: transcript.isObsolete
                ,name: uniqueName
//                ,timeAccessioned: new Date()
//                ,timeLastModified: new Date()
        ).save(failOnError: true)

//        CDS cds = new CDS(transcript.getOrganism(), uniqueName, transcript.isAnalysis(),
//                transcript.isObsolete(), null, transcript.getConfiguration());

        FeatureLocation transcriptFeatureLocation = FeatureLocation.findByFeature(transcript)

        FeatureLocation featureLocation = new FeatureLocation(
                strand: transcriptFeatureLocation.strand
                , sequence: transcriptFeatureLocation.sequence
                ,fmin: transcriptFeatureLocation.fmin
                ,fmax: transcriptFeatureLocation.fmax
                , feature: cds
        ).save(insert: true,failOnError: true)
        cds.addToFeatureLocations(featureLocation);
        cds.save(flush: true, insert: true)
        return cds;
    }

//    StopCodonReadThrough getStopCodonReadThrough(Feature feature, Transcript transcript) {
//        CDS cds = getCDS(transcript)
//        if (transcript) {
//            CVTerm partOfCvTerm = cvTermService.partOf
//            CVTerm stopCvTerm = cvTermService.getTerm(FeatureStringEnum.STOP_CODON_READTHROUGH.value)
////            Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
////            Collection<CVTerm> cdsCvterms = conf.getCVTermsForClass("StopCodonReadThrough");
//
//            for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//                if (partOfCvTerm != fr.getType()) {
//                    continue;
//                }
//                if (stopCvTerm != fr.getSubjectFeature().getType()) {
//                    continue;
//                }
//                return (StopCodonReadThrough) fr.childFeature
////                return new StopCodonReadThrough(fr.getSubjectFeature(), conf);
//            }
//            return null;
//        }
//    }

    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *
     * @param transcript - Transcript to be deleted
     */
    public void deleteTranscript(Gene gene, Transcript transcript) {
//        CVTerm partOfCvterm = CVTerm.findByName("PartOf")
//        List<String> geneNameList = ["Gene", "Pseudogene"]
//        Collection<CVTerm> geneCvterms = CVTerm.findAllByNameInList(geneNameList);
//        List<String> transcriptNameList = ["Transcript", "SnRNA", "MRNA", "SnoRNA", "MiRNA", "TRNA", "NcRNA", "RRNA"]
//        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);



        featureRelationshipService.deleteChildrenForTypes(gene,ontologyIds as String[])
        featureRelationshipService.deleteParentForTypes(transcript,Gene.ontologyId,Pseudogene.ontologyId)


//        // delete gene -> transcript child relationship
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCvterm.name != (fr.getType())) {
//                continue;
//            }
//            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
//                continue;
//            }
//            if (fr.getSubjectFeature().equals(transcript)) {
//                transcript.getChildFeatureRelationships().remove(fr);
//                transcript.save(flush: true)
//                break;
//            }
//        }

        // delete gene -> transcript parent relationship
//        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
//            if (partOfCvterm.name != (fr.getType())) {
//                continue;
//            }
//            if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
//                continue;
//            }
//            if (fr.getSubjectFeature().equals(transcript)) {
//                transcript.getParentFeatureRelationships().remove(fr);
//                transcript.save(flush: true)
//                break;
//            }
//        }

        // update bounds
        Integer fmin = null;
        Integer fmax = null;
        for (Transcript t : getTranscripts(gene)) {
            if (fmin == null || t.getSingleFeatureLocation().getFmin() < fmin) {
                fmin = t.getSingleFeatureLocation().getFmin();
            }
            if (fmax == null || t.getSingleFeatureLocation().getFmax() > fmax) {
                fmax = t.getSingleFeatureLocation().getFmax();
            }
        }
        if (fmin != null) {
            setFmin(transcript, fmin)
        }
        if (fmax != null) {
            setFmax(transcript, fmin)
        }

    }

    /** Get the number of transcripts.
     *
     * @return Number of transcripts
     */
//    public int getNumberOfTranscripts(Feature feature) {
//        CVTerm partOfCvterms = cvTermService.partOf
//        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT)
//        int numTranscripts = 0;
//
//        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//            if (partOfCvterms == fr.type && transcriptCvterms == fr.childFeature.type) {
//                ++numTranscripts;
//            }
//
//        }
//        return numTranscripts;
//    }

    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts(Gene gene) {
//        Collection<Transcript> transcripts = new ArrayList<Transcript>();
////        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
//        Collection<CVTerm> partOfCvterms = CVTerm.findAllByName("PartOf")
////        Collection<CVTerm> transcriptCvterms = conf.getDescendantCVTermsForClass("Transcript");
//        List<String> transcriptNameList = ["Transcript", "SnRNA", "MRNA", "SnoRNA", "MiRNA", "TRNA", "NcRNA", "RRNA"]
//        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);


        return (Collection<Transcript>) featureRelationshipService.getChildrenForFeatureAndTypes(gene,ontologyIds as String[])
////        featureRelationshipService.deleteParentForTypes(transcript,Gene.ontologyId,Pseudogene.ontologyId)
//
//        featureRelationshipService.getChildrenForFeatureAndTypes(gene,)
//
//        for (FeatureRelationship fr : gene.getChildFeatureRelationships()) {
//            if (!partOfCvterms.contains(fr.getType())) {
//                continue;
//            }
//            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
//                continue;
//            }
//            transcripts.add((Transcript) fr.getSubjectFeature())
////            transcripts.add((Transcript)BioObjectUtil.createBioObject(fr.getSubjectFeature(), conf));
//        }
//        return transcripts;
    }

    public void setFmin(Transcript transcript, Integer fmin) {
//        super.setFmin(fmin);
        transcript.getFeatureLocation().setFmin(fmin);
        Gene gene = getGene(transcript)
        if (gene != null && fmin < gene.getFmin()) {
            featureService.setFmin(gene, fmin)
        }
    }

    public void setFmax(Transcript transcript, Integer fmax) {
        transcript.getFeatureLocation().setFmax(fmax);
        Gene gene = getGene(transcript)
        if (gene != null && fmax > gene.getFmax()) {
            featureService.setFmax(gene, fmax);
        }
    }

    def updateGeneBoundaries(Transcript transcript) {
        Gene gene = getGene(transcript)
        if (gene == null) {
            return;
        }
        int geneFmax = Integer.MIN_VALUE;
        int geneFmin = Integer.MAX_VALUE;
        for (Transcript t : getTranscripts(gene)) {
            if (t.getFmin() < geneFmin) {
                geneFmin = t.getFmin();
            }
            if (t.getFmax() > geneFmax) {
                geneFmax = t.getFmax();
            }
        }
        featureService.setFmin(gene, geneFmin)
        featureService.setFmax(gene, geneFmax)

        // not sure if we want this if not actually saved
//        gene.setLastUpdated(new Date());
    }

    List<String> getFrameShiftOntologyIds(){
        List<String> frameshiftOntologyIds = new ArrayList<>()

        frameShiftOntologyIds.add(Plus1Frameshift.ontologyId)
        frameShiftOntologyIds.add(Plus2Frameshift.ontologyId)
        frameShiftOntologyIds.add(Minus1Frameshift.ontologyId)
        frameShiftOntologyIds.add(Minus2Frameshift.ontologyId)


        return frameshiftOntologyIds
    }

    List<Frameshift> getFrameshifts(Transcript transcript) {

        List<Frameshift> frameshiftList =  featureRelationshipService.getFeaturePropertyForTypes(transcript,frameShiftOntologyIds)

////        featureRelationshipService
//        Collection<CVTerm> frameshiftCvterms = cvTermService.frameshifts
//
//        for (FeatureProperty featureProperty : transcript.getFeatureProperties()) {
//            if (frameshiftCvterms.contains(featureProperty.getType())) {
//                frameshiftList.add((Frameshift) featureProperty);
//            }
//        }

        return frameshiftList
    }

    /** Set the CDS associated with this transcript.  Uses the configuration to determine
     *  the default term to use for CDS features.
     *
     * @param cds - CDS to be set to this transcript
     */
    public void setCDS(Feature feature,CDS cds,boolean replace = true) {
//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm cdsCvTerm = cvTermService.getTerm(FeatureStringEnum.CDS)


        if(replace){
            if(featureRelationshipService.setChildForType(feature,cds)){
                return
            }
//            for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//                if(partOfCvTerm==fr.type && cdsCvTerm==fr.parentFeature.type){
//                    fr.setSubjectFeature(cds);
//                    return;
//                }
//
//            }
        }

        FeatureRelationship fr = new FeatureRelationship(
//                type:partOfCvTerm
                parentFeature: feature
                , childFeature: cds
                ,rank: 0
        ).save(insert:true,failOnError: true)


        println "fr: ${fr}"
        println "feature: ${feature}"
        println "cds: ${cds}"
        feature.addToChildFeatureRelationships(fr)
        cds.addToParentFeatureRelationships(fr)

        cds.save()
        feature.save(flush: true)
    }

    def addExon(Transcript transcript, Exon exon) {

//        transcript.addExon(exon);
        featureRelationshipService.addChildFeature(transcript,exon)

//        removeExonOverlapsAndAdjacencies(transcript);
        featureService.removeExonOverlapsAndAdjacencies(transcript)
//
//        // if the exon is removed during a merge, then we will get a null-pointer
//        updateGeneBoundaries(transcript.getGene());
        updateGeneBoundaries(transcript);
//
//        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
    }
}
