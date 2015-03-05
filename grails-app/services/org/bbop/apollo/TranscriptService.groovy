package org.bbop.apollo

import grails.transaction.Transactional
//import grails.compiler.GrailsCompileStatic


//@GrailsCompileStatic
@Transactional
class TranscriptService {

    List<String> ontologyIds = [Transcript.ontologyId, SnRNA.ontologyId, MRNA.ontologyId, SnoRNA.ontologyId, MiRNA.ontologyId, TRNA.ontologyId, NcRNA.ontologyId, RRNA.ontologyId]
    FeatureService featureService
    FeatureRelationshipService featureRelationshipService
    ExonService exonService
    NameService nameService

    /** Retrieve the CDS associated with this transcript.  Uses the configuration to determine
     *  which child is a CDS.  The CDS object is generated on the fly.  Returns <code>null</code>
     *  if no CDS is associated.
     *
     * @return CDS associated with this transcript
     */
    public CDS getCDS(Transcript transcript) {

        return (CDS) featureRelationshipService.getChildForFeature(transcript,CDS.ontologyId)

    }

    /** Retrieve all the exons associated with this transcript.  Uses the configuration to determine
     *  which children are exons.  Exon objects are generated on the fly.  The collection
     *  will be empty if there are no exons associated with the transcript.
     *
     * @return Collection of exons associated with this transcript
     */
    public Collection<Exon> getExons(Transcript transcript) {
        return (Collection<Exon>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript,Exon.ontologyId)
    }

