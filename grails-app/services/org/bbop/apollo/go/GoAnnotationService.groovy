package org.bbop.apollo.go

import grails.transaction.Transactional
import org.bbop.apollo.Feature
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class GoAnnotationService {

    JSONObject getAnnotations(Feature feature) {
        def goAnnotations = GoAnnotation.findAllByFeature(feature)
        JSONObject returnObject = new JSONObject()

        JSONArray annotations = new JSONArray()

//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
        for (GoAnnotation goAnnotation in goAnnotations) {
            JSONObject goObject = new JSONObject()
            if(goAnnotation.getId()){
                goObject.put("id",goAnnotation.getId())
            }
            goObject.put("gene",feature.uniqueName)
            goObject.put("goTerm",goAnnotation.goRef)
            goObject.put("geneRelationship",goAnnotation.geneProductRelationshipRef)
            goObject.put("evidenceCode",goAnnotation.evidenceRef)
            goObject.put("negate",goAnnotation.negate)
            goObject.put("withOrFrom",goAnnotation.withOrFromArray)
            goObject.put("references",goAnnotation.referenceArray)
            annotations.add(goObject)
        }

        returnObject.put("annotations", annotations)


        return returnObject

    }

}
