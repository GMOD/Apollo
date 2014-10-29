package org.bbop.apollo

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.gmod.gbol.util.SequenceUtil

@CompileStatic
@Transactional
class NonCanonicalSplitSiteService {

    def cvTermService
    def featureRelationshipService
    def exonService

    /** Delete an non canonical 5' splice site.  Deletes both the transcript -> non canonical 5' splice site and
     *  non canonical 5' splice site -> transcript relationships.
     *
     * @param nonCanonicalFivePrimeSpliceSite - NonCanonicalFivePrimeSpliceSite to be deleted
     */
    public void deleteNonCanonicalFivePrimeSpliceSite(Transcript transcript, NonCanonicalFivePrimeSpliceSite nonCanonicalFivePrimeSpliceSite) {
        CVTerm partOfCvterms = cvTermService.partOf
        CVTerm nonCanonicalFivePrimeSpliceSiteCvterms = cvTermService.getTerm(FeatureStringEnum.NONCANONICALFIVEPRIMESPLICESITE)
        CVTerm transcriptCvTerm = cvTermService.transcript
//        Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalFivePrimeSpliceSite");

        // delete transcript -> non canonical 5' splice site child relationship
        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
            if (partOfCvterms == fr.type
                    && nonCanonicalFivePrimeSpliceSiteCvterms == fr.subjectFeature.type
                    && fr.getSubjectFeature().equals(nonCanonicalFivePrimeSpliceSite)) {
                boolean ok = transcript.getChildFeatureRelationships().remove(fr);
//                break;
            }
        }

