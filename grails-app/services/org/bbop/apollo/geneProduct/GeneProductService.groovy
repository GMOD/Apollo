package org.bbop.apollo.geneProduct

import grails.transaction.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.Gene
import org.bbop.apollo.Pseudogene
import org.bbop.apollo.Transcript
import org.bbop.apollo.geneProduct.GeneProduct
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class GeneProductService {

  def featureRelationshipService


  JSONObject convertAnnotationToJson(GeneProduct geneProduct) {
    JSONObject goObject = new JSONObject()
    if (geneProduct.getId()) {
      goObject.put("id", geneProduct.getId())
    }
    goObject.put("feature", geneProduct.feature.uniqueName)
    goObject.put("productName", geneProduct.productName)
    goObject.put("evidenceCode", geneProduct.evidenceRef)
    goObject.put("evidenceCodeLabel", geneProduct.evidenceRefLabel)
    goObject.put("alternate", geneProduct.alternate)
    goObject.put("withOrFrom", geneProduct.withOrFromArray)
    goObject.put("notes", geneProduct.notesArray)
    goObject.put("reference", geneProduct.reference)
    return goObject
  }

  JSONArray convertAnnotationsToJson(Collection<GeneProduct> geneProducts) {
    JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
    for (GeneProduct geneProduct in geneProducts) {
      annotations.add(convertAnnotationToJson(geneProduct))
    }
    return annotations
  }

  JSONObject getAnnotations(Feature feature) {
    def geneProducts = GeneProduct.findAllByFeature(feature)
    JSONObject returnObject = new JSONObject()
    JSONArray annotations = convertAnnotationsToJson(geneProducts)
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
        List<GeneProduct> annotations = GeneProduct.executeQuery("select ga from GeneProduct ga join ga.feature f where f = :parentFeature ", [parentFeature: parentFeature])
        GeneProduct.deleteAll(annotations)
      }
    }
  }

  def removeGeneProductsFromFeature(Feature feature) {
      def geneProducts = feature.geneProducts
      for(def annotation in geneProducts){
        feature.removeFromGeneProducts(annotation)
      }
  }
}
