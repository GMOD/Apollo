package org.bbop.apollo.go

import grails.transaction.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.Gene
import org.bbop.apollo.Pseudogene
import org.bbop.apollo.Transcript
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class GoAnnotationService {

  def featureRelationshipService


  JSONObject convertAnnotationToJson(GoAnnotation goAnnotation) {
    JSONObject goObject = new JSONObject()
    if (goAnnotation.getId()) {
      goObject.put("id", goAnnotation.getId())
    }
    goObject.put("gene", goAnnotation.feature.uniqueName)
    goObject.put("aspect", goAnnotation.aspect)
    goObject.put("goTerm", goAnnotation.goRef)
    goObject.put("goTermLabel", goAnnotation.goRefLabel)
    goObject.put("geneRelationship", goAnnotation.geneProductRelationshipRef)
    goObject.put("evidenceCode", goAnnotation.evidenceRef)
    goObject.put("evidenceCodeLabel", goAnnotation.evidenceRefLabel)
    goObject.put("negate", goAnnotation.negate)
    goObject.put("withOrFrom", goAnnotation.withOrFromArray)
    goObject.put("notes", goAnnotation.notesArray)
    goObject.put("reference", goAnnotation.reference)
    return goObject
  }

  JSONArray convertAnnotationsToJson(Collection<GoAnnotation> goAnnotations) {
    JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
    for (GoAnnotation goAnnotation in goAnnotations) {
      annotations.add(convertAnnotationToJson(goAnnotation))
    }
    return annotations
  }

  JSONObject getAnnotations(Feature feature) {
    def goAnnotations = GoAnnotation.findAllByFeature(feature)
    JSONObject returnObject = new JSONObject()
    JSONArray annotations = convertAnnotationsToJson(goAnnotations)
    returnObject.put("annotations", annotations)
    return returnObject
  }

  def deleteAnnotations(JSONArray featuresArray) {
    def featureUniqueNames = featuresArray.uniquename as List<String>
    List<Feature> features = Feature.findAllByUniqueNameInList(featureUniqueNames)
    for (Feature thisFeature in features) {
      Feature parentFeature = null
      if (thisFeature instanceof Transcript) {
        parentFeature = featureRelationshipService.getParentForFeature(thisFeature, Gene.ontologyId, Pseudogene.ontologyId)
      } else if (thisFeature instanceof Gene) {
        parentFeature = thisFeature
      }

      if (parentFeature) {
        List<GoAnnotation> annotations = GoAnnotation.executeQuery("select ga from GoAnnotation ga join ga.feature f where f = :parentFeature ", [parentFeature: parentFeature])
        GoAnnotation.deleteAll(annotations)
      }
    }
  }

  def removeGoAnnotationsFromFeature(Feature feature) {
      def goAnnotations = feature.goAnnotations
      for(def annotation in goAnnotations){
        feature.removeFromGoAnnotations(annotation)
      }
  }
}
