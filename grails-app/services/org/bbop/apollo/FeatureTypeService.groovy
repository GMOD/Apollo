package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum

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
        createFeatureTypeForFeature(Transcript.class,Transcript.cvTerm)
        createFeatureTypeForFeature(MRNA.class,MRNA.cvTerm)
        createFeatureTypeForFeature(SnRNA.class,SnRNA.cvTerm)
        createFeatureTypeForFeature(SnoRNA.class,SnoRNA.cvTerm)
        createFeatureTypeForFeature(MiRNA.class,MiRNA.cvTerm)
        createFeatureTypeForFeature(TRNA.class,TRNA.cvTerm)
        createFeatureTypeForFeature(NcRNA.class,NcRNA.cvTerm)
        createFeatureTypeForFeature(RRNA.class,RRNA.cvTerm)
        createFeatureTypeForFeature(RepeatRegion.class,RepeatRegion.cvTerm)
        createFeatureTypeForFeature(TransposableElement.class,TransposableElement.cvTerm)
    }
}
