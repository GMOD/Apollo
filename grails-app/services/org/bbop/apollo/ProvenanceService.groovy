package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class ProvenanceService {

  def featureRelationshipService


  JSONObject convertAnnotationToJson(Provenance provenance) {
    JSONObject goObject = new JSONObject()
    if (provenance.getId()) {
      goObject.put("id", provenance.getId())
    }
    goObject.put("feature", provenance.feature.uniqueName)
    goObject.put("field", provenance.field)
    goObject.put("evidenceCode", provenance.evidenceRef)
    goObject.put("evidenceCodeLabel", provenance.evidenceRefLabel)
    goObject.put("withOrFrom", provenance.withOrFromArray)
    goObject.put("notes", provenance.notesArray)
    goObject.put("reference", provenance.reference)
    return goObject
  }

  JSONArray convertAnnotationsToJson(Collection<Provenance> provenances) {
    JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
    for (Provenance provenance in provenances) {
      annotations.add(convertAnnotationToJson(provenance))
    }
    return annotations
  }

  JSONObject getAnnotations(Feature feature) {
    def provenances = Provenance.findAllByFeature(feature)
    JSONObject returnObject = new JSONObject()
    JSONArray annotations = convertAnnotationsToJson(provenances)
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
        List<Provenance> annotations = Provenance.executeQuery("select ga from Provenance ga join ga.feature f where f = :parentFeature ", [parentFeature: parentFeature])
        Provenance.deleteAll(annotations)
      }
    }
  }

  def removeProvenancesFromFeature(Feature feature) {
      def provenances = feature.provenances
      for(def annotation in provenances){
        feature.removeFromProvenances(annotation)
      }
  }
}
