package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional
import grails.compiler.GrailsCompileStatic
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.Strand

//@GrailsCompileStatic
@Transactional
class NonCanonicalSplitSiteService {

//    CvTermService cvTermService
    def featureRelationshipService
    def exonService
    def featureService
    def nameService

    /** Delete an non canonical 5' splice site.  Deletes both the transcript -> non canonical 5' splice site and
     *  non canonical 5' splice site -> transcript relationships.
     *
     * @param nonCanonicalFivePrimeSpliceSite - NonCanonicalFivePrimeSpliceSite to be deleted
     */
    public void deleteNonCanonicalFivePrimeSpliceSite(Transcript transcript, NonCanonicalFivePrimeSpliceSite nonCanonicalFivePrimeSpliceSite) {
//        CVTerm partOfCvterms = cvTermService.partOf
//        CVTerm nonCanonicalFivePrimeSpliceSiteCvterms = cvTermService.getTerm(FeatureStringEnum.NONCANONICALFIVEPRIMESPLICESITE)
//        CVTerm transcriptCvTerm = cvTermService.transcript
//        Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalFivePrimeSpliceSite");

        featureRelationshipService.deleteChildrenForTypes(transcript,NonCanonicalFivePrimeSpliceSite.ontologyId)
        featureRelationshipService.deleteParentForTypes(nonCanonicalFivePrimeSpliceSite,Transcript.ontologyId)
        nonCanonicalFivePrimeSpliceSite.delete()

//        // delete transcript -> non canonical 5' splice site child relationship
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCvterms == fr.type
//                    && nonCanonicalFivePrimeSpliceSiteCvterms == fr.childFeature.type
//                    && fr.getSubjectFeature().equals(nonCanonicalFivePrimeSpliceSite)) {
//                boolean ok = transcript.getChildFeatureRelationships().remove(fr);
////                break;
//            }
//        }
//
//
//        // delete transcript -> non canonical 5' splice site parent relationship
//        for (FeatureRelationship fr : nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships()) {
//            if (partOfCvterms == fr.type
//                    && transcriptCvTerm == fr.parentFeature.type
//                    && fr.childFeature == nonCanonicalFivePrimeSpliceSite) {
//                boolean ok = nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships().remove(fr);
//            }
//        }

    }

    public void deleteNonCanonicalThreePrimeSpliceSite(Transcript transcript, NonCanonicalThreePrimeSpliceSite nonCanonicalThreePrimeSpliceSite) {
//        CVTerm partOfCvterms = cvTermService.partOf
//        CVTerm nonCanonicalFivePrimeSpliceSiteCvterms = cvTermService.getTerm(FeatureStringEnum.NONCANONICALFIVEPRIMESPLICESITE)
//        CVTerm transcriptCvTerm = cvTermService.transcript
//        Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalFivePrimeSpliceSite");

        featureRelationshipService.deleteChildrenForTypes(transcript,NonCanonicalThreePrimeSpliceSite.ontologyId)
        featureRelationshipService.deleteParentForTypes(nonCanonicalThreePrimeSpliceSite,Transcript.ontologyId)
        nonCanonicalThreePrimeSpliceSite.delete()

//        // delete transcript -> non canonical 5' splice site child relationship
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCvterms == fr.type
//                    && nonCanonicalFivePrimeSpliceSiteCvterms == fr.childFeature.type
//                    && fr.getSubjectFeature().equals(nonCanonicalFivePrimeSpliceSite)) {
//                boolean ok = transcript.getChildFeatureRelationships().remove(fr);
////                break;
//            }
//        }
//
//
//        // delete transcript -> non canonical 5' splice site parent relationship
//        for (FeatureRelationship fr : nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships()) {
//            if (partOfCvterms == fr.type
//                    && transcriptCvTerm == fr.parentFeature.type
//                    && fr.childFeature == nonCanonicalFivePrimeSpliceSite) {
//                boolean ok = nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships().remove(fr);
//            }
//        }

    }

