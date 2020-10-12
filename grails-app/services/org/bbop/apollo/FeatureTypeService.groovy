package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeatureTypeService {

    def createFeatureTypeForFeature(Class clazz,String display) {

        FeatureType featureType = new FeatureType(
                name: clazz.cvTerm
                ,display: display
                , type: "sequence"
                , ontologyId: clazz.ontologyId
        ).save(insert: true, flush: true)
        return featureType
    }

    def stubDefaultFeatureTypes(){
        createFeatureTypeForFeature(Gene.class,Gene.cvTerm)
        createFeatureTypeForFeature(Pseudogene.class,Pseudogene.cvTerm)
        createFeatureTypeForFeature(PseudogenicRegion.class,PseudogenicRegion.cvTerm)
        createFeatureTypeForFeature(ProcessedPseudogene.class,ProcessedPseudogene.cvTerm)
        createFeatureTypeForFeature(Transcript.class,Transcript.cvTerm)
        createFeatureTypeForFeature(MRNA.class,MRNA.cvTerm)
        createFeatureTypeForFeature(SnRNA.class,SnRNA.cvTerm)
        createFeatureTypeForFeature(SnoRNA.class,SnoRNA.cvTerm)
        createFeatureTypeForFeature(MiRNA.class,MiRNA.cvTerm)
        createFeatureTypeForFeature(TRNA.class,TRNA.cvTerm)
        createFeatureTypeForFeature(NcRNA.class,NcRNA.cvTerm)

        createFeatureTypeForFeature(GuideRNA.class, GuideRNA.cvTerm)
        createFeatureTypeForFeature(RNaseMRPRNA.class, RNasePRNA.cvTerm)
        createFeatureTypeForFeature(TelomeraseRNA.class, TelomeraseRNA.cvTerm)
        createFeatureTypeForFeature(SrpRNA.class, SrpRNA.cvTerm)
        createFeatureTypeForFeature(LncRNA.class, LncRNA.cvTerm)
        createFeatureTypeForFeature(RNaseMRPRNA.class, RNaseMRPRNA.cvTerm)
        createFeatureTypeForFeature(ScRNA.class, ScRNA.cvTerm)
        createFeatureTypeForFeature(PiRNA.class, PiRNA.cvTerm)
        createFeatureTypeForFeature(TmRNA.class, TmRNA.cvTerm)
        createFeatureTypeForFeature(EnzymaticRNA.class, EnzymaticRNA.cvTerm)

        createFeatureTypeForFeature(RRNA.class,RRNA.cvTerm)
        createFeatureTypeForFeature(RepeatRegion.class,RepeatRegion.cvTerm)
        createFeatureTypeForFeature(Terminator.class,Terminator.alternateCvTerm)
        createFeatureTypeForFeature(ShineDalgarnoSequence.class,ShineDalgarnoSequence.cvTerm)
        createFeatureTypeForFeature(TransposableElement.class,TransposableElement.cvTerm)
        return true
    }
}
