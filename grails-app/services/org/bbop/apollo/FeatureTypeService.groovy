package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeatureTypeService {

    def createFeatureTypeForFeature(Class clazz) {
        FeatureType featureType = new FeatureType(
                name: clazz.cvTerm
                , type: "sequence"
                , ontologyId: clazz.ontologyId
        ).save(insert: true, flush: true)
        return featureType
    }

    def stubDefaultFeatureTypes(){
        createFeatureTypeForFeature(Gene.class)
        createFeatureTypeForFeature(Pseudogene.class)
        createFeatureTypeForFeature(Transcript.class)
        createFeatureTypeForFeature(MRNA.class)
        createFeatureTypeForFeature(SnRNA.class)
        createFeatureTypeForFeature(SnoRNA.class)
        createFeatureTypeForFeature(MiRNA.class)
        createFeatureTypeForFeature(TRNA.class)
        createFeatureTypeForFeature(NcRNA.class)
        createFeatureTypeForFeature(RRNA.class)

    }
}
