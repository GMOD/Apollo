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
        createFeatureTypeForFeature(MRNA.class,MRNA.alternateCvTerm)
        createFeatureTypeForFeature(SnRNA.class,SnRNA.alternateCvTerm)
        createFeatureTypeForFeature(SnoRNA.class,SnoRNA.alternateCvTerm)
        createFeatureTypeForFeature(MiRNA.class,MiRNA.alternateCvTerm)
        createFeatureTypeForFeature(TRNA.class,TRNA.alternateCvTerm)
        createFeatureTypeForFeature(NcRNA.class,NcRNA.alternateCvTerm)
        createFeatureTypeForFeature(RRNA.class,RRNA.alternateCvTerm)
        createFeatureTypeForFeature(RepeatRegion.class,RepeatRegion.alternateCvTerm)
        createFeatureTypeForFeature(TransposableElement.class,TransposableElement.alternateCvTerm)
    }
}