    /** Delete all non canonical 5' splice site.  Deletes all transcript -> non canonical 5' splice sites and
     *  non canonical 5' splice sites -> transcript relationships.
     *
     */
    public void deleteAllNonCanonicalFivePrimeSpliceSites(Transcript transcript) {
        for (NonCanonicalFivePrimeSpliceSite spliceSite : getNonCanonicalFivePrimeSpliceSites(transcript)) {
            deleteNonCanonicalFivePrimeSpliceSite(transcript,spliceSite);
        }
    }

    /** Retrieve all the non canonical 5' splice sites associated with this transcript.  Uses the configuration to determine
     *  which children are non canonical 5' splice sites.  Non canonical 5' splice site objects are generated on the fly.
     *  The collection will be empty if there are no non canonical 5' splice sites associated with the transcript.
     *
     * @return Collection of non canonical 5' splice sites associated with this transcript
     */
    public Collection<NonCanonicalFivePrimeSpliceSite> getNonCanonicalFivePrimeSpliceSites(Transcript transcript) {
        return (Collection<NonCanonicalFivePrimeSpliceSite>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript,NonCanonicalFivePrimeSpliceSite.ontologyId)
    }

    /** Retrieve all the non canonical 3' splice sites associated with this transcript.  Uses the configuration to determine
     *  which children are non canonical 3' splice sites.  Non canonical 3' splice site objects are generated on the fly.
     *  The collection will be empty if there are no non canonical 3' splice sites associated with the transcript.
     *
     * @return Collection of non canonical 3' splice sites associated with this transcript
     */
    public Collection<NonCanonicalThreePrimeSpliceSite> getNonCanonicalThreePrimeSpliceSites(Transcript transcript) {
//        return (Collection<NonCanonicalThreePrimeSpliceSite>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript,FeatureStringEnum.NONCANONICALTHREEPRIMESPLICESITE)
        return (Collection<NonCanonicalThreePrimeSpliceSite>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript,NonCanonicalThreePrimeSpliceSite.ontologyId)
    }

    /** Delete all non canonical 3' splice site.  Deletes all transcript -> non canonical 3' splice sites and
     *  non canonical 3' splice sites -> transcript relationships.
     *
     */
    public void deleteAllNonCanonicalThreePrimeSpliceSites(Transcript transcript) {
        for (NonCanonicalThreePrimeSpliceSite spliceSite : getNonCanonicalThreePrimeSpliceSites(transcript)) {
//            featureRelationshipService.deleteRelationships(transcript,NonCanonicalThreePrimeSpliceSite.ontologyId,Transcript.ontologyId)
            deleteNonCanonicalThreePrimeSpliceSite(transcript,spliceSite)
        }
    }

