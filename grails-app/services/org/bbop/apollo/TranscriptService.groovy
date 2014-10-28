package org.bbop.apollo

import grails.transaction.Transactional
import groovy.transform.CompileStatic

@CompileStatic
@Transactional
class TranscriptService {

    def cvTermService

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

    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *
     * @param transcript - Transcript to be deleted
     */
    public void deleteTranscript(Transcript transcript) {
        CVTerm partOfCvterm = CVTerm.findByName("PartOf")
        List<String> geneNameList = ["Gene","Pseudogene"]
        Collection<CVTerm> geneCvterms = CVTerm.findAllByNameInList(geneNameList);
        List<String> transcriptNameList = ["Transcript","SnRNA","MRNA","SnoRNA","MiRNA","TRNA","NcRNA","RRNA"]
        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);

        // delete gene -> transcript child relationship
        for (FeatureRelationship fr : getChildFeatureRelationships()) {
            if (partOfCvterm.name!=(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            if (fr.getSubjectFeature().equals(transcript)) {
                getChildFeatureRelationships().remove(fr);
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
        for (Transcript t : getTranscripts()) {
            if (fmin == null || t.getSingleFeatureLocation().getFmin() < fmin) {
                fmin = t.getSingleFeatureLocation().getFmin();
            }
            if (fmax == null || t.getSingleFeatureLocation().getFmax() > fmax) {
                fmax = t.getSingleFeatureLocation().getFmax();
            }
        }
        if (fmin != null) {
            featureLocation.setFmin(fmin);
        }
        if (fmax != null) {
            featureLocation.setFmax(fmax);
        }

    }

    /** Get the number of transcripts.
     *
     * @return Number of transcripts
     */
    public int getNumberOfTranscripts() {
        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");
        int numTranscripts = 0;

        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            ++numTranscripts;
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

}