        // delete transcript -> non canonical 5' splice site parent relationship
        for (FeatureRelationship fr : nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships()) {
            if (partOfCvterms == fr.type
                    && transcriptCvTerm == fr.objectFeature.type
                    && fr.subjectFeature == nonCanonicalFivePrimeSpliceSite) {
                boolean ok = nonCanonicalFivePrimeSpliceSite.getParentFeatureRelationships().remove(fr);
            }
        }

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
        return (Collection<NonCanonicalFivePrimeSpliceSite>) featureRelationshipService.getChildrenForFeature(transcript,FeatureStringEnum.NONCANONICALFIVEPRIMESPLICESITE)
    }

    /** Retrieve all the non canonical 3' splice sites associated with this transcript.  Uses the configuration to determine
     *  which children are non canonical 3' splice sites.  Non canonical 3' splice site objects are generated on the fly.
     *  The collection will be empty if there are no non canonical 3' splice sites associated with the transcript.
     *
     * @return Collection of non canonical 3' splice sites associated with this transcript
     */
    public Collection<NonCanonicalThreePrimeSpliceSite> getNonCanonicalThreePrimeSpliceSites(Transcript transcript) {
        return (Collection<NonCanonicalThreePrimeSpliceSite>) featureRelationshipService.getChildrenForFeature(transcript,FeatureStringEnum.NONCANONICALTHREEPRIMESPLICESITE)
    }

    /** Delete all non canonical 3' splice site.  Deletes all transcript -> non canonical 3' splice sites and
     *  non canonical 3' splice sites -> transcript relationships.
     *
     */
    public void deleteAllNonCanonicalThreePrimeSpliceSites(Transcript transcript) {
        for (NonCanonicalThreePrimeSpliceSite spliceSite : getNonCanonicalThreePrimeSpliceSites(transcript)) {
            featureRelationshipService.deleteRelationships(transcript,FeatureStringEnum.NONCANONICALFIVEPRIMESPLICESITE,FeatureStringEnum.TRANSCRIPT)
        }
    }

    public void findNonCanonicalAcceptorDonorSpliceSites(Transcript transcript) {

        deleteAllNonCanonicalFivePrimeSpliceSites(transcript)
        deleteAllNonCanonicalThreePrimeSpliceSites(transcript)

        List<Exon> exons = exonService.getSortedExons(transcript)
        int exonNum = 0;
        int sourceFeatureLength = transcript.getFeatureLocation().getSourceFeature().getSequenceLength();
        for (Exon exon : exons) {
            ++exonNum;
            int fivePrimeSpliceSitePosition = -1;
            int threePrimeSpliceSitePosition = -1;
            boolean validFivePrimeSplice = false;
            boolean validThreePrimeSplice = false;
            for (String donor : SequenceUtil.getSpliceDonorSites()) {
                for (String acceptor : SequenceUtil.getSpliceAcceptorSites()) {
                    FlankingRegion spliceAcceptorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmin() - donor.length(), exon.getFmin());
                    FlankingRegion spliceDonorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmax(), exon.getFmax() + donor.length());
                    if (exon.getStrand() == -1) {
                        FlankingRegion tmp = spliceAcceptorSiteFlankingRegion;
                        spliceAcceptorSiteFlankingRegion = spliceDonorSiteFlankingRegion;
                        spliceDonorSiteFlankingRegion = tmp;
                    }
                    /*
                    String donorSpliceSiteSequence = session.getResiduesWithAlterations(spliceDonorSiteFlankingRegion);
                    String acceptorSpliceSiteSequence = session.getResiduesWithAlterations(spliceAcceptorSiteFlankingRegion);
                    */
                    String donorSpliceSiteSequence = spliceDonorSiteFlankingRegion.getFmin() >= 0 && spliceDonorSiteFlankingRegion.getFmax() <= sourceFeatureLength ?
                            session.getResiduesWithAlterations(spliceDonorSiteFlankingRegion) : null;
                    String acceptorSpliceSiteSequence = spliceAcceptorSiteFlankingRegion.getFmin() >= 0 && spliceAcceptorSiteFlankingRegion.getFmax() <= sourceFeatureLength ?
                            session.getResiduesWithAlterations(spliceAcceptorSiteFlankingRegion) : null;
                    if (exonNum < exons.size()) {
                        if (!validFivePrimeSplice) {
                            if (!donorSpliceSiteSequence.equals(donor)) {
                                fivePrimeSpliceSitePosition = exon.getStrand() == -1 ? spliceDonorSiteFlankingRegion.getFmax() : spliceDonorSiteFlankingRegion.getFmin();
                            } else {
                                validFivePrimeSplice = true;
                            }
                        }
                    }
                    if (exonNum > 1) {
                        if (!validThreePrimeSplice) {
                            if (!acceptorSpliceSiteSequence.equals(acceptor)) {
                                threePrimeSpliceSitePosition = exon.getStrand() == -1 ? spliceAcceptorSiteFlankingRegion.getFmin() : spliceAcceptorSiteFlankingRegion.getFmax();
                            } else {
                                validThreePrimeSplice = true;
                            }
                        }
                    }
                }
            }
            if (!validFivePrimeSplice && fivePrimeSpliceSitePosition != -1) {
                transcript.addNonCanonicalFivePrimeSpliceSite(createNonCanonicalFivePrimeSpliceSite(transcript, fivePrimeSpliceSitePosition));
            }
            if (!validThreePrimeSplice && threePrimeSpliceSitePosition != -1) {
                transcript.addNonCanonicalThreePrimeSpliceSite(createNonCanonicalThreePrimeSpliceSite(transcript, threePrimeSpliceSitePosition));
            }
        }

        transcript.setTimeLastModified(new Date());


//        editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
        for (NonCanonicalFivePrimeSpliceSite spliceSite : transcript.getNonCanonicalFivePrimeSpliceSites()) {
            if (spliceSite.getTimeAccessioned() == null) {
                spliceSite.setTimeAccessioned(new Date());
            }
            spliceSite.setTimeLastModified(new Date());
            spliceSite.setOwner(transcript.getOwner());
        }
        for (NonCanonicalThreePrimeSpliceSite spliceSite : transcript.getNonCanonicalThreePrimeSpliceSites()) {
            if (spliceSite.getTimeAccessioned() == null) {
                spliceSite.setTimeAccessioned(new Date());
            }
            spliceSite.setTimeLastModified(new Date());
            spliceSite.setOwner(transcript.getOwner());
        }


        // event fire
//        fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);

    }
}