    public void findNonCanonicalAcceptorDonorSpliceSites(Transcript transcript) {

        transcript.attach()

        deleteAllNonCanonicalFivePrimeSpliceSites(transcript)
        deleteAllNonCanonicalThreePrimeSpliceSites(transcript)

        List<Exon> exons = exonService.getSortedExons(transcript)
        int fmin=transcript.getFeatureLocation().fmin
        int fmax=transcript.getFeatureLocation().fmax

        int exonNum = 0;
        int sequenceLength = transcript.getFeatureLocation().getSequence().getLength()
        String residues = featureService.getResiduesWithAlterations(createFlankingRegion(transcript, transcript.fmin, transcript.fmax));
        int length=residues.length()

        log.debug "${residues} ${length} ${sequenceLength}"
        for (Exon exon : exons) {
            int fivePrimeSpliceSitePosition = -1;
            int threePrimeSpliceSitePosition = -1;
            boolean validFivePrimeSplice = false;
            boolean validThreePrimeSplice = false;
            for (String donor : SequenceTranslationHandler.getSpliceDonorSites()){
                for (String acceptor : SequenceTranslationHandler.getSpliceAcceptorSites()){
                    log.debug "${exon.fmin} ${exon.fmax}"

                    int local1=exon.fmin-donor.length()-transcript.fmin
                    int local2=exon.fmin-transcript.fmin
                    int local3=exon.fmax-transcript.fmin
                    int local4=exon.fmax+donor.length()-transcript.fmin

                    log.debug "${local1} ${local2} ${local3} ${local4}"

                   // FlankingRegion spliceAcceptorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmin() - donor.length(), exon.getFmin());
                   // FlankingRegion spliceDonorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmax(), exon.getFmax() + donor.length());
                    if (exon.featureLocation.getStrand() == -1) {

                       // FlankingRegion tmp = spliceAcceptorSiteFlankingRegion;
                       // spliceAcceptorSiteFlankingRegion = spliceDonorSiteFlankingRegion;
                       // spliceDonorSiteFlankingRegion = tmp;
                        int tmp1=local1
                        int tmp2=local2
                        local1=local3
                        local2=local4
                        local3=tmp1
                        local4=tmp2
                    }
                    if(local1>=0) {
                        log.debug "${local1} ${local2} ${residues.length()}"
                        String donorSpliceSiteSequence = residues.substring(local1,local2)
                        log.debug "donor ${donorSpliceSiteSequence}"
                        if(donorSpliceSiteSequence==donor)
                            validFivePrimeSplice=true
                        else
                            fivePrimeSpliceSitePosition = exon.getStrand() == -1 ? local1 : local2;
                    }

                    if(local4<length) {
                        log.debug "${local3} ${local3} ${residues.length()}"
                        String acceptorSpliceSiteSequence = residues.substring(local3,local4)
                        log.debug "acceptor ${acceptorSpliceSiteSequence}"
                        if(acceptorSpliceSiteSequence==acceptor)
                            validThreePrimeSplice=true
                        else
                            threePrimeSpliceSitePosition = exon.getStrand() == -1 ? local3 : local4;
                    }
                }
            }
            if (!validFivePrimeSplice && fivePrimeSpliceSitePosition != -1) {
                log.debug "adding a noncanonical five prime splice site at ${fivePrimeSpliceSitePosition}"
                addNonCanonicalFivePrimeSpliceSite(transcript,createNonCanonicalFivePrimeSpliceSite(transcript, fivePrimeSpliceSitePosition));
            }
            if (!validThreePrimeSplice && threePrimeSpliceSitePosition != -1) {
                log.debug "adding a noncanonical three prime splice site at ${threePrimeSpliceSitePosition}"
                addNonCanonicalThreePrimeSpliceSite(transcript,createNonCanonicalThreePrimeSpliceSite(transcript, threePrimeSpliceSitePosition));
            }
        }

        for (NonCanonicalFivePrimeSpliceSite spliceSite : getNonCanonicalFivePrimeSpliceSites(transcript)) {
            if (spliceSite.getDateCreated() == null) {
                spliceSite.setDateCreated(new Date());
            }
            spliceSite.setLastUpdated(new Date());
        }
        for (NonCanonicalThreePrimeSpliceSite spliceSite : getNonCanonicalThreePrimeSpliceSites(transcript)) {
            if (spliceSite.getDateCreated() == null) {
                spliceSite.setDateCreated(new Date());
            }
            spliceSite.setLastUpdated(new Date());
        }
    }

    /** Add a non canonical 5' splice site.  Sets the splice site's transcript to this transcript object.
     *
     * @param nonCanonicalFivePrimeSpliceSite - Non canonical 5' splice site to be added
     */
    public void addNonCanonicalFivePrimeSpliceSite(Transcript transcript,NonCanonicalFivePrimeSpliceSite nonCanonicalFivePrimeSpliceSite) {
//        CVTerm partOfCvterm = cvTermService.partOf

        // add non canonical 5' splice site
        FeatureRelationship fr = new FeatureRelationship(
//                type: cvTermService.partOf
                parentFeature: transcript
                , childFeature: nonCanonicalFivePrimeSpliceSite
                ,rank:0 // TODO: Do we need to rank the order of any other transcripts?
        ).save();
        transcript.addToParentFeatureRelationships(fr);
        nonCanonicalFivePrimeSpliceSite.addToChildFeatureRelationships(fr);
    }

