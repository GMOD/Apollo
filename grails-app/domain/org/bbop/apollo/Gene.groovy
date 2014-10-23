package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
class Gene extends BiologicalRegion{

    static constraints = {
    }


    String ontologyId = "SO:0000704"// XX:NNNNNNN
    String cvTerm = "Gene"// may have a link

    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts() {
        Collection<Transcript> transcripts = new ArrayList<Transcript>();
//        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> partOfCvterms = CVTerm.findAllByName("PartOf")
//        Collection<CVTerm> transcriptCvterms = conf.getDescendantCVTermsForClass("Transcript");
        List<String> transcriptNameList = ["Transcript","SnRNA","MRNA","SnoRNA","MiRNA","TRNA","NcRNA","RRNA"]
        Collection<CVTerm> transcriptCvterms = CVTerm.findAllByNameInList(transcriptNameList);

        for (FeatureRelationship fr : getChildFeatureRelationships()) {
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

    /** Add a transcript.  If the transcript's bounds are beyond the gene's bounds, the gene's bounds
     *  are adjusted accordingly.  Sets the transcript's gene to this gene object.
     *
     * @param transcript - Transcript to be added
     */
    public void addTranscript(Transcript transcript) {
//        CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");
        Collection<CVTerm> partOfCvterm = CVTerm.findAllByName("PartOf")

        // no feature location, set location to transcript's
        if (getSingleFeatureLocation() == null) {
            setFeatureLocation(new FeatureLocation(transcript.getSingleFeatureLocation()));
        }
        else {
            // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
            if (transcript.getSingleFeatureLocation().getFmin() < getSingleFeatureLocation().getFmin()) {
                getSingleFeatureLocation().setFmin(transcript.getSingleFeatureLocation().getFmin());
            }
            if (transcript.getSingleFeatureLocation().getFmax() > getSingleFeatureLocation().getFmax()) {
                getSingleFeatureLocation().setFmax(transcript.getSingleFeatureLocation().getFmax());
            }
        }

        // add transcript
        int rank = 0;
        //TODO: do we need to figure out the rank?
        FeatureRelationship featureRelationship = new FeatureRelationship(
                type: partOfCvterm.iterator().next()
                ,objectFeature: this
                ,subjectFeature: transcript
                ,rank: rank
        ).save()
        childFeatureRelationships.add(featureRelationship)
//        getChildFeatureRelationships().add(
//                new FeatureRelationship(partOfCvterm, this, transcript, rank));

        // TODO: do I need to set the gene?
//        transcript.setGene(this);
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
}