    public Collection<Exon> getSortedExons(Transcript transcript) {
        Collection<Exon> exons = getExons(transcript)
        List<Exon> sortedExons = new LinkedList<Exon>(exons);
        Collections.sort(sortedExons, new FeaturePositionComparator<Exon>(false))
        return sortedExons
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

    public Pseudogene getPseudogene(Transcript transcript) {
        return (Pseudogene) featureRelationshipService.getParentForFeature(transcript,Pseudogene.ontologyId)
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
        ).save(failOnError: true)

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


    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *
     * @param transcript - Transcript to be deleted
     */
    public void deleteTranscript(Gene gene, Transcript transcript) {
        featureRelationshipService.deleteChildrenForTypes(gene,ontologyIds as String[])
        featureRelationshipService.deleteParentForTypes(transcript,Gene.ontologyId,Pseudogene.ontologyId)

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


    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts(Gene gene) {
        return (Collection<Transcript>) featureRelationshipService.getChildrenForFeatureAndTypes(gene,ontologyIds as String[])
    }


    List<Transcript> getTranscriptsSortedByFeatureLocation(Gene gene,boolean sortByStrand) {
        return getTranscripts(gene).sort(true,new FeaturePositionComparator<Transcript>(sortByStrand))
    }

    public void setFmin(Transcript transcript, Integer fmin) {
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
        return featureRelationshipService.getFeaturePropertyForTypes(transcript,frameShiftOntologyIds)
    }

    /** Set the CDS associated with this transcript.  Uses the configuration to determine
     *  the default term to use for CDS features.
     *
     * @param cds - CDS to be set to this transcript
     */
    public void setCDS(Feature feature,CDS cds,boolean replace = true) {
        if(replace){
            println "replacing CDS on feature"
            if(featureRelationshipService.setChildForType(feature,cds)){
                println "returning "
                return
            }
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
        feature.addToParentFeatureRelationships(fr)
        cds.addToChildFeatureRelationships(fr)

        cds.save()
        feature.save(flush: true)
    }

    def addExon(Transcript transcript, Exon exon) {

        if (exon.getFeatureLocation().getFmin() < transcript.getFeatureLocation().getFmin()) {
            transcript.getFeatureLocation().setFmin(exon.getFeatureLocation().getFmin());
        }
        if (exon.getFeatureLocation().getFmax() > transcript.getFeatureLocation().getFmax()) {
            transcript.getFeatureLocation().setFmax(exon.getFeatureLocation().getFmax());
        }
        transcript.save()

        // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
        Gene gene = getGene(transcript)
        if (gene) {
            if (transcript.featureLocation.getFmin() < gene.featureLocation.getFmin()) {
                gene.featureLocation.setFmin(transcript.featureLocation.getFmin());
            }
            if (transcript.featureLocation.getFmax() > gene.featureLocation.getFmax()) {
                gene.featureLocation.setFmax(transcript.getFmax());
            }
        }
        gene.save()

        int initialSize = transcript.parentFeatureRelationships?.size()
        println "initial size: ${initialSize}" // 3
        featureRelationshipService.addChildFeature(transcript,exon,false)
        int finalSize = transcript.parentFeatureRelationships?.size()
        println "final size: ${finalSize}" // 4 (+1 exon)


        featureService.removeExonOverlapsAndAdjacencies(transcript)
        println "post remove exons: ${transcript.parentFeatureRelationships?.size()}" // 6 (+2 splice sites)
//
//        // if the exon is removed during a merge, then we will get a null-pointer
        updateGeneBoundaries(transcript);  // 6, moved transcript fmin, fmax
        println "post update gene boundaries: ${transcript.parentFeatureRelationships?.size()}"
    }

    Transcript getParentTranscriptForFeature(Feature feature) {
        return (Transcript) featureRelationshipService.getParentForFeature(feature,ontologyIds as String[])
    }

    Transcript splitTranscript(Transcript transcript, Exon leftExon, Exon rightExon) {
//        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons());
        List<Exon> exons = exonService.getSortedExons(transcript)
//        Transcript splitTranscript = (Transcript) transcript.cloneFeature(splitTranscriptUniqueName);
        Transcript splitTranscript = new Transcript( transcript.properties )
        splitTranscript.uniqueName = nameService.generateUniqueName()


//        if (transcript.getGene() != null) {
//            addTranscript(transcript.getGene(), splitTranscript);
//        } else {
//            addFeature(splitTranscript);
//        }
        Gene gene = getGene(transcrip)
        if(gene){
            featureService.addTranscriptToGene(gene,splitTranscript)
        }
        else{
            featureService.addFeature(splitTranscript)
        }
      
        FeatureLocation transcriptFeatureLocation = transcript.featureLocation
//        transcript.setFmax(leftExon.getFmax());
        transcriptFeatureLocation.fmax = leftExon.fmax
//        splitTranscript.setFmin(rightExon.getFmin());
        FeatureLocation splitFeatureLocation = splitTranscript.featureLocation
        splitFeatureLocation.fmin = rightExon.fmin
        for (Exon exon : exons) {
            FeatureLocation exonFeatureLocation = exon.featureLocation
            FeatureLocation leftFeatureLoocaiton = leftExon.featureLocation
            if (exonFeatureLocation.fmin > leftFeatureLoocaiton.getFmin()) {
                exonService.deleteExon(transcript, exon);
                if (exon.equals(rightExon)) {
                    addExon(splitTranscript, rightExon);
                } else {
                    addExon(splitTranscript, exon);
                }
            }
        }
        
        return splitTranscript
    }

    /**
     * Duplicate a transcript.  Adds it to the parent gene if it is set.
     *
     * @param transcript - Transcript to be duplicated
     */
    public Transcript duplicateTranscript(Transcript transcript) {
        Transcript duplicate = (Transcript) transcript.generateClone(transcript);
        duplicate.name = transcript.name+"-copy"
        duplicate.uniqueName = nameService.generateUniqueName(transcript)
       
        Gene gene =  getGene(transcript)
        if (gene) {
            featureService.addTranscriptToGene(gene,duplicate)
            gene.save()
        }
        // copy exons
        for (Exon exon : getExons(transcript)) {
            Exon duplicateExon = (Exon) exon.generateClone()
            duplicateExon.name = exon.name + "-copy"
            duplicateExon.uniqueName = nameService.generateUniqueName(duplicateExon)
            addExon(duplicate,duplicateExon)
        }
        // copy CDS
        CDS cds = getCDS(transcript)
        if (cds) {
            CDS duplicateCDS = (CDS) cds.generateClone()
            duplicateCDS.name = cds.name + "-copy"
            duplicateCDS.uniqueName = nameService.generateUniqueName(duplicateCDS)
            setCDS(duplicate,cds)
        }


        duplicate.save()
        
        return duplicate
    }

    def mergeTranscripts(Transcript transcript1, Transcript transcript2) {
        // Merging transcripts basically boils down to moving all exons from one transcript to the other
        
        for (Exon exon : getExons(transcript2)) {
            exonService.deleteExon(transcript2,exon)
            addExon(transcript1,exon)
        }
        transcript1.save()
        Gene gene1 = getGene(transcript1)
        Gene gene2 = getGene(transcript2)
        if (gene1) {
            gene1.save()
        }
        // if the parent genes aren't the same, this leads to a merge of the genes
        if (gene1 && gene2) {
            if (gene1!=gene2) {
                List<Transcript> gene2Transcripts = getTranscripts(gene2)
                for (Transcript transcript : gene2Transcripts) {
                    if (transcript!=transcript2) {
                        deleteTranscript(gene2,transcript)
                        featureService.addTranscriptToGene(gene1,transcript)
                    }
                }
                featureService.deleteFeature(gene2)
            }
        }
        // Delete the empty transcript from the gene
        if (gene2) {
            deleteTranscript(gene2, transcript2);
        } else {
            featureService.deleteFeature(transcript2);
        }
        featureService.removeExonOverlapsAndAdjacencies(transcript1);
    }
}