    /** Add a non canonical 3' splice site.  Sets the splice site's transcript to this transcript object.
     *
     * @param nonCanonicalThreePrimeSpliceSite - Non canonical 3' splice site to be added
     */
    public void addNonCanonicalThreePrimeSpliceSite(Transcript transcript,NonCanonicalThreePrimeSpliceSite nonCanonicalThreePrimeSpliceSite) {

        // add non canonical 3' splice site
        FeatureRelationship fr = new FeatureRelationship(
//                type: cvTermService.partOf
                parentFeature: transcript
                , childFeature: nonCanonicalThreePrimeSpliceSite
                ,rank:0 // TODO: Do we need to rank the order of any other transcripts?
        ).save();
        transcript.addToParentFeatureRelationships(fr);
        nonCanonicalThreePrimeSpliceSite.addToChildFeatureRelationships(fr);
    }

    private NonCanonicalFivePrimeSpliceSite createNonCanonicalFivePrimeSpliceSite(Transcript transcript, int position) {
        String uniqueName = transcript.getUniqueName() + "-non_canonical_five_prive_splice_site-" + position;
        NonCanonicalFivePrimeSpliceSite spliceSite = new NonCanonicalFivePrimeSpliceSite(
                uniqueName: uniqueName
                ,isAnalysis: transcript.isAnalysis
                ,isObsolete: transcript.isObsolete
                ,name: uniqueName
//                ,timeAccessioned: new Date()
                ).save()
        spliceSite.addToFeatureLocations(new FeatureLocation(
                strand: transcript.strand
                ,sequence: transcript.featureLocation.sequence
                ,fmin: position
                ,fmax: position
                ,feature: spliceSite
        ).save());
//        spliceSite.featureLocation.setStrand(transcript.getStrand());
//        spliceSite.getFeatureLocation().setSourceFeature(transcript.getFeatureLocation().getSourceFeature());
//        spliceSite.featureLocation.setFmin(position);
//        spliceSite.featureLocation.setFmax(position);
//        spliceSite.setLastUpdated(new Date());
        return spliceSite;
    }


    private NonCanonicalThreePrimeSpliceSite createNonCanonicalThreePrimeSpliceSite(Transcript transcript, int position) {
        String uniqueName = transcript.getUniqueName() + "-non_canonical_three_prive_splice_site-" + position;
        NonCanonicalThreePrimeSpliceSite spliceSite = new NonCanonicalThreePrimeSpliceSite(
                uniqueName: uniqueName
                ,name: uniqueName
                ,isAnalysis: transcript.isAnalysis
                ,isObsolete: transcript.isObsolete
//                ,timeAccessioned: new Date()
        ).save()
        spliceSite.addToFeatureLocations(new FeatureLocation(
                strand: transcript.strand
                ,sequence: transcript.featureLocation.sequence
                ,fmin: position
                ,fmax: position
                ,feature: spliceSite
        ).save());
//        spliceSite.setFeatureLocation(new FeatureLocation());
//        spliceSite.featureLocation.setStrand(transcript.getStrand());
//        spliceSite.getFeatureLocation().setSourceFeature(transcript.getFeatureLocation().getSourceFeature());
//        spliceSite.featureLocation.setFmin(position);
//        spliceSite.featureLocation.setFmax(position);
//        spliceSite.setLastUpdated(new Date());
        return spliceSite;
    }

    private FlankingRegion createFlankingRegion(Feature feature, int fmin, int fmax) {
        FlankingRegion flankingRegion = new FlankingRegion();
        flankingRegion.setIsAnalysis(false)
        flankingRegion.setIsObsolete(false)
        flankingRegion.setName(nameService.generateUniqueName())
        flankingRegion.setUniqueName(flankingRegion.name)
        flankingRegion.save()

        flankingRegion.addToFeatureLocations(new FeatureLocation(
                strand: feature.strand
                ,sequence: feature.featureLocation.sequence
                ,fmin: fmin
                ,fmax: fmax
                ,feature: flankingRegion
        ).save());
//        flankingRegion.add(new FeatureLocation());
//        flankingRegion.getFeatureLocation().setSourceFeature(feature.getFeatureLocation().getSourceFeature());
//        flankingRegion.featureLocation.setStrand(feature.getStrand());
//        flankingRegion.featureLocation.setFmin(fmin);
//        flankingRegion.featureLocation.setFmax(fmax);
        return flankingRegion;
    }
}
