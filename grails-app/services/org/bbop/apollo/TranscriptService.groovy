package org.bbop.apollo

import grails.transaction.Transactional
import groovy.transform.CompileStatic

@CompileStatic
@Transactional
class TranscriptService {

    def cvTermService
    def featureService
    def nonCanonicalSplitSiteService

    /** Retrieve the CDS associated with this transcript.  Uses the configuration to determine
     *  which child is a CDS.  The CDS object is generated on the fly.  Returns <code>null</code>
     *  if no CDS is associated.
     *
     * @return CDS associated with this transcript
     */
    public CDS getCDS(Transcript transcript) {
        CVTerm partOfCvTerm = cvTermService.partOf
        CVTerm cdsCvTerm = cvTermService.getTerm(FeatureStringEnum.CDS.value)

        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
            if(partOfCvTerm!=fr.getType()){
                continue;
            }
            if (cdsCvTerm!=fr.getSubjectFeature().getType()) {
                continue;
            }
            return (CDS) fr.getSubjectFeature();
        }
        return null;
    }


    /** Retrieve all the exons associated with this transcript.  Uses the configuration to determine
     *  which children are exons.  Exon objects are generated on the fly.  The collection
     *  will be empty if there are no exons associated with the transcript.
     *
     * @return Collection of exons associated with this transcript
     */
    public Collection<Exon> getExons(Transcript transcript) {
        Collection<Exon> exons = new ArrayList<Exon>();
        CVTerm partOfCVTerm = cvTermService.partOf
        CVTerm exonCvTerm= cvTermService.getTerm(FeatureStringEnum.EXON.value)

        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
            if (partOfCVTerm!=fr.getType()) {
                continue;
            }
            if (exonCvTerm!=fr.getSubjectFeature().getType()) {
                continue;
            }
            exons.add((Exon) fr.getSubjectFeature())
        }
        return exons;
    }


    /** Retrieve the gene that this transcript is associated with.  Uses the configuration to
     * determine which parent is a gene.  The gene object is generated on the fly.  Returns
     * <code>null</code> if this transcript is not associated with any gene.
     *
     * @return Gene that this Transcript is associated with
     */
    public Gene getGene(Transcript transcript) {
        CVTerm partOfCvterm = cvTermService.partOf
        CVTerm geneCvterm = cvTermService.getTerm(FeatureStringEnum.GENE.value)
        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
            if (partOfCvterm!=fr.getType()) {
                continue;
            }
            if (geneCvterm!=fr.getObjectFeature().getType()) {
                continue;
            }
//            return ((Gene)BioObjectUtil.createBioObject(fr.getObjectFeature(), conf));
            return (Gene) fr.getObjectFeature()
        }
        return null;
    }


    public boolean isProteinCoding(Transcript transcript) {
        if (getGene(transcript) != null && getGene(transcript) instanceof Pseudogene) {
            return false;
        }
        return true;
    }

    StopCodonReadThrough getStopCodonReadThrough(Feature feature,Transcript transcript) {
        CDS cds = getCDS(transcript)
        if(transcript){
            CVTerm partOfCvTerm = cvTermService.partOf
            CVTerm stopCvTerm = cvTermService.getTerm(FeatureStringEnum.STOP_CODON_READTHROUGH.value)
//            Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
//            Collection<CVTerm> cdsCvterms = conf.getCVTermsForClass("StopCodonReadThrough");

            for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
                if (partOfCvTerm !=fr.getType()) {
                    continue;
                }
                if (stopCvTerm != fr.getSubjectFeature().getType()) {
                    continue;
                }
                return (StopCodonReadThrough) fr.subjectFeature
//                return new StopCodonReadThrough(fr.getSubjectFeature(), conf);
            }
            return null;
        }
    }

    public void removeChildFeature(Transcript transcript,Feature feature,String type){
        CVTerm partOfCvterm = cvTermService.partOf
        CVTerm exonCvterm = cvTermService.getTerm(type);
        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT);

        // delete transcript -> exon child relationship
        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
            if (partOfCvterm == fr.type
                    && exonCvterm == fr.subjectFeature.type
                    && fr.getSubjectFeature().equals(feature)
            ) {
                boolean ok = transcript.getChildFeatureRelationships().remove(fr);
                break;

            }
        }

    }

    public void removeParentFeature(Transcript transcript,Feature feature,String type){
        CVTerm partOfCvterm = cvTermService.partOf
        CVTerm exonCvterm = cvTermService.getTerm(type);
        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT);

        // delete transcript -> exon child relationship
        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
            if (partOfCvterm == fr.type
                    && transcriptCvterms == fr.objectFeature.type
                    && fr.getSubjectFeature().equals(feature)
            ) {
                boolean ok = feature.getParentFeatureRelationships().remove(fr);
                break;

            }
        }

    }

    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *
     * @param transcript - Transcript to be deleted
     */
    public void deleteTranscript(Gene gene,Transcript transcript) {
        CVTerm partOfCvterm = CVTerm.findByName("PartOf")
        List<String> geneNameList = ["Gene","Pseudogene"]
        Collection<CVTerm> geneCvterms = CVTerm.findAllByNameInList(geneNameList);
        List<String> transcriptNameList = ["Transcript","SnRNA","MRNA","SnoRNA","MiRNA","TRNA","NcRNA","RRNA"]
        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);

        // delete gene -> transcript child relationship
        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
            if (partOfCvterm.name!=(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            if (fr.getSubjectFeature().equals(transcript)) {
                transcript.getChildFeatureRelationships().remove(fr);
                transcript.save(flush:true)
                break;
            }
        }

        // delete gene -> transcript parent relationship
        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
            if (partOfCvterm.name!=(fr.getType())) {
                continue;
            }
            if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
                continue;
            }
            if (fr.getSubjectFeature().equals(transcript)) {
                transcript.getParentFeatureRelationships().remove(fr);
                transcript.save(flush:true)
                break;
            }
        }

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
            setFmin(transcript,fmin)
        }
        if (fmax != null) {
            setFmax(transcript,fmin)
        }

    }

    /** Get the number of transcripts.
     *
     * @return Number of transcripts
     */
    public int getNumberOfTranscripts(Feature feature) {
        CVTerm partOfCvterms = cvTermService.partOf
        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT)
        int numTranscripts = 0;

        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if(partOfCvterms==fr.type && transcriptCvterms==fr.subjectFeature.type){
                ++numTranscripts;
            }

        }
        return numTranscripts;
    }

    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts(Gene gene) {
        Collection<Transcript> transcripts = new ArrayList<Transcript>();
//        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> partOfCvterms = CVTerm.findAllByName("PartOf")
//        Collection<CVTerm> transcriptCvterms = conf.getDescendantCVTermsForClass("Transcript");
        List<String> transcriptNameList = ["Transcript","SnRNA","MRNA","SnoRNA","MiRNA","TRNA","NcRNA","RRNA"]
        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);

        for (FeatureRelationship fr : gene.getChildFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            transcripts.add((Transcript) fr.getSubjectFeature())
//            transcripts.add((Transcript)BioObjectUtil.createBioObject(fr.getSubjectFeature(), conf));
        }
        return transcripts;
    }

    @Override
    public void setFmin(Transcript transcript,Integer fmin) {
//        super.setFmin(fmin);
        transcript.getFeatureLocation().setFmin(fmin);
        Gene gene = getGene(transcript)
        if (gene!=null && fmin < gene.getFmin()) {
            featureService.setFmin(gene,fmin)
        }
    }

    @Override
    public void setFmax(Transcript transcript,Integer fmax) {
        transcript.getFeatureLocation().setFmax(fmax);
        Gene gene = getGene(transcript)
        if (gene != null && fmax > gene.getFmax()) {
            featureService.setFmax(gene,fmax);
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
        featureService.setFmin(gene,geneFmin)
        featureService.setFmax(gene,geneFmax)
        gene.setTimeLastModified(new Date());

    }


}
